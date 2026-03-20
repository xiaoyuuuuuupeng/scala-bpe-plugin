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
        registrar.registerReferenceProvider(
            PlatformPatterns.psiElement(),
            BpeScalaReferenceProvider()
        )
    }
}

private class BpeScalaReferenceProvider : PsiReferenceProvider() {

    companion object {
        val CLASS_NAME_REGEX = Regex("""Flow_([a-z0-9]+)_([a-z0-9]+)""")
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
            return arrayOf(BpeFlowToXmlReference(element, range, match.groupValues[1], match.groupValues[2]))
        }

        // Case 2: string literal token like "dbPayRouter.isExistOrderUserRecord" (with quotes)
        INVOKE_STRING_REGEX.matchEntire(text)?.let { match ->
            val range = TextRange(1, text.length - 1) // content inside quotes
            return arrayOf(BpeFlowToXmlReference(element, range,
                match.groupValues[1].lowercase(), match.groupValues[2].lowercase()))
        }

        return PsiReference.EMPTY_ARRAY
    }

    private fun getRefsForLargeElement(element: PsiElement, text: String): Array<PsiReference> {
        val refs = mutableListOf<PsiReference>()

        for (match in CLASS_NAME_REGEX.findAll(text)) {
            val range = TextRange(match.range.first, match.range.last + 1)
            refs.add(BpeFlowToXmlReference(element, range, match.groupValues[1], match.groupValues[2]))
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
