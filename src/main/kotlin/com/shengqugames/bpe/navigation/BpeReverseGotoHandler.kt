package com.shengqugames.bpe.navigation

import com.intellij.codeInsight.navigation.actions.GotoDeclarationHandler
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiWhiteSpace
import com.shengqugames.bpe.util.BpeXmlFinder

/**
 * 与 [com.shengqugames.bpe.reference.BpeScalaReferenceContributor] 配合：
 * 安装 Scala 官方插件后，类名上的 Ctrl+Click 会优先走 Scala 的声明解析；
 * 通过 GotoDeclarationHandler 额外提供 XML &lt;message&gt; 目标，与 Scala 目标一并出现（多目标时弹出选择）。
 *
 * 使用 [PsiElement.containingFile.findElementAt] 定位 offset 处真实 token，
 * 避免 sourceElement 为整段 class 体时因长文本被提前 return。
 */
class BpeReverseGotoHandler : GotoDeclarationHandler {

    companion object {
        /** 与 BpeScalaReferenceContributor.CLASS_NAME_REGEX 保持一致 */
        val CLASS_NAME_PATTERN = Regex("""Flow_([a-zA-Z0-9]+)_([a-zA-Z0-9]+)""")
    }

    override fun getGotoDeclarationTargets(
        sourceElement: PsiElement?,
        offset: Int,
        editor: Editor
    ): Array<PsiElement>? {
        val containingFile = sourceElement?.containingFile ?: return null
        val vf = containingFile.virtualFile ?: return null
        val ext = vf.extension?.lowercase() ?: return null
        if (ext != "scala" && ext != "flow") return null

        var el = containingFile.findElementAt(offset) ?: return null
        if (el is PsiWhiteSpace) {
            el = containingFile.findElementAt(offset - 1) ?: return null
        }

        val text = el.text
        if (text.contains('\n') || text.length > 200) return null

        val match = CLASS_NAME_PATTERN.matchEntire(text) ?: return null
        val svcLower = match.groupValues[1].lowercase()
        val msgLower = match.groupValues[2].lowercase()

        val targets = BpeXmlFinder.findMessageElements(el.project, svcLower, msgLower)
        return if (targets.isNotEmpty()) targets.toTypedArray() else null
    }
}
