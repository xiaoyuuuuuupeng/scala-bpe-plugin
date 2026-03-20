package com.shengqugames.bpe.reference

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference
import com.intellij.psi.PsiReferenceProvider
import com.intellij.psi.xml.XmlAttribute
import com.intellij.psi.xml.XmlAttributeValue
import com.intellij.psi.xml.XmlTag
import com.intellij.util.ProcessingContext

class BpeMessageReferenceProvider : PsiReferenceProvider() {
    override fun getReferencesByElement(
        element: PsiElement,
        context: ProcessingContext
    ): Array<PsiReference> {
        val attrValue = element as? XmlAttributeValue ?: return PsiReference.EMPTY_ARRAY
        val attr = attrValue.parent as? XmlAttribute ?: return PsiReference.EMPTY_ARRAY

        if (attr.name != "name") return PsiReference.EMPTY_ARRAY

        val messageTag = attr.parent as? XmlTag ?: return PsiReference.EMPTY_ARRAY
        if (messageTag.name != "message") return PsiReference.EMPTY_ARRAY

        val serviceTag = messageTag.parentTag
        if (serviceTag?.name != "service") return PsiReference.EMPTY_ARRAY

        val messageName = attrValue.value.takeIf { it.isNotEmpty() } ?: return PsiReference.EMPTY_ARRAY
        val messageId = messageTag.getAttributeValue("id")?.takeIf { it.isNotEmpty() }
            ?: return PsiReference.EMPTY_ARRAY
        val serviceName = serviceTag.getAttributeValue("name")?.takeIf { it.isNotEmpty() }
            ?: return PsiReference.EMPTY_ARRAY

        return arrayOf(BpeMessageReference(attrValue, messageName, messageId, serviceName))
    }
}
