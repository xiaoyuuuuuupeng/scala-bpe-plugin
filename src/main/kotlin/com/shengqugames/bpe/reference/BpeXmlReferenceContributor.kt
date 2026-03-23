package com.shengqugames.bpe.reference

import com.intellij.patterns.PlatformPatterns
import com.intellij.psi.PsiReferenceContributor
import com.intellij.psi.PsiReferenceRegistrar
import com.intellij.psi.xml.XmlAttributeValue

class BpeXmlReferenceContributor : PsiReferenceContributor() {
    override fun registerReferenceProviders(registrar: PsiReferenceRegistrar) {
        registrar.registerReferenceProvider(
            PlatformPatterns.psiElement(XmlAttributeValue::class.java),
            BpeMessageReferenceProvider()
        )
        registrar.registerReferenceProvider(
            PlatformPatterns.psiElement(XmlAttributeValue::class.java),
            BpeServiceNameReferenceProvider()
        )
        registrar.registerReferenceProvider(
            PlatformPatterns.psiElement(XmlAttributeValue::class.java),
            BpeXmlTypeReferenceProvider()
        )
    }
}
