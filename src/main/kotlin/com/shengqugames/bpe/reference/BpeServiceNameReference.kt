package com.shengqugames.bpe.reference

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementResolveResult
import com.intellij.psi.PsiManager
import com.intellij.psi.PsiPolyVariantReferenceBase
import com.intellij.psi.ResolveResult
import com.intellij.psi.xml.XmlAttributeValue
import com.shengqugames.bpe.util.BpeComposeConfIndex

/**
 * Ctrl+Click `<service name="...">` 的 name 值 → 列出该 service 下全部 .scala / .flow 实现文件（compose_conf）。
 */
class BpeServiceNameReference(
    element: XmlAttributeValue,
    private val serviceName: String
) : PsiPolyVariantReferenceBase<XmlAttributeValue>(element) {

    override fun multiResolve(incompleteCode: Boolean): Array<ResolveResult> {
        val targets = BpeComposeConfIndex.findAllImplementationFilesForService(element.project, serviceName)
        val psi = PsiManager.getInstance(element.project)
        return targets.mapNotNull { vf ->
            psi.findFile(vf)?.let { PsiElementResolveResult(it) }
        }.toTypedArray()
    }

    override fun resolve(): PsiElement? {
        val results = multiResolve(false)
        if (results.size == 1) return results[0].element
        return null
    }

    override fun isSoft(): Boolean = true

    override fun getVariants(): Array<Any> = emptyArray()
}
