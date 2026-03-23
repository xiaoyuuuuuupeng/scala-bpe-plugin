package com.shengqugames.bpe.reference

import com.intellij.openapi.util.TextRange
import com.intellij.pom.Navigatable
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReferenceBase

class BpeFlowToXmlReference(
    element: PsiElement,
    rangeInElement: TextRange,
    private val svcLower: String,
    private val msgLower: String
) : PsiReferenceBase<PsiElement>(element, rangeInElement) {

    override fun resolve(): PsiElement? {
        return resolveAll().firstOrNull()
    }

    /** 同一 message 可能对应多个 XML 文件时，返回全部 &lt;message&gt; 节点 */
    fun resolveAll(): List<PsiElement> {
        return com.shengqugames.bpe.util.BpeXmlFinder
            .findMessageElements(element.project, svcLower, msgLower)
    }

    override fun getVariants(): Array<Any> = emptyArray()
}
