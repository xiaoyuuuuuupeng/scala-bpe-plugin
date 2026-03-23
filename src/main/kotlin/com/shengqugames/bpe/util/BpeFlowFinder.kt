package com.shengqugames.bpe.util

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile

/**
 * Locates the flow/scala implementation file for a given BPE service message.
 *
 * ScalaBPE has two code file formats under compose_conf/:
 *
 * 1. .flow files (simplified DSL, compiled by framework):
 *    - Naming: {messageName}_{messageId}.flow  (e.g. queryUserInfo_1.flow)
 *    - Also:   {serviceName}.{messageName}.flow (e.g. payCore.changeOrderState.flow)
 *    - Header: //$serviceName.messageName  （与 XML 中 &lt;service name&gt; / &lt;message name&gt; 对应，见 findByDollarCommentInFlowFiles）
 *
 * 2. .scala files (full Scala class):
 *    - Naming: {serviceName}.{messageName}.scala (e.g. subService.payRouter.doAlipayChannelRouter.scala)
 *    - Class:  Flow_{servicename_lower}_{messagename_lower}
 *
 * At runtime the framework loads: scalabpe.flow.Flow_{serviceName_lower}_{msgName_lower}
 *
 * 查找实现时优先 [BpeComposeConfIndex]（与 VSCode bp-bpeduo 预扫描一致），再使用路径/文件名/注释等回退策略。
 */
object BpeFlowFinder {

    data class SearchParams(
        val serviceName: String,
        val messageName: String,
        val messageId: String
    )

    /**
     * Find matching implementation files for a message. Returns all matches
     * (there should typically be exactly one).
     */
    fun findFiles(project: Project, params: SearchParams): List<VirtualFile> {
        val basePath = project.basePath ?: return emptyList()
        val composeDir = LocalFileSystem.getInstance().findFileByPath("$basePath/compose_conf")
            ?: return emptyList()

        // Strategy 0（对齐 VSCode bp-bpeduo）：compose_conf 预扫描索引，key = service.message
        BpeComposeConfIndex.findFiles(project, params.serviceName, params.messageName)
            .takeIf { it.isNotEmpty() }
            ?.let { return it.distinct() }

        val results = mutableListOf<VirtualFile>()

        // Strategy 1: {messageName}_{messageId}.flow in simpleflows/{serviceName}/
        findBySimpleFlowPath(basePath, params)?.let { results.add(it) }

        // Strategy 2: filename ends with .{messageName}.flow or .{messageName}.scala (case-insensitive)
        // Strategy 3: filename is {serviceName}.{messageName}.{flow|scala} (case-insensitive)
        if (results.isEmpty()) {
            findByFilenameScan(composeDir, params, results)
        }

        // Strategy 4: .flow 文件首行/注释中的 //$serviceName.messageName（与反向导航 FLOW_COMMENT 一致）
        if (results.isEmpty()) {
            findByDollarCommentInFlowFiles(composeDir, params, results)
        }

        // Strategy 5: search .scala files for class Flow_{servicename}_{messagename}
        if (results.isEmpty()) {
            findByClassNameInContent(composeDir, params, results)
        }

        return results
    }

    /**
     * Quick check: does at least one implementation file exist?
     */
    fun hasFile(project: Project, params: SearchParams): Boolean {
        val basePath = project.basePath ?: return false
        val composeDir = LocalFileSystem.getInstance().findFileByPath("$basePath/compose_conf")
            ?: return false

        if (BpeComposeConfIndex.hasFile(project, params.serviceName, params.messageName)) return true

        if (findBySimpleFlowPath(basePath, params) != null) return true

        val results = mutableListOf<VirtualFile>()
        findByFilenameScan(composeDir, params, results)
        if (results.isNotEmpty()) return true

        findByDollarCommentInFlowFiles(composeDir, params, results)
        if (results.isNotEmpty()) return true

        findByClassNameInContent(composeDir, params, results)
        return results.isNotEmpty()
    }

    /**
     * Strategy 1: compose_conf/simpleflows/{serviceName}/{messageName}_{messageId}.flow
     */
    private fun findBySimpleFlowPath(basePath: String, params: SearchParams): VirtualFile? {
        val path = "$basePath/compose_conf/simpleflows/${params.serviceName}/${params.messageName}_${params.messageId}.flow"
        return LocalFileSystem.getInstance().findFileByPath(path)
    }

    /**
     * Strategy 2 & 3: scan compose_conf/ recursively for files matching name patterns.
     *
     * Matches (case-insensitive):
     *   - {serviceName}.{messageName}.scala
     *   - {serviceName}.{messageName}.flow
     *   - Any file ending with .{messageName}.scala or .{messageName}.flow
     */
    private fun findByFilenameScan(dir: VirtualFile, params: SearchParams, results: MutableList<VirtualFile>) {
        val exactSuffix = ".${params.messageName}.scala".lowercase()
        val exactFlowSuffix = ".${params.messageName}.flow".lowercase()
        val fullScalaName = "${params.serviceName}.${params.messageName}.scala".lowercase()
        val fullFlowName = "${params.serviceName}.${params.messageName}.flow".lowercase()

        scanDirectory(dir) { file ->
            val nameLower = file.name.lowercase()
            when {
                nameLower == fullScalaName || nameLower == fullFlowName -> results.add(file)
                nameLower.endsWith(exactSuffix) || nameLower.endsWith(exactFlowSuffix) -> results.add(file)
            }
        }
    }

    /**
     * 在 compose_conf 下所有 .flow 内容中查找 `//$serviceName.messageName`（大小写不敏感）。
     * 与 .flow 中约定的声明注释一致，解决仅依赖文件名找不到实现的情况。
     */
    private fun findByDollarCommentInFlowFiles(dir: VirtualFile, params: SearchParams, results: MutableList<VirtualFile>) {
        // 与 .flow 中 //$serviceName.messageName 一致；允许 // 与 $ 间空白
        val re = Regex(
            "//\\s*\\$" + Regex.escape(params.serviceName) + "\\." + Regex.escape(params.messageName),
            RegexOption.IGNORE_CASE
        )

        scanDirectory(dir) { file ->
            if (file.extension?.lowercase() != "flow" || file in results) return@scanDirectory
            try {
                val content = String(file.contentsToByteArray(), Charsets.UTF_8)
                if (re.containsMatchIn(content)) {
                    results.add(file)
                }
            } catch (_: Exception) {
                // skip
            }
        }
    }

    /**
     * Strategy 5: scan .scala files for class declaration matching
     * Flow_{servicename_lower}_{messagename_lower}
     */
    private fun findByClassNameInContent(dir: VirtualFile, params: SearchParams, results: MutableList<VirtualFile>) {
        val targetClassName = "Flow_${params.serviceName.lowercase()}_${params.messageName.lowercase()}"

        scanDirectory(dir) { file ->
            if (file.extension == "scala" && file !in results) {
                try {
                    val content = String(file.contentsToByteArray(), Charsets.UTF_8)
                    if (content.contains("class $targetClassName")) {
                        results.add(file)
                    }
                } catch (_: Exception) {
                    // skip unreadable files
                }
            }
        }
    }

    private fun scanDirectory(dir: VirtualFile, visitor: (VirtualFile) -> Unit) {
        for (child in dir.children) {
            if (child.isDirectory) {
                scanDirectory(child, visitor)
            } else {
                visitor(child)
            }
        }
    }
}
