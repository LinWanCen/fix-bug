package io.github.linwancen.plugin.fix.logic

import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.*
import io.github.linwancen.plugin.fix.common.ProblemUtils
import io.github.linwancen.plugin.fix.common.ReplaceFix
import io.github.linwancen.plugin.fix.ui.I18n

object SetGetInspectionRegister {

    @JvmStatic
    fun problem(
        getMap: Map<String, PsiMethodCallExpression>,
        setBean: PsiExpression,
        setField: String,
        holder: ProblemsHolder,
        inspection: SetGetInspection,
    ) {
        for ((getName, get) in getMap) {
            val getBean = get.methodExpression.qualifierExpression ?: continue
            if (getBean.text == setBean.text) {
                continue
            }
            val v = get.methodExpression.referenceNameElement ?: continue
            val method = get.resolveMethod() ?: continue
            problemMethod(method, setField, getName, v, holder, inspection)
        }
    }

    @JvmStatic
    fun problemRefs(
        refsMap: Map<String, PsiMethodReferenceExpression>,
        setBean: PsiExpression,
        setField: String,
        holder: ProblemsHolder,
        inspection: SetGetInspection,
    ) {
        for ((getName, get) in refsMap) {
            val getBean = get.qualifierExpression ?: continue
            if (getBean.text == setBean.text) {
                continue
            }
            val v = get.referenceNameElement ?: continue
            val method = get.resolve() ?: continue
            if (method !is PsiMethod) {
                continue
            }
            problemMethod(method, setField, getName, v, holder, inspection)
        }
    }

    @JvmStatic
    private fun problemMethod(
        method: PsiMethod,
        setField: String,
        getName: String,
        v: PsiElement,
        holder: ProblemsHolder,
        inspection: SetGetInspection,
    ) {
        val psiClass = method.containingClass ?: return
        val newName = "get$setField"
        val methods = psiClass.findMethodsByName(newName, true)
        if (methods.isNotEmpty()) {
            val tip = I18n.message("inspection.SetGet.problem.descriptor.diff", getName, newName)
            ProblemUtils.register(holder, v, arrayOf(tip), inspection, ReplaceFix(newName))
        }
    }
}