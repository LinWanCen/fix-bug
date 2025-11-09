package io.github.linwancen.plugin.fix.spring

import com.intellij.codeInspection.AbstractBaseJavaLocalInspectionTool
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.JavaElementVisitor
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.PsiMethod
import com.intellij.util.ui.FormBuilder
import io.github.linwancen.plugin.fix.SuppressFix
import io.github.linwancen.plugin.fix.ui.I18n
import io.github.linwancen.plugin.fix.ui.RegexpFieldUtils
import org.jdom.Element
import java.util.regex.Pattern
import javax.swing.JComponent

class RequestBodyInspection : AbstractBaseJavaLocalInspectionTool() {
    // language="regexp"
    var typeRegexp = "(?:^|<)(?:String|Date|Serializable|MultipartFile" +
            "|Integer|Long|Float|Double|Boolean|Byte|Short|Char" +
            "|int|long|float|double|boolean|byte|short|char)(?:>|\\[])?$" +
            "|^(?:Http)?Servlet(?:Request|Response)$" +
            "|^(?:BindingResult|Principal|Model)$"
    var typePattern = Pattern.compile(typeRegexp)

    var methodAnno = mutableListOf(
        "org.springframework.web.bind.annotation.RequestMapping",
        "org.springframework.web.bind.annotation.PostMapping",
        "org.springframework.web.bind.annotation.GetMapping",
        "org.springframework.web.bind.annotation.PutMapping",
        "org.springframework.web.bind.annotation.DeleteMapping",
        "org.springframework.web.bind.annotation.PatchMapping",
    )

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return object : JavaElementVisitor() {
            override fun visitMethod(section: PsiMethod?) {
                super.visitMethod(section ?: return)
                if (!section.annotations.any { methodAnno.contains(it.qualifiedName) }) return
                val className = section.containingClass?.qualifiedName ?: ""
                for (parameter in section.parameterList.parameters) {
                    if (parameter.annotations.any { it.qualifiedName == "org.springframework.web.bind.annotation.RequestBody" }) return
                    val typeFullName = parameter.typeElement?.text ?: return
                    if (typePattern.matcher(typeFullName).find()) return
                    val suppress = SuppressFix.build(this@RequestBodyInspection, parameter)
                    holder.registerProblem(
                        parameter,
                        I18n.message(
                            "inspection.RequestBody.problem.descriptor",
                            className, section.name, typeFullName
                        ),
                        RequestBodyFix(),
                        suppress
                    )
                }
            }
        }
    }

    override fun createOptionsPanel(): JComponent? {
        val typeField = RegexpFieldUtils.regexpField(typePattern) { s, p -> typeRegexp = s; typePattern = p }
        return FormBuilder.createFormBuilder()
            .addLabeledComponent("ignore type Regexp", typeField, 1, true)
            .panel
    }

    override fun readSettings(node: Element) {
        super.readSettings(node)
        try {
            typePattern = Pattern.compile(this.typeRegexp)
        } catch (_: Exception) {
        }
    }
}