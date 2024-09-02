package io.github.linwancen.plugin.fix.exception

import com.intellij.openapi.editor.Document
import com.intellij.psi.PsiElement

class UnusedExceptionThrowFix : UnusedExceptionLogFix() {
    companion object {
        val INSTANCE = UnusedExceptionThrowFix()
    }

    override fun getFamilyName(): String {
        return """=> throw new RuntimeException("", e);"""
    }

    override fun insertIntoCatch(
        catchBlock: PsiElement, document: Document,
        startSpace: String, addSpace: String, msg: String, e: PsiElement,
    ) {
        val rBrace = catchBlock.lastChild
        val insertIndex = rBrace.textRange?.startOffset ?: return
        document.insertString(insertIndex, """${addSpace}throw new RuntimeException("$msg", e);$startSpace""")
    }
}