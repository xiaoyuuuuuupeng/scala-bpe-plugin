package com.shengqugames.bpe.reference

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference
import com.intellij.psi.PsiReferenceProvider
import com.intellij.psi.xml.XmlAttribute
import com.intellij.psi.xml.XmlAttributeValue
import com.intellij.util.ProcessingContext
import com.shengqugames.bpe.util.BpeXmlFinder

/**
 * [requestParameter]/[responseParameter] 等下的 `<field type="某类型名"/>` → 解析到 `<type name="某类型名"/>`
 */
class BpeXmlTypeReferenceProvider : PsiReferenceProvider() {

    override fun getReferencesByElement(
        element: PsiElement,
        context: ProcessingContext
    ): Array<PsiReference> {
        val attrValue = element as? XmlAttributeValue ?: return PsiReference.EMPTY_ARRAY
        val file = attrValue.containingFile.virtualFile ?: return PsiReference.EMPTY_ARRAY
        if (!BpeXmlFinder.isAvenueConfXml(attrValue.project, file)) return PsiReference.EMPTY_ARRAY

        val attr = attrValue.parent as? XmlAttribute ?: return PsiReference.EMPTY_ARRAY
        if (attr.name != "type") return PsiReference.EMPTY_ARRAY

        val typeName = attrValue.value.trim().takeIf { it.isNotEmpty() } ?: return PsiReference.EMPTY_ARRAY

        return arrayOf(BpeXmlTypeReference(attrValue, typeName))
    }
}
