package io.github.linwancen.plugin.fix.nullable

import com.intellij.codeInsight.NullableNotNullManager
import com.intellij.codeInspection.AbstractBaseJavaLocalInspectionTool
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.*
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.searches.ReferencesSearch
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.containers.stream
import com.intellij.util.ui.FormBuilder
import io.github.linwancen.plugin.fix.common.ProblemUtils
import io.github.linwancen.plugin.fix.ui.RegexpFieldUtils
import org.jdom.Element
import java.util.regex.Pattern
import javax.swing.JComponent

class MethodReturnNullInspection : AbstractBaseJavaLocalInspectionTool() {
    //language="regexp"
    var callRegexp = "[.](select.*|find.*)"
    private var callPattern = Pattern.compile(callRegexp)
    //language="regexp"
    var typeRegexp = "<|\\[|Optional|^(int|long|short|double|float|boolean|char|byte)$"
    var typePattern = Pattern.compile(typeRegexp)
    //language="regexp"
    var nullRegexp = "[Nn]ull|Blank|Empty"
    var nullPattern = Pattern.compile(nullRegexp)

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return object : JavaElementVisitor() {
            override fun visitLocalVariable(section: PsiLocalVariable?) {
                super.visitLocalVariable(section ?: return)
                val call = PsiTreeUtil.getChildOfType(section, PsiMethodCallExpression::class.java) ?: return
                val methodName = call.text ?: return
                if (!callPattern.matcher(methodName).find()) return

                val type = PsiTreeUtil.findChildOfType(section, PsiTypeElement::class.java) ?: return
                if (typePattern.matcher(type.text).find()) return

                try {
                    val method = call.resolveMethod()
                    if (method != null) {
                        if (method.annotations.stream().anyMatch { it.qualifiedName?.contains("NotNull") == true }) {
                            return
                        }
                        val nullManager = NullableNotNullManager.getInstance(section.project)
                        if (nullManager.isNotNull(method, true)) {
                            return
                        }
                    }
                } catch (_: Exception) {
                }

                val references = ReferencesSearch.search(section, GlobalSearchScope.fileScope(holder.file)).findAll()
                if (references.isEmpty()) return
                val haveNull = references.stream().anyMatch {
                    nullPattern.matcher(it.element.parent.parent.text).find()
                }
                if (haveNull) return
                val v = PsiTreeUtil.nextVisibleLeaf(type) ?: return
                ProblemUtils.register(holder, v, this@MethodReturnNullInspection, MethodReturnNullFix.INSTANCE)
            }
        }
    }

    override fun createOptionsPanel(): JComponent? {
        val callField = RegexpFieldUtils.regexpField(callPattern) { s, p -> callRegexp = s; callPattern = p }
        val typeField = RegexpFieldUtils.regexpField(typePattern) { s, p -> typeRegexp = s; typePattern = p }
        val nullField = RegexpFieldUtils.regexpField(nullPattern) { s, p -> nullRegexp = s; nullPattern = p }
        return FormBuilder.createFormBuilder()
            .addLabeledComponent("call Regexp", callField, 1, true)
            .addLabeledComponent("type Regexp", typeField, 1, true)
            .addLabeledComponent("null Regexp", nullField, 1, true)
            .panel
    }

    override fun readSettings(node: Element) {
        super.readSettings(node)
        try {
            callPattern = Pattern.compile(this.callRegexp)
            typePattern = Pattern.compile(this.typeRegexp)
            nullPattern = Pattern.compile(this.nullRegexp)
        } catch (_: Exception) {
        }
    }
}