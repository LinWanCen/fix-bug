package io.github.linwancen.plugin.fix.spring

import com.intellij.openapi.editor.Document
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiTypeElement
import com.intellij.psi.impl.source.PsiClassReferenceType
import com.intellij.psi.util.PsiTreeUtil
import io.github.linwancen.plugin.fix.common.AbstractFix
import io.github.linwancen.plugin.fix.common.ImportFixUtils

open class UseImplFix : AbstractFix() {
    companion object {
        val INSTANCE = UseImplFix()
    }

    override fun getFamilyName(): String {
        return """=> use interface"""
    }

    override fun documentFix(element: PsiElement, document: Document) {
        if (element !is PsiTypeElement) return
        val type = element.type
        if (type !is PsiClassReferenceType) return
        val psiClass = try {
            type.resolve() ?: return
        } catch (_: Throwable) {
            return
        }
        val interfaces = psiClass.interfaces
        if (interfaces.isEmpty()) return
        val thisClass = PsiTreeUtil.getParentOfType(element, PsiClass::class.java) ?: return
        val face = interfaces[0]
        val name = face.name ?: return
        val qualifiedName = face.qualifiedName ?: return
        val range = element.textRange ?: return
        document.replaceString(range.startOffset, range.endOffset, name)
        ImportFixUtils.add(document, thisClass, qualifiedName)
    }
}