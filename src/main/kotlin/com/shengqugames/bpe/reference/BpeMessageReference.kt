package com.shengqugames.bpe.reference

import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.*
import com.intellij.psi.xml.XmlAttributeValue
import com.shengqugames.bpe.util.BpeFlowFinder
import com.shengqugames.bpe.util.BpeInvokeFinder

/**
 * Ctrl+Click on <message name="..."> → navigate to invoke call sites only.
 * (Implementation files are handled by the gutter icon in BpeLineMarkerProvider.)
 */
class BpeMessageReference(
    element: XmlAttributeValue,
    val messageName: String,
    val messageId: String,
    val serviceName: String
) : PsiPolyVariantReferenceBase<XmlAttributeValue>(element) {

    override fun multiResolve(incompleteCode: Boolean): Array<ResolveResult> {
        val callSites = BpeInvokeFinder.findCallSites(element.project, serviceName, messageName)
        return callSites.map { PsiElementResolveResult(it) }.toTypedArray()
    }

    override fun resolve(): PsiElement? {
        val results = multiResolve(false)
        if (results.size == 1) return results[0].element
        return null
    }

    fun findFlowFiles(): List<VirtualFile> {
        val params = BpeFlowFinder.SearchParams(serviceName, messageName, messageId)
        return BpeFlowFinder.findFiles(element.project, params)
    }

    override fun isSoft(): Boolean = true

    override fun getVariants(): Array<Any> = emptyArray()
}
