package com.shengqugames.bpe.action

import com.intellij.codeInsight.hint.HintManager
import com.intellij.ide.util.DefaultPsiElementCellRenderer
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.pom.Navigatable
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.shengqugames.bpe.reference.findBpeFlowToXmlReferenceAt
import com.shengqugames.bpe.util.BpeImplementationFileServiceMessage
import com.shengqugames.bpe.util.BpeXmlFinder

/**
 * 在 .scala / .flow 编辑器中右键：跳转到 avenue_conf 中对应的 XML &lt;message&gt; 定义。
 * 优先使用光标处的 [BpeFlowToXmlReference]；若无，则从整文件解析 class Flow_… 或 `//$` 注释（可在任意位置触发）。
 */
class BpeGoToXmlDefinitionAction : AnAction() {

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    override fun update(e: AnActionEvent) {
        val file = e.getData(CommonDataKeys.VIRTUAL_FILE)
        val ext = file?.extension?.lowercase()
        e.presentation.isEnabledAndVisible = ext == "scala" || ext == "flow"
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val editor = e.getData(CommonDataKeys.EDITOR) ?: return
        val psiFile = PsiDocumentManager.getInstance(project).getPsiFile(editor.document) ?: return
        val doc = editor.document
        val offset = editor.caretModel.offset
        val maxOff = (doc.textLength - 1).coerceAtLeast(0)

        val bpeRef = findBpeFlowToXmlReferenceAt(psiFile, offset)
            ?: findBpeFlowToXmlReferenceAt(psiFile, (offset - 1).coerceAtLeast(0))
            ?: findBpeFlowToXmlReferenceAt(psiFile, (offset + 1).coerceAtMost(maxOff))

        val targets = if (bpeRef != null) {
            bpeRef.resolveAll()
        } else {
            val pair = BpeImplementationFileServiceMessage.parseFromPsiFile(psiFile)
            if (pair == null) {
                showHint(
                    editor,
                    "无法关联到 XML message：当前文件缺少可识别的 class Flow_… 声明或 //$ 注释"
                )
                return
            }
            BpeXmlFinder.findMessageElements(project, pair.first, pair.second)
        }
        when {
            targets.isEmpty() ->
                showHint(editor, "找不到对应的 XML message 定义（请确认 avenue_conf 中存在该 service/message）")
            targets.size == 1 -> {
                val nav = targets.first() as? Navigatable
                if (nav != null && nav.canNavigate()) nav.navigate(true)
            }
            else ->
                showChooser(editor, targets)
        }
    }

    private fun showHint(editor: Editor, message: String) {
        HintManager.getInstance().showInformationHint(editor, message)
    }

    private fun showChooser(editor: Editor, targets: List<PsiElement>) {
        val popup = JBPopupFactory.getInstance().createPopupChooserBuilder(targets)
            .setTitle("选择 XML message 定义")
            .setItemChosenCallback { chosen ->
                val nav = chosen as? Navigatable
                if (nav != null && nav.canNavigate()) nav.navigate(true)
            }
            .setRenderer(DefaultPsiElementCellRenderer())
            .createPopup()

        val point = editor.visualPositionToXY(editor.caretModel.visualPosition)
        popup.showInScreenCoordinates(editor.component, point)
    }
}
