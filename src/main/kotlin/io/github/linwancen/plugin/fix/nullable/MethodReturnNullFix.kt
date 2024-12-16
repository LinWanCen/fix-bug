package io.github.linwancen.plugin.fix.nullable

import com.intellij.openapi.editor.Document
import com.intellij.psi.*
import com.intellij.psi.util.PsiTreeUtil
import io.github.linwancen.plugin.fix.common.AbstractFix
import java.util.regex.Pattern

open class MethodReturnNullFix : AbstractFix() {
    companion object {
        @JvmStatic
        val NEW_LINE: Pattern = Pattern.compile("(?:\r\n|\r|\n)[ \t]++$")
        val INSTANCE = MethodReturnNullFix()
    }

    override fun getFamilyName(): String {
        return """=> if (v == null) { return/continue; }"""
    }

    override fun documentFix(element: PsiElement, document: Document) {
        val line = PsiTreeUtil.getParentOfType(element, PsiLocalVariable::class.java) ?: return
        val insertIndex = line.textRange?.endOffset ?: return
        var space = line.parent.prevSibling.text
        val matcher = NEW_LINE.matcher(space)
        if (matcher.find()) {
            space = matcher.group()
        }
        val text = returnText(element)
        document.insertString(insertIndex, """${space}if (${element.text} == null) {${space}    $text${space}}""")
    }

    open fun returnText(element: PsiElement): String {
        if (PsiTreeUtil.getParentOfType(element, PsiLoopStatement::class.java) != null) {
            return "continue;"
        }
        val method = PsiTreeUtil.getParentOfType(element, PsiMethod::class.java) ?: return "return;"
        return PsiTreeUtil.findChildOfType(method, PsiReturnStatement::class.java)?.text ?: "return;"
    }
}