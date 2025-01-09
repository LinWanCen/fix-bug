package io.github.linwancen.plugin.fix.spring

import com.intellij.codeInspection.AbstractBaseJavaLocalInspectionTool
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.codeInspection.util.SpecialAnnotationsUtil
import com.intellij.psi.*
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.searches.ClassInheritorsSearch
import com.intellij.psi.search.searches.ReferencesSearch
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.ui.FormBuilder
import io.github.linwancen.plugin.fix.SuppressFix
import io.github.linwancen.plugin.fix.ui.I18n
import javax.swing.JComponent

class NotAnnoInspection : AbstractBaseJavaLocalInspectionTool() {
    var clazzAnno = mutableListOf(
        "org.springframework.stereotype.Component",
        "org.springframework.stereotype.Repository",
        "org.springframework.stereotype.Service",
        "org.springframework.stereotype.Controller",
        "org.springframework.web.bind.annotation.RestController",
        "org.springframework.context.annotation.Bean",
        "org.springframework.context.annotation.Configuration",
        "org.springframework.boot.context.properties.ConfigurationProperties",
        "org.apache.dubbo.config.annotation.DubboService",
    )
    private var memberAnno = mutableListOf(
        "javax.annotation.Resource",
        "org.springframework.beans.factory.annotation.Autowired",
    )

    private fun spring(owner: PsiModifierListOwner) = owner.annotations.any { memberAnno.contains(it.qualifiedName) }

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return object : JavaElementVisitor() {
            override fun visitClass(section: PsiClass?) {
                super.visitClass(section ?: return)
                if (section.isInterface || section.isEnum || section.isRecord || section.isAnnotationType) {
                    return
                }
                if (section.hasModifierProperty(PsiModifier.ABSTRACT)) {
                    return
                }
                if (section.annotations.any { clazzAnno.contains(it.qualifiedName) }) {
                    return
                }
                val interfaces = section.interfaces
                val scope = GlobalSearchScope.projectScope(holder.project)
                val references = if (interfaces.size == 1) {
                    val face = interfaces.first()
                    if (face.qualifiedName?.startsWith("java.") == true) {
                        return
                    }
                    val haveOther = ClassInheritorsSearch.search(face, scope, true)
                        .anyMatch { clazz -> clazz.annotations.any { clazzAnno.contains(it.qualifiedName) } }
                    if (haveOther) {
                        return
                    }
                    ReferencesSearch.search(face, scope).findAll()
                } else {
                    ReferencesSearch.search(section, scope).findAll()
                }
                if (references.isEmpty()) return
                val springRef = springRef(references) ?: return
                val v = section.identifyingElement ?: return
                val suppress = SuppressFix.build(this@NotAnnoInspection, v)
                holder.registerProblem(
                    v,
                    I18n.message("inspection.NotAnno.problem.descriptor") + springRef,
                    NotAnnoFix.INSTANCE,
                    suppress
                )
            }
        }
    }

    fun springRef(references: Collection<PsiReference>): String? {
        references.forEach { ref ->
            val psiMethod = PsiTreeUtil.getParentOfType(ref.element, PsiMethod::class.java)
            val bean = psiMethod?.annotations?.any { "org.springframework.context.annotation.Bean" == it.qualifiedName }
            if (bean == true) {
                return null
            }
            // not search PsiClass for pref
            val psiClass = PsiTreeUtil.getParentOfType(ref.element, PsiClass::class.java)
            val conf = psiClass?.annotations?.any { "org.springframework.boot.context.properties.ConfigurationProperties" == it.qualifiedName }
            if (conf == true) {
                return null
            }
        }
        references.forEach { ref ->
            val refElement = ref.element
            PsiTreeUtil.getParentOfType(refElement, PsiField::class.java)?.let { psiField ->
                if (psiField.hasInitializer()) return@forEach
                val name = "${psiField.containingClass?.qualifiedName ?: ""}.${psiField.name}"
                if (spring(psiField)) return@springRef name
                if (psiField.modifierList?.hasModifierProperty(PsiModifier.FINAL) == true) {
                    PsiTreeUtil.getParentOfType(refElement, PsiClass::class.java)?.let { clazz ->
                        if (clazz.annotations.any { clazzAnno.contains(it.qualifiedName) }
                            && clazz.annotations.any { "lombok.RequiredArgsConstructor" == it.qualifiedName }) return@springRef name
                    }
                }
                return@forEach
            }
            PsiTreeUtil.getParentOfType(refElement, PsiParameter::class.java)?.let { psiParameter ->
                PsiTreeUtil.getParentOfType(refElement, PsiMethod::class.java)?.let { psiMethod ->
                    val name = "${psiMethod.containingClass?.qualifiedName ?: ""}.${psiMethod.name}(${psiParameter.name}"
                    if (spring(psiParameter)) return@springRef name
                    if (psiMethod.isConstructor) {
                        PsiTreeUtil.getParentOfType(refElement, PsiClass::class.java)?.let { clazz ->
                            if (clazz.annotations.any { clazzAnno.contains(it.qualifiedName) }) return@springRef name
                        }
                        return@forEach
                    }
                    if (spring(psiMethod)) return@springRef name
                }
            }
        }
        return null
    }

    override fun createOptionsPanel(): JComponent? {
        val anno1Field = SpecialAnnotationsUtil.createSpecialAnnotationsListControl(this.clazzAnno, "@ClassAnno", true)
        val anno2Field = SpecialAnnotationsUtil.createSpecialAnnotationsListControl(this.memberAnno, "@InnerAnno", true)
        return FormBuilder.createFormBuilder()
            .addComponent(anno1Field)
            .addComponent(anno2Field)
            .panel
    }
}