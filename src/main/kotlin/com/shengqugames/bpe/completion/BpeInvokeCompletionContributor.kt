package com.shengqugames.bpe.completion

import com.intellij.codeInsight.completion.*
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.codeInsight.template.TemplateManager
import com.intellij.codeInsight.template.impl.ConstantNode
import com.intellij.icons.AllIcons
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.patterns.PlatformPatterns
import com.intellij.psi.PsiManager
import com.intellij.psi.xml.XmlFile
import com.intellij.psi.xml.XmlTag
import com.intellij.util.ProcessingContext

class BpeInvokeCompletionContributor : CompletionContributor() {
    init {
        extend(CompletionType.BASIC, PlatformPatterns.psiElement(), BpeInvokeCompletionProvider())
    }
}

/**
 * Fuzzy subsequence matcher: all characters of the prefix must appear
 * in order within the candidate name (case-insensitive).
 *
 * e.g. "dbPins" matches "dbPayRouter.insertPayCollectionInfo"
 */
private class BpeFuzzyMatcher(prefix: String) : PrefixMatcher(prefix) {

    override fun prefixMatches(name: String): Boolean {
        if (myPrefix.isEmpty()) return true
        val prefixLower = myPrefix.lowercase()
        val nameLower = name.lowercase()
        var nameIdx = 0
        for (ch in prefixLower) {
            nameIdx = nameLower.indexOf(ch, nameIdx)
            if (nameIdx < 0) return false
            nameIdx++
        }
        return true
    }

    override fun cloneWithPrefix(prefix: String): PrefixMatcher {
        return BpeFuzzyMatcher(prefix)
    }
}

private class BpeInvokeCompletionProvider : CompletionProvider<CompletionParameters>() {

    companion object {
        /** 第二参数为 service.message 字符串：invoke(...) 与 invokeWithNoReply(...) */
        val INVOKE_SECOND_PARAM = Regex("""(?:invokeWithNoReply|invoke)\s*\([^,]+,\s*"([^"]*)$""")
    }

    override fun addCompletions(
        parameters: CompletionParameters,
        context: ProcessingContext,
        result: CompletionResultSet
    ) {
        val file = parameters.originalFile.virtualFile ?: return
        val ext = file.extension?.lowercase()
        if (ext != "scala" && ext != "flow") return

        val document = parameters.editor.document
        val offset = parameters.offset

        val lookBackStart = maxOf(0, offset - 500)
        val textWindow = document.getText(TextRange(lookBackStart, offset))

        val match = INVOKE_SECOND_PARAM.find(textWindow) ?: return
        val prefix = match.groupValues[1]
        val fuzzyResult = result.withPrefixMatcher(BpeFuzzyMatcher(prefix))

        val project = parameters.position.project
        val basePath = project.basePath ?: return
        val avenueDir = LocalFileSystem.getInstance().findFileByPath("$basePath/avenue_conf") ?: return
        val psiManager = PsiManager.getInstance(project)

        scanXmlDir(avenueDir) { xmlFile ->
            val psiFile = psiManager.findFile(xmlFile) as? XmlFile ?: return@scanXmlDir
            val rootTag = psiFile.rootTag ?: return@scanXmlDir
            if (rootTag.name != "service") return@scanXmlDir

            val svcName = rootTag.getAttributeValue("name") ?: return@scanXmlDir
            val typeMap = buildTypeMap(rootTag)

            for (msgTag in rootTag.findSubTags("message")) {
                val msgName = msgTag.getAttributeValue("name") ?: continue
                val fullName = "$svcName.$msgName"
                val reqParams = extractRequestParams(msgTag, typeMap)
                val tailText = if (reqParams.isEmpty()) "" else "  (${reqParams.size} params)"

                val element = LookupElementBuilder.create(fullName)
                    .withIcon(AllIcons.Nodes.Method)
                    .withTailText(tailText, true)
                    .withTypeText(xmlFile.nameWithoutExtension, true)
                    .withInsertHandler(BpeInvokeInsertHandler(reqParams))

                fuzzyResult.addElement(element)
            }
        }
    }

    private fun buildTypeMap(serviceTag: XmlTag): Map<String, String> {
        val map = mutableMapOf<String, String>()
        for (typeTag in serviceTag.findSubTags("type")) {
            val name = typeTag.getAttributeValue("name") ?: continue
            val clazz = typeTag.getAttributeValue("class") ?: continue
            map[name] = clazz
        }
        return map
    }

    private fun extractRequestParams(messageTag: XmlTag, typeMap: Map<String, String>): List<Pair<String, String>> {
        val reqTag = messageTag.findFirstSubTag("requestParameter") ?: return emptyList()
        return reqTag.findSubTags("field").mapNotNull { field ->
            val name = field.getAttributeValue("name") ?: return@mapNotNull null
            val typeName = field.getAttributeValue("type") ?: ""
            val typeClass = typeMap[typeName] ?: typeName.removeSuffix("_type")
            Pair(name, typeClass)
        }
    }

    private fun scanXmlDir(dir: VirtualFile, visitor: (VirtualFile) -> Unit) {
        for (child in dir.children) {
            if (child.isDirectory) scanXmlDir(child, visitor)
            else if (child.extension == "xml") visitor(child)
        }
    }
}

/**
 * Inserts serviceName.messageName with closing quote, then starts a
 * Live Template so each parameter value is a Tab-stop the user can
 * select, edit, and press Tab to jump to the next one.
 */
private class BpeInvokeInsertHandler(
    private val params: List<Pair<String, String>>
) : InsertHandler<LookupElement> {

    override fun handleInsert(context: InsertionContext, item: LookupElement) {
        val document = context.document
        val editor = context.editor
        val tailOffset = context.tailOffset

        val lineNum = document.getLineNumber(tailOffset)
        val lineStart = document.getLineStartOffset(lineNum)
        val lineText = document.getText(TextRange(lineStart, tailOffset))

        val quoteIdx = lineText.lastIndexOf('"')
        if (quoteIdx < 0) return

        val replaceStart = lineStart + quoteIdx + 1
        val fullName = item.lookupString

        document.replaceString(replaceStart, tailOffset, fullName)
        var newOffset = replaceStart + fullName.length

        if (newOffset < document.textLength &&
            document.getText(TextRange(newOffset, newOffset + 1)) == "\"") {
            newOffset++
        } else {
            document.insertString(newOffset, "\"")
            newOffset++
        }

        context.commitDocument()
        editor.caretModel.moveToOffset(newOffset)

        if (params.isEmpty()) return

        val invokeLineText = document.getText(TextRange(lineStart, replaceStart))
        val baseIndent = invokeLineText.takeWhile { it == ' ' || it == '\t' }
        val paramIndent = baseIndent + "      "

        val templateManager = TemplateManager.getInstance(context.project)
        val template = templateManager.createTemplate("bpe_invoke_params", "BPE")
        template.isToReformat = false

        template.addTextSegment(", timeout,\n")
        for ((i, param) in params.withIndex()) {
            val (name, typeClass) = param
            val placeholder = when (typeClass) {
                "int" -> "0"
                "long" -> "0L"
                else -> "\"\""
            }
            template.addTextSegment("$paramIndent\"$name\" -> ")
            template.addVariable("val_$name", ConstantNode(placeholder), true)
            if (i < params.size - 1) template.addTextSegment(",\n")
        }
        template.addTextSegment(")")
        template.addEndVariable()

        templateManager.startTemplate(editor, template)
    }
}
