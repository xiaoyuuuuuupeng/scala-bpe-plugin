package com.shengqugames.bpe.util

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import java.util.concurrent.ConcurrentHashMap

/**
 * 对齐 VSCode 插件 bp-bpeduo 的 AvenueFlowClassScanner：预扫描 compose_conf，
 * 索引 key 为 serviceName.messageName（小写），等价于 VSCode 的 flowClassList.get。
 *
 * .flow：首行注释中解析 service、message（与 getServiceMessageNameFromFlow 同语义）。
 * .scala：class Flow_svc_msg extends（与 getServiceMessageNameFromScala 同语义）。
 */
object BpeComposeConfIndex {

    /** basePath -> ( "service.message".lowercase() -> 文件路径列表 ) */
    private val cache = ConcurrentHashMap<String, Map<String, List<String>>>()

    /**
     * 索引失效（保存 compose_conf 下文件后可调用，下次查找会重建）
     */
    fun invalidate(project: Project) {
        project.basePath?.let { cache.remove(it) }
    }

    fun findFiles(project: Project, serviceName: String, messageName: String): List<VirtualFile> {
        val key = indexKey(serviceName, messageName)
        val map = getOrBuildMap(project) ?: return emptyList()
        val paths = map[key] ?: return emptyList()
        val fs = LocalFileSystem.getInstance()
        return paths.mapNotNull { fs.findFileByPath(it) }
    }

    fun hasFile(project: Project, serviceName: String, messageName: String): Boolean {
        val key = indexKey(serviceName, messageName)
        val map = getOrBuildMap(project) ?: return false
        return map.containsKey(key)
    }

    /**
     * 某 service 下全部实现：索引中 key 以 `serviceName.` 开头的条目，并合并「文件名以 `serviceName.` 开头」的 .scala / .flow（去重）。
     */
    fun findAllImplementationFilesForService(project: Project, serviceName: String): List<VirtualFile> {
        if (serviceName.isBlank()) return emptyList()
        val base = project.basePath ?: return emptyList()
        val fs = LocalFileSystem.getInstance()
        val pathSet = linkedSetOf<String>()

        getOrBuildMap(project)?.let { map ->
            val prefix = "${serviceName.lowercase()}."
            for ((key, paths) in map) {
                if (key.startsWith(prefix)) pathSet.addAll(paths)
            }
        }

        collectFilesByFilenamePrefix(base, serviceName) { pathSet.add(it.path) }

        return pathSet.mapNotNull { fs.findFileByPath(it) }
    }

    private fun collectFilesByFilenamePrefix(
        basePath: String,
        serviceName: String,
        addPath: (VirtualFile) -> Unit
    ) {
        val composeDir = LocalFileSystem.getInstance().findFileByPath("$basePath/compose_conf") ?: return
        val prefix = "${serviceName}.".lowercase()
        scanDirectory(composeDir) { file ->
            val ext = file.extension?.lowercase() ?: return@scanDirectory
            if (ext != "scala" && ext != "flow") return@scanDirectory
            if (file.name.lowercase().startsWith(prefix)) addPath(file)
        }
    }

    private fun indexKey(serviceName: String, messageName: String): String =
        "${serviceName.lowercase()}.${messageName.lowercase()}"

    private fun getOrBuildMap(project: Project): Map<String, List<String>>? {
        val base = project.basePath ?: return null
        return cache.computeIfAbsent(base) { buildIndex(it) }
    }

    private fun buildIndex(basePath: String): Map<String, List<String>> {
        val composeDir = LocalFileSystem.getInstance().findFileByPath("$basePath/compose_conf")
            ?: return emptyMap()
        val acc = mutableMapOf<String, MutableList<String>>()
        scanDirectory(composeDir) { file ->
            when (file.extension?.lowercase()) {
                "flow" -> indexFlowFile(file, acc)
                "scala" -> indexScalaFile(file, acc)
            }
        }
        return acc.mapValues { it.value.toList() }
    }

    // 与 VSCode getServiceMessageNameFromFlow 一致：两段 word，中间任意单字符（多为点号）
    private val flowHeaderVsCode = Regex("""^\s*//\s*\$\s*(\w*).(\w*)""")

    // 与 VSCode getServiceMessageNameFromScala 一致
    private val scalaFlowClassVsCode = Regex("""^class\s*Flow_(\w+)_(\w+)\s+extends\s+""", RegexOption.MULTILINE)

    private fun indexFlowFile(file: VirtualFile, acc: MutableMap<String, MutableList<String>>) {
        val head = readHead(file, 16 * 1024) ?: return
        for (line in head.lineSequence()) {
            val m = flowHeaderVsCode.find(line) ?: continue
            val svc = m.groupValues[1]
            val msg = m.groupValues[2]
            if (svc.isEmpty() && msg.isEmpty()) continue
            add(acc, file.path, svc, msg)
            return
        }
    }

    private fun indexScalaFile(file: VirtualFile, acc: MutableMap<String, MutableList<String>>) {
        val content = readHead(file, 512 * 1024) ?: return
        val m = scalaFlowClassVsCode.find(content) ?: return
        val svc = m.groupValues[1]
        val msg = m.groupValues[2]
        add(acc, file.path, svc, msg)
    }

    private fun add(
        acc: MutableMap<String, MutableList<String>>,
        path: String,
        serviceName: String,
        messageName: String
    ) {
        val key = indexKey(serviceName, messageName)
        acc.getOrPut(key) { mutableListOf() }.add(path)
    }

    private fun readHead(file: VirtualFile, maxBytes: Int): String? {
        return try {
            val bytes = file.contentsToByteArray()
            val n = minOf(bytes.size, maxBytes)
            String(bytes, 0, n, Charsets.UTF_8)
        } catch (_: Exception) {
            null
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
