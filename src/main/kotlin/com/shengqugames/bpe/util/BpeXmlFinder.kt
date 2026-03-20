package com.shengqugames.bpe.util

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiManager
import com.intellij.psi.xml.XmlFile
import com.intellij.psi.xml.XmlTag

/**
 * Finds XML <message> elements in avenue_conf/ that match a given service + message name.
 *
 * Mapping (all comparisons are case-insensitive):
 *   class Flow_{svcLower}_{msgLower} → <service name="..."> / <message name="...">
 *   //$serviceName.messageName      → <service name="serviceName"> / <message name="messageName">
 */
object BpeXmlFinder {

    fun findMessageElements(
        project: Project,
        serviceNameLower: String,
        msgNameLower: String
    ): List<PsiElement> {
        val results = mutableListOf<PsiElement>()
        val psiManager = PsiManager.getInstance(project)

        forEachXmlFile(project) { xmlFile ->
            val psiFile = psiManager.findFile(xmlFile) as? XmlFile ?: return@forEachXmlFile
            val rootTag = psiFile.rootTag ?: return@forEachXmlFile
            if (rootTag.name != "service") return@forEachXmlFile

            val svcName = rootTag.getAttributeValue("name") ?: return@forEachXmlFile
            if (svcName.lowercase() != serviceNameLower) return@forEachXmlFile

            for (messageTag in rootTag.findSubTags("message")) {
                val msgName = messageTag.getAttributeValue("name") ?: continue
                if (msgName.lowercase() == msgNameLower) {
                    results.add(messageTag)
                }
            }
        }

        return results
    }

    fun hasMessageElement(
        project: Project,
        serviceNameLower: String,
        msgNameLower: String
    ): Boolean {
        var found = false
        val psiManager = PsiManager.getInstance(project)

        forEachXmlFile(project) { xmlFile ->
            if (found) return@forEachXmlFile
            val psiFile = psiManager.findFile(xmlFile) as? XmlFile ?: return@forEachXmlFile
            val rootTag = psiFile.rootTag ?: return@forEachXmlFile
            if (rootTag.name != "service") return@forEachXmlFile

            val svcName = rootTag.getAttributeValue("name") ?: return@forEachXmlFile
            if (svcName.lowercase() != serviceNameLower) return@forEachXmlFile

            for (messageTag in rootTag.findSubTags("message")) {
                val msgName = messageTag.getAttributeValue("name") ?: continue
                if (msgName.lowercase() == msgNameLower) {
                    found = true
                    return@forEachXmlFile
                }
            }
        }

        return found
    }

    /**
     * **仅当前 XML 文件内**查找 `<type name="..."/>`，用于 field 等处的 `type="类型名"` 跳转（不跨文件）。
     */
    fun findTypeDefinitionElementsInFile(xmlFile: XmlFile, typeName: String): List<PsiElement> {
        if (typeName.isBlank()) return emptyList()
        val results = mutableListOf<PsiElement>()
        val rootTag = xmlFile.rootTag ?: return emptyList()
        collectTypeTagsByName(rootTag, typeName, results)
        return results
    }

    private fun collectTypeTagsByName(tag: XmlTag, typeName: String, out: MutableList<PsiElement>) {
        if (tag.name == "type") {
            val n = tag.getAttributeValue("name")
            if (n != null && n.equals(typeName, ignoreCase = true)) {
                out.add(tag)
            }
        }
        for (sub in tag.subTags) {
            collectTypeTagsByName(sub, typeName, out)
        }
    }

    /** 契约 XML 是否位于 `avenue_conf` 下（避免误处理其它 XML 的 `type` 属性） */
    fun isAvenueConfXml(project: Project, file: VirtualFile): Boolean {
        if (file.extension?.lowercase() != "xml") return false
        val base = project.basePath ?: return false
        val normBase = base.replace('\\', '/').trimEnd('/')
        val normPath = file.path.replace('\\', '/')
        return normPath.startsWith("$normBase/avenue_conf/")
    }

    private fun forEachXmlFile(project: Project, visitor: (VirtualFile) -> Unit) {
        val basePath = project.basePath ?: return
        val avenueDir = LocalFileSystem.getInstance().findFileByPath("$basePath/avenue_conf")
            ?: return
        scanDir(avenueDir, visitor)
    }

    private fun scanDir(dir: VirtualFile, visitor: (VirtualFile) -> Unit) {
        for (child in dir.children) {
            if (child.isDirectory) {
                scanDir(child, visitor)
            } else if (child.extension == "xml") {
                visitor(child)
            }
        }
    }
}
