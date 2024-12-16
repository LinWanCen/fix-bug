package io.github.linwancen.plugin.fix.common

import com.intellij.codeInspection.InspectionProfileEntry
import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.psi.PsiElement
import io.github.linwancen.plugin.fix.SuppressFix
import io.github.linwancen.plugin.fix.ui.I18n

object ProblemUtils {

    @JvmStatic
    fun register(
        holder: ProblemsHolder,
        v: PsiElement,
        inspection: InspectionProfileEntry,
        vararg fixes: LocalQuickFix?,
    ) {
        val name = inspection.shortName
        val tip = I18n.message("inspection.$name.problem.descriptor")
        val suppress = SuppressFix.build(inspection, v)
        val fixesNew = arrayOf(*fixes, suppress)
        val highlightType = ProblemHighlightType.GENERIC_ERROR_OR_WARNING
        val descriptor = holder.manager.createProblemDescriptor(v, tip, holder.isOnTheFly, fixesNew, highlightType)
        descriptor.setTextAttributes(TextAttributesKey.find("FIX_BUG.$name"))
        holder.registerProblem(descriptor)
    }
}