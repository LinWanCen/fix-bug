package io.github.linwancen.plugin.fix.spring

import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.*
import com.intellij.psi.impl.source.PsiClassReferenceType
import io.github.linwancen.plugin.fix.common.ProblemUtils

class UseImplInspection : NotAnnoInspection() {

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return object : JavaElementVisitor() {
            override fun visitClass(section: PsiClass?) {
                super.visitClass(section ?: return)
                if (canNotSpringClass(section)) return
                if (!section.annotations.any { clazzAnno.contains(it.qualifiedName) }) {
                    return
                }
                section.allFields.forEach { if (springMember(it)) checkType(it.typeElement, holder, section) }
                section.constructors.forEach { psiMethod ->
                    psiMethod.parameterList.parameters.forEach { param ->
                        checkType(
                            param.typeElement,
                            holder,
                            section
                        )
                    }
                }
                val lombok = section.annotations.any { "lombok.RequiredArgsConstructor" == it.qualifiedName }
                if (lombok) {
                    section.allFields.forEach {
                        if (it.modifierList?.hasModifierProperty(PsiModifier.FINAL) == true) {
                            checkType(it.typeElement, holder, section)
                        }
                    }
                }
            }
        }
    }

    private fun checkType(typeElement: PsiTypeElement?, holder: ProblemsHolder, section: PsiClass) {
        val type = typeElement?.type ?: return
        if (type !is PsiClassReferenceType) return
        val psiClass = try {
            type.resolve() ?: return
        } catch (_: Throwable) {
            return
        }
        if (psiClass.isInterface) return
        val interfaces = psiClass.interfaces
        if (interfaces.isEmpty()) return
        val arr1 = section.qualifiedName?.split(".") ?: return
        if (arr1.size < 2) return
        val sameCompany = interfaces.any {
            val arr2 = it.qualifiedName?.split(".")
            if (arr2 == null || arr2.size < 2) return@any false
            return@any arr2[1] == arr1[1]
        }
        if (sameCompany) {
            ProblemUtils.register(
                holder,
                typeElement,
                arrayOf(interfaces.joinToString { "," }),
                this@UseImplInspection,
                UseImplFix.INSTANCE
            )
        }
    }
}