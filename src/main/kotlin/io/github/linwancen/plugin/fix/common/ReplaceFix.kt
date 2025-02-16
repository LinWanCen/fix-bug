package io.github.linwancen.plugin.fix.common

import com.intellij.openapi.editor.Document
import com.intellij.psi.PsiElement

open class ReplaceFix(var s: String) : AbstractFix() {
    override fun getFamilyName(): String {
        return """=> replace $s"""
    }

    override fun documentFix(element: PsiElement, document: Document) {
        val range = element.textRange ?: return
        document.replaceString(range.startOffset, range.endOffset, s)
    }
}