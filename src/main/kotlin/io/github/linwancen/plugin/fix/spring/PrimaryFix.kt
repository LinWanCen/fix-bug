package io.github.linwancen.plugin.fix.spring

import com.intellij.openapi.editor.Document
import com.intellij.psi.PsiElement
import io.github.linwancen.plugin.fix.common.AbstractFix
import io.github.linwancen.plugin.fix.common.AnnoUtils

open class PrimaryFix : AbstractFix() {
    companion object {
        val INSTANCE = PrimaryFix()
    }

    override fun getFamilyName(): String {
        return """=> add @Primary"""
    }

    override fun documentFix(element: PsiElement, document: Document) {
        AnnoUtils.add(element, document, "org.springframework.context.annotation.Primary", "Primary")
    }
}