package io.github.linwancen.plugin.fix.spring

import com.intellij.codeInspection.AbstractBaseJavaLocalInspectionTool
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.codeInspection.util.SpecialAnnotationsUtil
import com.intellij.psi.JavaElementVisitor
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiElementVisitor
import io.github.linwancen.plugin.fix.SuppressFix
import io.github.linwancen.plugin.fix.common.DeleteFix
import io.github.linwancen.plugin.fix.ui.I18n
import javax.swing.JComponent

class AnnoStaticInspection : AbstractBaseJavaLocalInspectionTool() {
    var annotations = mutableListOf(
        "javax.annotation.Resource",
        "javax.annotation.PreDestroy",
        "javax.annotation.PostConstruct",
        "org.springframework.context.annotation.Bean",
        "org.springframework.beans.factory.annotation.Value",
        "org.springframework.beans.factory.annotation.Autowired",
        "org.springframework.transaction.annotation.Transactional",
    )

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return object : JavaElementVisitor() {
            override fun visitAnnotation(section: PsiAnnotation?) {
                super.visitAnnotation(section)
                val name = section?.qualifiedName ?: return
                if (!annotations.contains(name)) return
                val v = section.parent.children.reversed().find { it.text.equals("static") } ?: return
                val suppress = SuppressFix.build(this@AnnoStaticInspection, v)
                holder.registerProblem(
                    v,
                    name + I18n.message("inspection.AnnoStatic.problem.descriptor"),
                    DeleteFix.INSTANCE,
                    suppress
                )
            }
        }
    }

    override fun createOptionsPanel(): JComponent? {
        // PublicFieldInspection#createOptionsPanel
        return SpecialAnnotationsUtil.createSpecialAnnotationsListControl(this.annotations,"@Anno not static", true)
    }
}