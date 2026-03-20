package com.shengqugames.bpe.util

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiManager
import com.intellij.psi.xml.XmlFile

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
