package com.shengqugames.bpe.marker

import com.intellij.codeInsight.daemon.LineMarkerInfo
import com.intellij.codeInsight.daemon.LineMarkerProvider
import com.intellij.openapi.editor.markup.GutterIconRenderer
import com.intellij.openapi.util.IconLoader
import com.intellij.openapi.util.TextRange
import com.intellij.pom.Navigatable
import com.intellij.psi.PsiElement
import com.shengqugames.bpe.util.BpeXmlFinder

/**
 * Shows a gutter icon (← arrow) on class declarations / flow comments that link back to XML.
 *
 * Handles two PSI structures:
 *   - Structured PSI (Scala plugin): element is a single-token identifier like "Flow_xxx_yyy"
 *   - Plain text PSI (no Scala plugin / .flow files): element contains the full file text
 */
class BpeReverseLineMarkerProvider : LineMarkerProvider {

    companion object {
        val ICON = IconLoader.getIcon("/icons/bpe_xml.svg", BpeReverseLineMarkerProvider::class.java)
        val CLASS_NAME_PATTERN = Regex("""Flow_([a-z0-9]+)_([a-z0-9]+)""")
        val FULL_CLASS_PATTERN = Regex("""class\s+Flow_([a-z0-9]+)_([a-z0-9]+)\s+extends""")
        val FLOW_COMMENT_PATTERN = Regex("""//\${'$'}(\w+)\.(\w+)""")
    }

    override fun getLineMarkerInfo(element: PsiElement): LineMarkerInfo<*>? {
        val file = element.containingFile?.virtualFile ?: return null
        val ext = file.extension?.lowercase()
        if (ext != "scala" && ext != "flow") return null

        val text = element.text

        // Structured PSI case: element is a single identifier token like "Flow_xxx_yyy"
        if (!text.contains('\n') && !text.contains(' ')) {
            val match = CLASS_NAME_PATTERN.matchEntire(text) ?: return null
            val svcLower = match.groupValues[1]
            val msgLower = match.groupValues[2]
            if (!BpeXmlFinder.hasMessageElement(element.project, svcLower, msgLower)) return null
            return createMarker(element, element.textRange, svcLower, msgLower)
        }

        return null
    }

    override fun collectSlowLineMarkers(
        elements: List<PsiElement>,
        result: MutableCollection<in LineMarkerInfo<*>>
    ) {
        for (element in elements) {
            val file = element.containingFile?.virtualFile ?: continue
            val ext = file.extension?.lowercase()
            if (ext != "scala" && ext != "flow") continue

            val text = element.text
            if (!text.contains('\n')) continue

            // Plain text case: element spans the whole file, scan first ~2000 chars
            val scanText = text.take(2000)
            val baseOffset = element.textRange.startOffset

            val classMatch = FULL_CLASS_PATTERN.find(scanText)
            if (classMatch != null) {
                val svcLower = classMatch.groupValues[1]
                val msgLower = classMatch.groupValues[2]
                if (BpeXmlFinder.hasMessageElement(element.project, svcLower, msgLower)) {
                    val range = TextRange(
                        baseOffset + classMatch.range.first,
                        baseOffset + classMatch.range.last + 1
                    )
                    result.add(createMarker(element, range, svcLower, msgLower))
                    continue
                }
            }

            val flowMatch = FLOW_COMMENT_PATTERN.find(scanText)
            if (flowMatch != null) {
                val svcLower = flowMatch.groupValues[1].lowercase()
                val msgLower = flowMatch.groupValues[2].lowercase()
                if (BpeXmlFinder.hasMessageElement(element.project, svcLower, msgLower)) {
                    val range = TextRange(
                        baseOffset + flowMatch.range.first,
                        baseOffset + flowMatch.range.last + 1
                    )
                    result.add(createMarker(element, range, svcLower, msgLower))
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
            { "跳转到 XML 中的 message 定义" },
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
