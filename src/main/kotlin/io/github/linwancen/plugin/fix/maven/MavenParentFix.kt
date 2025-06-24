package io.github.linwancen.plugin.fix.maven

import com.intellij.openapi.editor.Document
import com.intellij.psi.PsiElement
import com.intellij.psi.xml.XmlTag
import io.github.linwancen.plugin.fix.common.AbstractFix

open class MavenParentFix : AbstractFix() {
    companion object {
        val INSTANCE = MavenParentFix()
    }

    override fun getFamilyName(): String {
        return """=> add <relativePath/>"""
    }

    override fun documentFix(element: PsiElement, document: Document) {
        if (element !is XmlTag) {
            return
        }
        val version = element.findFirstSubTag("version") ?: return
        val space = version.prevSibling.text
        val insertIndex = version.textRange.endOffset
        document.insertString(insertIndex, "$space<relativePath/>")
    }
}