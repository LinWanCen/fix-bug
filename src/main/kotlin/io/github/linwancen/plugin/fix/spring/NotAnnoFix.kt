package io.github.linwancen.plugin.fix.spring

import com.intellij.openapi.editor.Document
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import io.github.linwancen.plugin.fix.common.AbstractFix
import io.github.linwancen.plugin.fix.common.ImportFixUtils

open class NotAnnoFix : AbstractFix() {
    companion object {
        val INSTANCE = NotAnnoFix()
    }

    override fun getFamilyName(): String {
        return """=> add @Service"""
    }

    override fun documentFix(element: PsiElement, document: Document) {
        val psiClass = PsiTreeUtil.getParentOfType(element, PsiClass::class.java) ?: return
        val docComment = psiClass.docComment
        if (docComment != null) {
            val insertIndex = docComment.textRange.endOffset
            document.insertString(insertIndex, "\n@Service")
        } else {
            val insertIndex = psiClass.textRange.startOffset
            document.insertString(insertIndex, "@Service\n")
        }
        ImportFixUtils.add(document, psiClass, "org.springframework.stereotype.Service")
    }
}