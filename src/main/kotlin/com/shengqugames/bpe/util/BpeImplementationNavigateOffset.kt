package com.shengqugames.bpe.util

import com.intellij.openapi.vfs.VirtualFile

/**
 * 从 XML 跳转到实现文件时，定位到关键位置而非文件开头：
 * - **.flow**：`//$serviceName.messageName` 注释行起始偏移
 * - **.scala**：`class Flow_... extends` 声明起始偏移
 */
object BpeImplementationNavigateOffset {

    fun getOffset(file: VirtualFile, serviceName: String, messageName: String): Int {
        return try {
            val content = String(file.contentsToByteArray(), Charsets.UTF_8)
            when (file.extension?.lowercase()) {
                "flow" -> offsetForFlow(content, serviceName, messageName)
                "scala" -> offsetForScala(content, serviceName, messageName)
                else -> 0
            }
        } catch (_: Exception) {
            0
        }
    }

    /** 对齐 BpeFlowFinder.findByDollarCommentInFlowFiles 的转义匹配 */
    private fun offsetForFlow(content: String, serviceName: String, messageName: String): Int {
        val exact = Regex(
            "//\\s*\\$" + Regex.escape(serviceName) + "\\." + Regex.escape(messageName),
            RegexOption.IGNORE_CASE
        )
        exact.find(content)?.range?.first?.let { return it }

        // 与索引/VSCode 一致的宽松行：^\s*//\s*\$\s*(\w*).(\w*)
        val lineRe = Regex("""(?m)^\s*//\s*\$\s*(\w*).(\w*)""")
        for (m in lineRe.findAll(content)) {
            val g1 = m.groupValues.getOrNull(1) ?: continue
            val g2 = m.groupValues.getOrNull(2) ?: continue
            if (g1.equals(serviceName, ignoreCase = true) && g2.equals(messageName, ignoreCase = true)) {
                return m.range.first
            }
        }
        return 0
    }

    private fun offsetForScala(content: String, serviceName: String, messageName: String): Int {
        val className = "Flow_${serviceName.lowercase()}_${messageName.lowercase()}"
        val escaped = Regex.escape(className)

        // 行首 class（常见：无 package 或 class 在单独一行）
        Regex("""(?m)^\s*class\s+$escaped\s+extends\s+""", RegexOption.IGNORE_CASE)
            .find(content)?.range?.first?.let { return it }

        // 有 package / import 时，class 不一定在行首
        Regex("""class\s+$escaped\s+extends\s+""", RegexOption.IGNORE_CASE)
            .find(content)?.range?.first?.let { return it }

        // 最后手段：子串（与 BpeFlowFinder.findByClassNameInContent 一致）
        val needle = "class $className"
        val idx = content.indexOf(needle, ignoreCase = true)
        return if (idx >= 0) idx else 0
    }
}
