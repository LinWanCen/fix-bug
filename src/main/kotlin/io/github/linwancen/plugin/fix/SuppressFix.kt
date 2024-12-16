package io.github.linwancen.plugin.fix

import com.intellij.codeInspection.AbstractBaseJavaLocalInspectionTool
import com.intellij.codeInspection.InspectionProfileEntry
import com.intellij.psi.PsiElement
import com.siyeh.ig.fixes.SuppressForTestsScopeFix

object SuppressFix {
    fun build(inspection: InspectionProfileEntry?, context: PsiElement?): SuppressForTestsScopeFix? {
        try {
            val method = SuppressForTestsScopeFix::class.java
                .getMethod("build", InspectionProfileEntry::class.java, PsiElement::class.java)
            return method.invoke(null, inspection, context) as SuppressForTestsScopeFix?
        } catch (e: Exception) {
            val method = SuppressForTestsScopeFix::class.java
                .getMethod("build", AbstractBaseJavaLocalInspectionTool::class.java, PsiElement::class.java)
            return method.invoke(null, inspection, context) as SuppressForTestsScopeFix?
        }
    }
}