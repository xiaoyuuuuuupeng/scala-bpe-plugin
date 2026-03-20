package com.shengqugames.bpe.navigation

import com.intellij.codeInsight.navigation.actions.GotoDeclarationHandler
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiPlainText
import com.shengqugames.bpe.util.BpeXmlFinder

/**
 * Handles Ctrl+Click only for structured Scala PSI (when Scala plugin is installed).
 * For plain text files, returns null so PsiReferenceContributor provides precise-range navigation.
 */
class BpeReverseGotoHandler : GotoDeclarationHandler {

    companion object {
        val CLASS_NAME_PATTERN = Regex("""Flow_([a-z0-9]+)_([a-z0-9]+)""")
    }

    override fun getGotoDeclarationTargets(
        sourceElement: PsiElement?,
        offset: Int,
        editor: Editor
    ): Array<PsiElement>? {
        val element = sourceElement ?: return null

        // Plain text → let PsiReferenceContributor handle it (precise underline range)
        if (element is PsiPlainText) return null
        if (element.text.contains('\n') || element.text.length > 200) return null

        val file = element.containingFile?.virtualFile ?: return null
        val ext = file.extension?.lowercase()
        if (ext != "scala" && ext != "flow") return null

        // Structured Scala PSI: element is a single identifier token like "Flow_xxx_yyy"
        val match = CLASS_NAME_PATTERN.matchEntire(element.text) ?: return null
        val svcLower = match.groupValues[1]
        val msgLower = match.groupValues[2]

        val targets = BpeXmlFinder.findMessageElements(element.project, svcLower, msgLower)
        return if (targets.isNotEmpty()) targets.toTypedArray() else null
    }
}
