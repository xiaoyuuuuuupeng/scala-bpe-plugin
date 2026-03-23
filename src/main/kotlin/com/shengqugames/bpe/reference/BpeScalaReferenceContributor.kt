package com.shengqugames.bpe.reference

import com.intellij.openapi.util.TextRange
import com.intellij.patterns.PlatformPatterns
import com.intellij.psi.*
import com.intellij.util.ProcessingContext

/**
 * Provides precise-range PsiReferences in .scala / .flow files for:
 *   1. Class names:      Flow_xxx_yyy           → XML <message>
 *   2. Flow comments:    //$svc.msg             → XML <message>
 *   3. Invoke strings:   "svcName.msgName"      → XML <message>
 */
class BpeScalaReferenceContributor : PsiReferenceContributor() {

    override fun registerReferenceProviders(registrar: PsiReferenceRegistrar) {
        // 较高优先级，使 BPE 引用尽量与 Scala 插件的引用一并参与解析（具体合并行为由平台决定）
        registrar.registerReferenceProvider(
            PlatformPatterns.psiElement(),
            BpeScalaReferenceProvider(),
            2000.0
        )
    }
}

private class BpeScalaReferenceProvider : PsiReferenceProvider() {

    companion object {
        /** 类名各段允许大小写（与 XML 比较时统一转小写） */
        val CLASS_NAME_REGEX = Regex("""Flow_([a-zA-Z0-9]+)_([a-zA-Z0-9]+)""")
        val FLOW_COMMENT_REGEX = Regex("""//\${'$'}(\w+)\.(\w+)""")
        // "serviceName.messageName" — exactly one dot, word chars only
        val INVOKE_STRING_REGEX = Regex(""""(\w+)\.(\w+)"""")
    }

    override fun getReferencesByElement(
        element: PsiElement,
        context: ProcessingContext
    ): Array<PsiReference> {
        val file = element.containingFile?.virtualFile ?: return PsiReference.EMPTY_ARRAY
        val ext = file.extension?.lowercase()
        if (ext != "scala" && ext != "flow") return PsiReference.EMPTY_ARRAY

        val text = element.text

        // Structured PSI (Scala plugin): element is a small single token
        if (!text.contains('\n') && text.length < 200) {
            return getRefsForSmallElement(element, text)
        }

        // Plain text PSI: element spans the entire file
        return getRefsForLargeElement(element, text)
    }

    private fun getRefsForSmallElement(element: PsiElement, text: String): Array<PsiReference> {
        // Case 1: identifier token like "Flow_xxx_yyy"
        CLASS_NAME_REGEX.matchEntire(text)?.let { match ->
            val range = TextRange(0, text.length)
            return arrayOf(
                BpeFlowToXmlReference(
                    element, range,
                    match.groupValues[1].lowercase(), match.groupValues[2].lowercase()
                )
            )
        }

        // Case 2: string literal token like "dbPayRouter.isExistOrderUserRecord" (with quotes)
        INVOKE_STRING_REGEX.matchEntire(text)?.let { match ->
            val range = TextRange(1, text.length - 1) // content inside quotes
            return arrayOf(BpeFlowToXmlReference(element, range,
                match.groupValues[1].lowercase(), match.groupValues[2].lowercase()))
        }

        // Case 3: 单行 //$svc.msg（独立 Psi 元素）。此前仅在「整文件大元素」分支匹配，.flow 被当作 Scala 后注释成小元素，导致无法 Ctrl+Click
        val trimmedForComment = text.trimStart()
        val leadingWs = text.length - trimmedForComment.length
        FLOW_COMMENT_REGEX.matchEntire(trimmedForComment)?.let { match ->
            val range = TextRange(
                leadingWs + match.range.first,
                leadingWs + match.range.last + 1
            )
            return arrayOf(BpeFlowToXmlReference(element, range,
                match.groupValues[1].lowercase(), match.groupValues[2].lowercase()))
        }

        return PsiReference.EMPTY_ARRAY
    }

    private fun getRefsForLargeElement(element: PsiElement, text: String): Array<PsiReference> {
        val refs = mutableListOf<PsiReference>()

        for (match in CLASS_NAME_REGEX.findAll(text)) {
            val range = TextRange(match.range.first, match.range.last + 1)
            refs.add(BpeFlowToXmlReference(element, range,
                match.groupValues[1].lowercase(), match.groupValues[2].lowercase()))
        }

        for (match in FLOW_COMMENT_REGEX.findAll(text)) {
            val range = TextRange(match.range.first, match.range.last + 1)
            refs.add(BpeFlowToXmlReference(element, range,
                match.groupValues[1].lowercase(), match.groupValues[2].lowercase()))
        }

        // invoke("svcName.msgName", ...) — underline only the content inside quotes
        for (match in INVOKE_STRING_REGEX.findAll(text)) {
            val range = TextRange(match.range.first + 1, match.range.last) // skip quotes
            refs.add(BpeFlowToXmlReference(element, range,
                match.groupValues[1].lowercase(), match.groupValues[2].lowercase()))
        }

        return refs.toTypedArray()
    }
}

/**
 * 供右键菜单等：在偏移处找出覆盖该位置的 BPE → XML 引用（与 Ctrl+Click 相同规则）。
 */
fun findBpeFlowToXmlReferenceAt(psiFile: PsiFile, offset: Int): BpeFlowToXmlReference? {
    val provider = BpeScalaReferenceProvider()
    val ctx = ProcessingContext()
    val el = psiFile.findElementAt(offset)
        ?: psiFile.findElementAt((offset - 1).coerceAtLeast(0))
        ?: return null
    var current: PsiElement? = el
    while (current != null) {
        val refs = provider.getReferencesByElement(current, ctx)
        for (r in refs) {
            if (r !is BpeFlowToXmlReference) continue
            val host = r.element
            val base = host.textRange.startOffset
            val a = base + r.rangeInElement.startOffset
            val b = base + r.rangeInElement.endOffset
            if (offset in a until b) return r
        }
        current = current.parent
    }
    return null
}
