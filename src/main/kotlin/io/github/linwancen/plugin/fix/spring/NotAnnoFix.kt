package io.github.linwancen.plugin.fix.spring

import com.intellij.openapi.editor.Document
import com.intellij.psi.PsiElement
import io.github.linwancen.plugin.fix.common.AbstractFix
import io.github.linwancen.plugin.fix.common.AnnoUtils

open class NotAnnoFix : AbstractFix() {
    companion object {
        val INSTANCE = NotAnnoFix()
    }

    override fun getFamilyName(): String {
        return """=> add @Service"""
    }

    override fun documentFix(element: PsiElement, document: Document) {
        AnnoUtils.add(element, document, "org.springframework.stereotype.Service", "Service")
    }
}