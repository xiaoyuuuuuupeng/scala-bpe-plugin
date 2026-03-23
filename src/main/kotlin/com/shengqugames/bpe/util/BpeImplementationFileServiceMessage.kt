package com.shengqugames.bpe.util

import com.intellij.psi.PsiFile

/**
 * 从单个 .scala / .flow 实现文件内容解析 serviceName、messageName（与 compose_conf 索引、反向 gutter 规则一致），
 * 用于在编辑器任意位置触发「跳转到 XML」时无需落在 Flow_ / //$ / invoke 上。
 */
object BpeImplementationFileServiceMessage {

    private val CLASS_DECL = Regex(
        """class\s+Flow_([a-zA-Z0-9]+)_([a-zA-Z0-9]+)\s+extends""",
        setOf(RegexOption.IGNORE_CASE, RegexOption.MULTILINE)
    )

    /** 与索引一致：按行匹配首条 `//$service.message`（允许 service 段为空等宽松情况） */
    private val FLOW_LINE_HEADER = Regex("""^\s*//\s*\$\s*(\w*)\s*\.\s*(\w*)""")

    /** 全文内第一处 `//$a.b`（scala 无 class 声明时的兜底） */
    private val DOLLAR_COMMENT_ANYWHERE = Regex("""//\s*\$\s*(\w+)\.(\w+)""")

    fun parseFromPsiFile(psiFile: PsiFile): Pair<String, String>? {
        val vf = psiFile.virtualFile ?: return null
        val ext = vf.extension?.lowercase() ?: return null
        if (ext != "scala" && ext != "flow") return null
        return parseFromContent(psiFile.text, ext)
    }

    fun parseFromContent(content: String, ext: String): Pair<String, String>? {
        val scan = content.take(512 * 1024)

        CLASS_DECL.find(scan)?.let {
            return it.groupValues[1].lowercase() to it.groupValues[2].lowercase()
        }

        if (ext == "flow") {
            var lineNo = 0
            for (line in scan.lineSequence()) {
                if (lineNo++ > 500) break
                val m = FLOW_LINE_HEADER.find(line) ?: continue
                val s = m.groupValues[1]
                val msg = m.groupValues[2]
                if (s.isEmpty() && msg.isEmpty()) continue
                return s.lowercase() to msg.lowercase()
            }
        }

        DOLLAR_COMMENT_ANYWHERE.find(scan)?.let {
            return it.groupValues[1].lowercase() to it.groupValues[2].lowercase()
        }

        return null
    }
}
