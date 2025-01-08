package io.github.linwancen.plugin.fix.exception

import com.intellij.codeInspection.AbstractBaseJavaLocalInspectionTool
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.JavaElementVisitor
import com.intellij.psi.PsiCatchSection
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.searches.ReferencesSearch
import io.github.linwancen.plugin.fix.SuppressFix
import io.github.linwancen.plugin.fix.ui.I18n

class PrintStackTraceInspection : AbstractBaseJavaLocalInspectionTool() {

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return object : JavaElementVisitor() {
            override fun visitCatchSection(section: PsiCatchSection?) {
                super.visitCatchSection(section)
                val parameter = section?.parameter ?: return
                val e = parameter.identifyingElement ?: return
                val suppress = SuppressFix.build(this@PrintStackTraceInspection, e)
                ReferencesSearch.search(parameter, GlobalSearchScope.fileScope(holder.file)).forEach {
                    val parent = it.element.parent ?: return@forEach
                    val method = parent.lastChild ?: return@forEach
                    val text = method.text ?: return@forEach
                    if (text == "printStackTrace") {
                        holder.registerProblem(
                            method,
                            I18n.message("inspection.PrintStackTrace.problem.descriptor"),
                            PrintStackTraceLogFix.INSTANCE,
                            PrintStackTraceRemoveFix.INSTANCE,
                            suppress
                        )
                    }
                }
            }
        }
    }
}