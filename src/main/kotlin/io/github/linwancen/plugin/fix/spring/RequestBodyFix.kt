package io.github.linwancen.plugin.fix.spring

import com.intellij.openapi.editor.Document
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import io.github.linwancen.plugin.fix.common.AbstractFix
import io.github.linwancen.plugin.fix.common.ImportFixUtils

open class RequestBodyFix : AbstractFix() {
    companion object {
        val INSTANCE = RequestBodyFix()
    }

    override fun getFamilyName(): String {
        return """=> add @RequestBody"""
    }

    override fun documentFix(element: PsiElement, document: Document) {
        val insertIndex = element.textRange.startOffset
        document.insertString(insertIndex, "@RequestBody ")
        val psiClass = PsiTreeUtil.getParentOfType(element, PsiClass::class.java) ?: return
        ImportFixUtils.add(document, psiClass, "org.springframework.web.bind.annotation.RequestBody")
    }
}