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
        val targets = com.shengqugames.bpe.util.BpeXmlFinder
            .findMessageElements(element.project, svcLower, msgLower)
        return targets.firstOrNull()
    }

    override fun getVariants(): Array<Any> = emptyArray()
}
