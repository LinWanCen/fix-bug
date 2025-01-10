package io.github.linwancen.plugin.fix.common

import com.intellij.openapi.editor.Document
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil

object AnnoUtils {
    @JvmStatic
    fun add(element: PsiElement, document: Document, classFullName: String, classSimpleName: String) {
        val psiClass = PsiTreeUtil.getParentOfType(element, PsiClass::class.java) ?: return
        val docComment = psiClass.docComment
        if (docComment != null) {
            val insertIndex = docComment.textRange.endOffset
            document.insertString(insertIndex, "\n@$classSimpleName" +
                    "")
        } else {
            val insertIndex = psiClass.textRange.startOffset
            document.insertString(insertIndex, "@$classSimpleName\n")
        }
        ImportFixUtils.add(document, psiClass, classFullName)
    }
}