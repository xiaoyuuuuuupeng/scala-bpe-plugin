package com.shengqugames.bpe.marker

import com.intellij.codeInsight.daemon.LineMarkerInfo
import com.intellij.codeInsight.daemon.LineMarkerProvider
import com.intellij.openapi.editor.markup.GutterIconRenderer
import com.intellij.openapi.util.IconLoader
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.pom.Navigatable
import com.intellij.psi.PsiElement
import com.shengqugames.bpe.util.BpeXmlFinder

/**
 * Shows a gutter icon (← arrow) on class declarations / flow comments that link back to XML.
 *
 * 每个 .scala / .flow 文件只生成一个 gutter：在 collectSlowLineMarkers 里按 VirtualFile 去重并扫描全文前段，
 * 避免「类名标识符」与「整段扫描」两条路径重复导致同一行两个图标。
 */
class BpeReverseLineMarkerProvider : LineMarkerProvider {

    companion object {
        val ICON = IconLoader.getIcon("/icons/bpe_xml.svg", BpeReverseLineMarkerProvider::class.java)
        val FULL_CLASS_PATTERN = Regex("""class\s+Flow_([a-zA-Z0-9]+)_([a-zA-Z0-9]+)\s+extends""")
        val FLOW_COMMENT_PATTERN = Regex("""//\${'$'}(\w+)\.(\w+)""")
    }

    override fun getLineMarkerInfo(element: PsiElement): LineMarkerInfo<*>? = null

    override fun collectSlowLineMarkers(
        elements: List<PsiElement>,
        result: MutableCollection<in LineMarkerInfo<*>>
    ) {
        val processedFiles = mutableSetOf<VirtualFile>()
        for (element in elements) {
            val vf = element.containingFile?.virtualFile ?: continue
            val ext = vf.extension?.lowercase()
            if (ext != "scala" && ext != "flow") continue
            if (!processedFiles.add(vf)) continue

            val psiFile = element.containingFile ?: continue
            val text = psiFile.text
            if (text.isEmpty()) continue

            val scanText = text.take(2000)

            val classMatch = FULL_CLASS_PATTERN.find(scanText)
            if (classMatch != null) {
                val svcLower = classMatch.groupValues[1].lowercase()
                val msgLower = classMatch.groupValues[2].lowercase()
                if (BpeXmlFinder.hasMessageElement(element.project, svcLower, msgLower)) {
                    val range = TextRange(
                        classMatch.range.first,
                        classMatch.range.last + 1
                    )
                    result.add(createMarker(psiFile, range, svcLower, msgLower))
                    continue
                }
            }

            val flowMatch = FLOW_COMMENT_PATTERN.find(scanText)
            if (flowMatch != null) {
                val svcLower = flowMatch.groupValues[1].lowercase()
                val msgLower = flowMatch.groupValues[2].lowercase()
                if (BpeXmlFinder.hasMessageElement(element.project, svcLower, msgLower)) {
                    val range = TextRange(
                        flowMatch.range.first,
                        flowMatch.range.last + 1
                    )
                    result.add(createMarker(psiFile, range, svcLower, msgLower))
                }
            }
        }
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
