package io.github.linwancen.plugin.fix.common

import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiVariable

object VarUtils {
    @JvmStatic
    fun varByName(psiElement: PsiElement, vararg names: String): PsiVariable? {
        val helper = JavaPsiFacade.getInstance(psiElement.project)?.resolveHelper ?: return null
        for (name in names) {
            val logVal = helper.resolveReferencedVariable(name, psiElement)
            if (logVal != null) return logVal
        }
        return null
    }
}