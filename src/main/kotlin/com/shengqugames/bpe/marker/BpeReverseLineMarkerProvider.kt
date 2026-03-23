package com.shengqugames.bpe.marker

import com.intellij.codeInsight.daemon.LineMarkerInfo
import com.intellij.codeInsight.daemon.LineMarkerProvider
import com.intellij.openapi.editor.markup.GutterIconRenderer
import com.intellij.openapi.util.IconLoader
import com.intellij.openapi.util.TextRange
import com.intellij.pom.Navigatable
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.shengqugames.bpe.util.BpeXmlFinder

/**
 * .scala / .flow → XML 的 gutter（←）。
 *
 * **不要依赖 [collectSlowLineMarkers] 作为唯一来源**：`LineMarkersPass` 在 `Mode.FAST` 下会在调用
 * `collectSlowLineMarkers` 之前直接返回，导致慢速收集从不执行，gutter 会全部消失。
 *
 * 在 [getLineMarkerInfo] 中处理：
 * - 可见区域内的 `Flow_` 类名 token、`//$` 注释（与 Ctrl+Click 一致）
 * - 无 Scala PSI 的纯文本 `.flow`：单元素覆盖整文件时再扫 class / `//$`
 */
class BpeReverseLineMarkerProvider : LineMarkerProvider {

    companion object {
        val ICON = IconLoader.getIcon("/icons/bpe_xml.svg", BpeReverseLineMarkerProvider::class.java)
        val FULL_CLASS_PATTERN = Regex("""class\s+Flow_([a-zA-Z0-9]+)_([a-zA-Z0-9]+)\s+extends""")
        val FLOW_COMMENT_PATTERN = Regex("""//\s*\$\s*(\w+)\.(\w+)""")
        private val CLASS_NAME_TOKEN = Regex("""Flow_([a-zA-Z0-9]+)_([a-zA-Z0-9]+)""")
        private const val SCAN_PREFIX_CHARS = 512 * 1024
    }

    override fun getLineMarkerInfo(element: PsiElement): LineMarkerInfo<*>? {
        val psiFile = element.containingFile ?: return null
        val vf = psiFile.virtualFile ?: return null
        val ext = vf.extension?.lowercase() ?: return null
        if (ext != "scala" && ext != "flow") return null

        val project = element.project
        val text = element.text

        // 1) 类名标识符 Flow_xxx_yyy（Scala / 结构化 .flow）
        if (!text.contains('\n') && !text.contains(' ') && text.length < 200) {
            CLASS_NAME_TOKEN.matchEntire(text)?.let { m ->
                val svc = m.groupValues[1].lowercase()
                val msg = m.groupValues[2].lowercase()
                if (BpeXmlFinder.hasMessageElement(project, svc, msg)) {
                    return createMarker(element, element.textRange, svc, msg)
                }
            }
        }

        // 2) 单行 //$service.message（注释被拆成独立 Psi 时）
        if (!text.contains('\n') && text.length < 500) {
            val trimmed = text.trimStart()
            val leadingWs = text.length - trimmed.length
            FLOW_COMMENT_PATTERN.matchEntire(trimmed)?.let { m ->
                val svc = m.groupValues[1].lowercase()
                val msg = m.groupValues[2].lowercase()
                if (BpeXmlFinder.hasMessageElement(project, svc, msg)) {
                    val start = element.textRange.startOffset + leadingWs + m.range.first
                    val end = element.textRange.startOffset + leadingWs + m.range.last + 1
                    return createMarker(element, TextRange(start, end), svc, msg)
                }
            }
        }

        // 3) 纯文本 .flow：整文件一个元素时扫 class / //$（无 Scala 插件时常见）
        if (ext == "flow" && isPlainTextFlow(psiFile) && spansWholeFile(element, psiFile) && text.contains('\n')) {
            return markerFromScannedContent(psiFile, element, text.take(SCAN_PREFIX_CHARS))
        }

        return null
    }

    /** FAST 模式下不会执行；留空避免与 [getLineMarkerInfo] 在 ALL 模式下重复注册 */
    override fun collectSlowLineMarkers(
        elements: List<PsiElement>,
        result: MutableCollection<in LineMarkerInfo<*>>
    ) {
    }

    private fun isPlainTextFlow(psiFile: PsiFile): Boolean {
        val id = psiFile.language.id
        return id.equals("TEXT", ignoreCase = true) ||
            id.equals("Plain text", ignoreCase = true) ||
            id.equals("text", ignoreCase = true)
    }

    private fun spansWholeFile(element: PsiElement, psiFile: PsiFile): Boolean {
        val fr = psiFile.textRange
        val er = element.textRange
        return er.startOffset == fr.startOffset && er.endOffset == fr.endOffset
    }

    private fun markerFromScannedContent(
        psiFile: PsiFile,
        anchor: PsiElement,
        scanText: String
    ): LineMarkerInfo<PsiElement>? {
        val base = psiFile.textRange.startOffset

        FULL_CLASS_PATTERN.find(scanText)?.let { classMatch ->
            val svcLower = classMatch.groupValues[1].lowercase()
            val msgLower = classMatch.groupValues[2].lowercase()
            if (BpeXmlFinder.hasMessageElement(psiFile.project, svcLower, msgLower)) {
                val range = TextRange(
                    base + classMatch.range.first,
                    base + classMatch.range.last + 1
                )
                return createMarker(anchor, range, svcLower, msgLower)
            }
        }

        FLOW_COMMENT_PATTERN.find(scanText)?.let { flowMatch ->
            val svcLower = flowMatch.groupValues[1].lowercase()
            val msgLower = flowMatch.groupValues[2].lowercase()
            if (BpeXmlFinder.hasMessageElement(psiFile.project, svcLower, msgLower)) {
                val range = TextRange(
                    base + flowMatch.range.first,
                    base + flowMatch.range.last + 1
                )
                return createMarker(anchor, range, svcLower, msgLower)
            }
        }

        return null
    }

    private fun createMarker(
        element: PsiElement,
        range: TextRange,
        svcLower: String,
        msgLower: String
    ): LineMarkerInfo<PsiElement> {
        return LineMarkerInfo(
            element,
            range,
            ICON,
            { "跳转到声明" },
            { _, _ ->
                val targets = BpeXmlFinder.findMessageElements(element.project, svcLower, msgLower)
                val target = targets.firstOrNull()
                if (target is Navigatable && target.canNavigate()) {
                    target.navigate(true)
                }
            },
            GutterIconRenderer.Alignment.LEFT,
            { "Navigate to XML message definition" }
        )
    }
}
