package io.github.linwancen.plugin.fix.exception

import com.intellij.codeInspection.AbstractBaseJavaLocalInspectionTool
import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.JavaElementVisitor
import com.intellij.psi.PsiCatchSection
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.codeStyle.VariableKind
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.searches.ReferencesSearch
import com.siyeh.ig.fixes.RenameFix
import com.siyeh.ig.psiutils.VariableNameGenerator
import io.github.linwancen.plugin.fix.common.ProblemUtils
import org.slf4j.LoggerFactory

class UnusedExceptionInspection : AbstractBaseJavaLocalInspectionTool() {
    private val LOG = LoggerFactory.getLogger(this::class.java)

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return object : JavaElementVisitor() {
            override fun visitCatchSection(section: PsiCatchSection?) {
                super.visitCatchSection(section)
                try {
                    visit(section)
                } catch (e: Throwable) {
                    LOG.info("UnusedExceptionInspection.visitCatchSection() catch Throwable but log to record.", e)
                }
            }

            private fun visit(section: PsiCatchSection?) {
                val parameter = section?.parameter ?: return
                val ref = ReferencesSearch.search(parameter, GlobalSearchScope.fileScope(holder.file)).findFirst()
                if (ref != null) {
                    return
                }
                val e = parameter.identifyingElement ?: return
                if (e.text.contains("ignore")) return
                val list =
                    mutableListOf<LocalQuickFix>(UnusedExceptionLogFix.INSTANCE, UnusedExceptionThrowFix.INSTANCE)
                section.catchBlock?.let {
                    val newName = VariableNameGenerator(it, VariableKind.LOCAL_VARIABLE)
                        .byName("ignored")
                        .generate(true)
                    val renameFix = RenameFix(newName, false, false)
                    list.add(renameFix)
                }
                ProblemUtils.register(holder, e, this@UnusedExceptionInspection, *list.toTypedArray())
            }
        }
    }
}