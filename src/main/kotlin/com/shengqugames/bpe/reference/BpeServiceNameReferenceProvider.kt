package com.shengqugames.bpe.reference

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference
import com.intellij.psi.PsiReferenceProvider
import com.intellij.psi.xml.XmlAttribute
import com.intellij.psi.xml.XmlAttributeValue
import com.intellij.psi.xml.XmlTag
import com.intellij.util.ProcessingContext
import com.shengqugames.bpe.util.BpeXmlFinder

/**
 * 仅 `<service name="...">`，与 [BpeMessageReferenceProvider] 的 `<message name="...">` 区分。
 */
class BpeServiceNameReferenceProvider : PsiReferenceProvider() {

    override fun getReferencesByElement(
        element: PsiElement,
        context: ProcessingContext
    ): Array<PsiReference> {
        val attrValue = element as? XmlAttributeValue ?: return PsiReference.EMPTY_ARRAY
        val file = attrValue.containingFile.virtualFile ?: return PsiReference.EMPTY_ARRAY
        if (!BpeXmlFinder.isAvenueConfXml(attrValue.project, file)) return PsiReference.EMPTY_ARRAY

        val attr = attrValue.parent as? XmlAttribute ?: return PsiReference.EMPTY_ARRAY
        if (attr.name != "name") return PsiReference.EMPTY_ARRAY

        val tag = attr.parent as? XmlTag ?: return PsiReference.EMPTY_ARRAY
        if (tag.name != "service") return PsiReference.EMPTY_ARRAY

        val serviceName = attrValue.value.trim().takeIf { it.isNotEmpty() } ?: return PsiReference.EMPTY_ARRAY

        return arrayOf(BpeServiceNameReference(attrValue, serviceName))
    }
}
