package com.shengqugames.bpe.reference

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementResolveResult
import com.intellij.psi.PsiPolyVariantReferenceBase
import com.intellij.psi.ResolveResult
import com.intellij.psi.xml.XmlAttributeValue
import com.intellij.psi.xml.XmlFile
import com.shengqugames.bpe.util.BpeXmlFinder

/**
 * `type="x_sendCount_type"` → 跳转到 `<type name="x_sendCount_type" .../>`
 */
class BpeXmlTypeReference(
    element: XmlAttributeValue,
    private val typeName: String
) : PsiPolyVariantReferenceBase<XmlAttributeValue>(element) {

    override fun multiResolve(incompleteCode: Boolean): Array<ResolveResult> {
        val xmlFile = element.containingFile as? XmlFile ?: return emptyArray()
        val targets = BpeXmlFinder.findTypeDefinitionElementsInFile(xmlFile, typeName)
        return targets.map { PsiElementResolveResult(it) }.toTypedArray()
    }

    override fun resolve(): PsiElement? {
        val results = multiResolve(false)
        if (results.size == 1) return results[0].element
        return null
    }

    override fun isSoft(): Boolean = true

    override fun getVariants(): Array<Any> = emptyArray()
}
