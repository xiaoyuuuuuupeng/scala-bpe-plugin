package com.shengqugames.bpe.util

import com.intellij.navigation.ItemPresentation
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.psi.impl.FakePsiElement
import javax.swing.Icon

/**
 * Scans compose_conf/ for invoke call sites that reference a given service.message.
 *
 * Returns navigatable elements that jump to the exact offset of
 * "serviceName.messageName" in .scala / .flow files.
 */
object BpeInvokeFinder {

    fun findCallSites(
        project: Project,
        serviceName: String,
        messageName: String
    ): List<PsiElement> {
        val composeDir = findComposeDir(project) ?: return emptyList()
        val target = "\"$serviceName.$messageName\""
        val targetLower = target.lowercase()
        val results = mutableListOf<PsiElement>()
        val psiManager = PsiManager.getInstance(project)

        scanDir(composeDir) { file ->
            try {
                val content = String(file.contentsToByteArray(), Charsets.UTF_8)
                val idx = content.lowercase().indexOf(targetLower)
                if (idx >= 0) {
                    val psiFile = psiManager.findFile(file) ?: return@scanDir
                    val label = "${file.nameWithoutExtension} — invoke(\"$serviceName.$messageName\")"
                    results.add(InvokeCallSiteElement(psiFile, idx + 1, label))
                }
            } catch (_: Exception) { }
        }

        return results
    }

    fun hasCallSite(
        project: Project,
        serviceName: String,
        messageName: String
    ): Boolean {
        val composeDir = findComposeDir(project) ?: return false
        val target = "\"$serviceName.$messageName\"".lowercase()
        var found = false

        scanDir(composeDir) { file ->
            if (found) return@scanDir
            try {
                val content = String(file.contentsToByteArray(), Charsets.UTF_8).lowercase()
                if (content.contains(target)) found = true
            } catch (_: Exception) { }
        }

        return found
    }

    private fun findComposeDir(project: Project): VirtualFile? {
        val basePath = project.basePath ?: return null
        return LocalFileSystem.getInstance().findFileByPath("$basePath/compose_conf")
    }

    private fun scanDir(dir: VirtualFile, visitor: (VirtualFile) -> Unit) {
        for (child in dir.children) {
            if (child.isDirectory) {
                scanDir(child, visitor)
            } else {
                val ext = child.extension?.lowercase()
                if (ext == "scala" || ext == "flow") {
                    visitor(child)
                }
            }
        }
    }
}

/**
 * A lightweight navigatable PsiElement that opens a file at a specific offset.
 * Used to represent invoke call sites in navigation popups.
 */
class InvokeCallSiteElement(
    private val containingPsiFile: PsiFile,
    private val offset: Int,
    private val label: String
) : FakePsiElement() {

    override fun getParent(): PsiElement = containingPsiFile

    override fun getContainingFile(): PsiFile = containingPsiFile

    override fun navigate(requestFocus: Boolean) {
        val vf = containingPsiFile.virtualFile ?: return
        OpenFileDescriptor(containingPsiFile.project, vf, offset).navigate(requestFocus)
    }

    override fun canNavigate(): Boolean = containingPsiFile.virtualFile != null

    override fun canNavigateToSource(): Boolean = canNavigate()

    override fun getName(): String = label

    override fun getPresentation(): ItemPresentation {
        return object : ItemPresentation {
            override fun getPresentableText(): String = label
            override fun getLocationString(): String {
                val vf = containingPsiFile.virtualFile ?: return ""
                val basePath = containingPsiFile.project.basePath ?: return vf.path
                return vf.path.removePrefix(basePath).removePrefix("/")
            }
            override fun getIcon(unused: Boolean): Icon? = containingPsiFile.getIcon(0)
        }
    }
}
