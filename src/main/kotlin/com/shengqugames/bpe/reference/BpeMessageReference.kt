package com.shengqugames.bpe.reference

import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.*
import com.intellij.psi.xml.XmlAttributeValue
import com.shengqugames.bpe.util.BpeFlowFinder
import com.shengqugames.bpe.util.BpeInvokeFinder
import com.shengqugames.bpe.util.NoInvokeCallSiteElement

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
        if (callSites.isEmpty()) {
            // 无 invoke 时仍提供可解析目标，导航时弹出说明，避免 IDE「找不到要转到的声明」
            return arrayOf(
                PsiElementResolveResult(NoInvokeCallSiteElement(element, serviceName, messageName))
            )
        }
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
