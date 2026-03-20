package com.shengqugames.bpe.doc

import com.intellij.lang.documentation.AbstractDocumentationProvider
import com.intellij.psi.PsiElement
import com.intellij.psi.xml.XmlTag

/**
 * Shows service/message name and request/response parameter info
 * both on Ctrl+hover (quick info) and Ctrl+Q (full documentation).
 *
 * Works for two scenarios:
 *   1. Hover on "svcName.msgName" in .scala/.flow → element is the resolved <message> XmlTag
 *   2. Hover on <message name="..."> in XML → element might not be an XmlTag,
 *      so we also check originalElement's parent hierarchy
 */
class BpeDocumentationProvider : AbstractDocumentationProvider() {

    override fun getQuickNavigateInfo(element: PsiElement?, originalElement: PsiElement?): String? {
        val messageTag = findMessageTag(element, originalElement) ?: return null
        val serviceTag = messageTag.parentTag ?: return null
        if (serviceTag.name != "service") return null

        val serviceName = serviceTag.getAttributeValue("name") ?: return null
        val messageName = messageTag.getAttributeValue("name") ?: return null
        val messageId = messageTag.getAttributeValue("id") ?: ""
        val typeMap = buildTypeMap(serviceTag)

        val sb = StringBuilder()
        sb.append("<b>$serviceName.$messageName</b>")
        if (messageId.isNotEmpty()) sb.append("  <code>id=$messageId</code>")

        val reqFields = getFieldList(messageTag, "requestParameter", typeMap)
        if (reqFields.isNotEmpty()) {
            sb.append("<br/><br/><b>Request:</b> ")
            sb.append(reqFields.joinToString(", ") { "<code>${it.first}</code>: ${it.second}" })
        }

        val respFields = getFieldList(messageTag, "responseParameter", typeMap)
        if (respFields.isNotEmpty()) {
            sb.append("<br/><b>Response:</b> ")
            sb.append(respFields.joinToString(", ") { "<code>${it.first}</code>: ${it.second}" })
        }

        return sb.toString()
    }

    override fun generateDoc(element: PsiElement?, originalElement: PsiElement?): String? {
        val messageTag = findMessageTag(element, originalElement) ?: return null
        val serviceTag = messageTag.parentTag ?: return null
        if (serviceTag.name != "service") return null

        val serviceName = serviceTag.getAttributeValue("name") ?: return null
        val messageName = messageTag.getAttributeValue("name") ?: return null
        val messageId = messageTag.getAttributeValue("id") ?: ""
        val typeMap = buildTypeMap(serviceTag)

        val sb = StringBuilder()
        sb.append("<div>")
        sb.append("<b>$serviceName.$messageName</b>")
        if (messageId.isNotEmpty()) sb.append(" &nbsp; <code>id=$messageId</code>")
        sb.append("</div>")
        sb.append("<hr/>")

        val reqTag = messageTag.findFirstSubTag("requestParameter")
        if (reqTag != null) {
            sb.append("<p><b>Request Parameters:</b></p>")
            appendFieldTable(sb, reqTag, typeMap)
        }

        val respTag = messageTag.findFirstSubTag("responseParameter")
        if (respTag != null) {
            sb.append("<p><b>Response Parameters:</b></p>")
            appendFieldTable(sb, respTag, typeMap)
        }

        return sb.toString()
    }

    private fun findMessageTag(element: PsiElement?, originalElement: PsiElement?): XmlTag? {
        // Try resolved element first (works for .scala/.flow → XML hover)
        resolveMessageTag(element)?.let { return it }
        // Fallback: walk up from originalElement (works for XML hover)
        resolveMessageTag(originalElement)?.let { return it }
        walkUpToMessageTag(originalElement)?.let { return it }
        return null
    }

    private fun resolveMessageTag(element: PsiElement?): XmlTag? {
        if (element == null) return null
        if (element is XmlTag && element.name == "message") return element
        val parent = element.parent
        if (parent is XmlTag && parent.name == "message") return parent
        return null
    }

    private fun walkUpToMessageTag(element: PsiElement?): XmlTag? {
        var current: PsiElement? = element
        while (current != null) {
            if (current is XmlTag && current.name == "message") return current
            current = current.parent
        }
        return null
    }

    private fun buildTypeMap(serviceTag: XmlTag): Map<String, String> {
        val map = mutableMapOf<String, String>()
        for (typeTag in serviceTag.findSubTags("type")) {
            val name = typeTag.getAttributeValue("name") ?: continue
            val clazz = typeTag.getAttributeValue("class") ?: continue
            map[name] = clazz
        }
        return map
    }

    private fun getFieldList(messageTag: XmlTag, tagName: String, typeMap: Map<String, String>): List<Pair<String, String>> {
        val paramTag = messageTag.findFirstSubTag(tagName) ?: return emptyList()
        return paramTag.findSubTags("field").mapNotNull { field ->
            val name = field.getAttributeValue("name") ?: return@mapNotNull null
            val typeName = field.getAttributeValue("type") ?: ""
            val typeClass = typeMap[typeName] ?: typeName.removeSuffix("_type")
            Pair(name, typeClass)
        }
    }

    private fun appendFieldTable(sb: StringBuilder, paramTag: XmlTag, typeMap: Map<String, String>) {
        val fields = paramTag.findSubTags("field")
        if (fields.isEmpty()) {
            sb.append("<p><i>(none)</i></p>")
            return
        }
        sb.append("<table style='margin-left:8px'>")
        for (field in fields) {
            val name = field.getAttributeValue("name") ?: continue
            val typeName = field.getAttributeValue("type") ?: ""
            val typeClass = typeMap[typeName] ?: typeName.removeSuffix("_type")
            sb.append("<tr>")
            sb.append("<td><code>$name</code></td>")
            sb.append("<td>&nbsp; : &nbsp;</td>")
            sb.append("<td><code>$typeClass</code></td>")
            sb.append("</tr>")
        }
        sb.append("</table>")
    }
}
