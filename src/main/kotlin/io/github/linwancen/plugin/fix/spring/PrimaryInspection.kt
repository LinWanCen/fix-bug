package io.github.linwancen.plugin.fix.spring

import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.psi.*
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.searches.ClassInheritorsSearch
import com.intellij.psi.search.searches.ReferencesSearch
import com.intellij.psi.util.PsiTreeUtil
import io.github.linwancen.plugin.fix.SuppressFix
import io.github.linwancen.plugin.fix.ui.I18n

class PrimaryInspection : NotAnnoInspection() {

    fun qualifier(owner: PsiModifierListOwner?): Boolean {
        owner ?: return false
        if (owner.getAnnotation("org.springframework.beans.factory.annotation.Qualifier") != null) return true
        val resource = owner.getAnnotation("javax.annotation.Resource") ?: return false
        return resource.attributes.any { it.attributeName == "type" || it.attributeName == "name" || it.attributeName == "mapperName" }
    }

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return object : JavaElementVisitor() {
            override fun visitClass(section: PsiClass?) {
                super.visitClass(section ?: return)
                if (section.hasAnnotation("org.springframework.context.annotation.Primary")) return
                if (section.hasAnnotation("org.springframework.boot.autoconfigure.condition.ConditionalOnProperty")) return
                if (!section.annotations.any { clazzAnno.contains(it.qualifiedName) }) {
                    return
                }

                val interfaces = section.interfaces
                if (interfaces.size != 1) {
                    return
                }
                val face = interfaces.first()
                if (face.qualifiedName?.startsWith("java.") == true) {
                    return
                }
                val scope = GlobalSearchScope.projectScope(holder.project)

                val other = ClassInheritorsSearch.search(face, scope, true)
                    .filter { clazz -> clazz != section && clazz.annotations.any { clazzAnno.contains(it.qualifiedName) } }
                if (other.isEmpty()) {
                    return
                }
                val module = ModuleUtilCore.findModuleForPsiElement(section)
                var allDiff = true
                other.forEach { psiClass ->
                    psiClass.annotations.forEach {
                        if ("org.springframework.context.annotation.Primary" == it.qualifiedName) return@visitClass
                        if ("org.springframework.boot.autoconfigure.condition.ConditionalOnProperty" == it.qualifiedName) return@visitClass
                    }
                    allDiff = allDiff && module != ModuleUtilCore.findModuleForPsiElement(psiClass)
                }
                if (allDiff) return

                val references = ReferencesSearch.search(face, scope).findAll()
                if (references.isEmpty()) return
                val springRef = springRef(references) ?: return
                references.forEach { ref ->
                    val psiField = PsiTreeUtil.getParentOfType(ref.element, PsiField::class.java)
                    if (qualifier(psiField)) return@visitClass
                    val text = psiField?.text
                    if (text != null && (text.contains("<") || text.contains("["))) {
                        return@visitClass
                    }
                    val psiParameter = PsiTreeUtil.getParentOfType(ref.element, PsiParameter::class.java)
                    if (qualifier(psiParameter)) return@visitClass
                }

                val v = section.identifyingElement ?: return
                val otherNames = other.map { it.qualifiedName }.joinToString("\n")
                val suppress = SuppressFix.build(this@PrimaryInspection, v)
                holder.registerProblem(
                    v,
                    I18n.message("inspection.Primary.problem.descriptor", face.qualifiedName ?: "", springRef, otherNames),
                    PrimaryFix.INSTANCE,
                    suppress
                )
            }
        }
    }
}