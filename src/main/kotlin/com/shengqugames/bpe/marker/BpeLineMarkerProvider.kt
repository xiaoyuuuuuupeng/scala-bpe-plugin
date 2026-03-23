package com.shengqugames.bpe.marker

import com.intellij.codeInsight.daemon.LineMarkerInfo
import com.intellij.codeInsight.daemon.LineMarkerProvider
import com.intellij.openapi.editor.markup.GutterIconRenderer
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.util.IconLoader
import com.intellij.psi.PsiElement
import com.intellij.psi.xml.XmlTag
import com.intellij.psi.xml.XmlToken
import com.intellij.psi.xml.XmlTokenType
import com.shengqugames.bpe.util.BpeFlowFinder
import com.shengqugames.bpe.util.BpeImplementationNavigateOffset

/**
 * Gutter icon on <message> tags in XML → navigates to implementation file only.
 * (Invoke call sites are handled by Ctrl+Click via BpeMessageReference.)
 */
class BpeLineMarkerProvider : LineMarkerProvider {

    companion object {
        val ICON = IconLoader.getIcon("/icons/bpe_impl.svg", BpeLineMarkerProvider::class.java)
    }

    override fun getLineMarkerInfo(element: PsiElement): LineMarkerInfo<*>? {
        if (element !is XmlToken) return null
        if (element.tokenType != XmlTokenType.XML_NAME) return null

        // Only match the opening <message>, skip </message>
        val prev = element.prevSibling
        if (prev is XmlToken && prev.tokenType == XmlTokenType.XML_END_TAG_START) return null

        val tag = element.parent as? XmlTag ?: return null
        if (tag.name != "message") return null

        val serviceTag = tag.parentTag
        if (serviceTag?.name != "service") return null

        val messageName = tag.getAttributeValue("name")?.takeIf { it.isNotEmpty() } ?: return null
        val messageId = tag.getAttributeValue("id")?.takeIf { it.isNotEmpty() } ?: return null
        val serviceName = serviceTag.getAttributeValue("name")?.takeIf { it.isNotEmpty() } ?: return null

        val project = element.project
        val params = BpeFlowFinder.SearchParams(serviceName, messageName, messageId)

        if (!BpeFlowFinder.hasFile(project, params)) return null

        return LineMarkerInfo(
            element,
            element.textRange,
            ICON,
            { "跳转到 $serviceName.$messageName 的实现" },
            { _, _ ->
                BpeFlowFinder.findFiles(project, params).firstOrNull()?.let { vf ->
                    val offset = BpeImplementationNavigateOffset.getOffset(vf, serviceName, messageName)
                    OpenFileDescriptor(project, vf, offset).navigate(true)
                }
            },
            GutterIconRenderer.Alignment.RIGHT,
            { "Go to implementation: $serviceName.$messageName" }
        )
    }
}
