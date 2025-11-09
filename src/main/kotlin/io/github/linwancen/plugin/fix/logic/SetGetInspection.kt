package io.github.linwancen.plugin.fix.logic

import com.intellij.codeInspection.AbstractBaseJavaLocalInspectionTool
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.*
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.ui.components.CheckBox
import com.intellij.util.ui.FormBuilder
import io.github.linwancen.plugin.fix.common.DeleteFix
import io.github.linwancen.plugin.fix.common.ProblemUtils
import io.github.linwancen.plugin.fix.common.ReplaceFix
import io.github.linwancen.plugin.fix.ui.I18n
import io.github.linwancen.plugin.fix.ui.RegexpFieldUtils
import org.jdom.Element
import java.util.regex.Pattern
import javax.swing.JComponent

class SetGetInspection : AbstractBaseJavaLocalInspectionTool() {
    val came = Regex("([a-z])([A-Z])")

    // language="regexp"
    var setExcludeRegexp = "Text|String"
    private var setExcludePattern = Pattern.compile(setExcludeRegexp)

    // language="regexp"
    var getExcludeRegexp = "Text|Instance"
    private var getExcludePattern = Pattern.compile(getExcludeRegexp)

    // language="regexp"
    var notLettersRegexp = "\\W"
    private var notLettersPattern = Pattern.compile(notLettersRegexp)
    var keyMatch = false
    var checkRef = false

    override fun createOptionsPanel(): JComponent? {
        val setField =
            RegexpFieldUtils.regexpField(setExcludePattern) { s, p -> setExcludeRegexp = s; setExcludePattern = p }
        val getField =
            RegexpFieldUtils.regexpField(getExcludePattern) { s, p -> getExcludeRegexp = s; getExcludePattern = p }
        val notLettersField =
            RegexpFieldUtils.regexpField(notLettersPattern) { s, p -> notLettersRegexp = s; notLettersPattern = p }
        val stringMatchBox = CheckBox("string key match", keyMatch)
        val checkRefBox = CheckBox("check method reference", checkRef)
        stringMatchBox.addActionListener { keyMatch = stringMatchBox.isSelected }
        checkRefBox.addActionListener { checkRef = checkRefBox.isSelected }
        return FormBuilder.createFormBuilder()
            .addLabeledComponent("set exclude regexp", setField, 1, true)
            .addLabeledComponent("get exclude regexp", getField, 1, true)
            .addLabeledComponent("not letters regexp", notLettersField, 1, true)
            .addComponent(stringMatchBox)
            .addComponent(checkRefBox)
            .panel
    }

    override fun readSettings(node: Element) {
        super.readSettings(node)
        try {
            setExcludePattern = Pattern.compile(this.setExcludeRegexp)
            notLettersPattern = Pattern.compile(this.notLettersRegexp)
        } catch (_: Exception) {
        }
    }

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return object : JavaElementVisitor() {
            override fun visitMethodCallExpression(section: PsiMethodCallExpression?) {
                super.visitMethodCallExpression(section ?: return)
                val setName = section.methodExpression.referenceName ?: return
                if (!setName.startsWith("set")) {
                    return
                }
                val setField = setName.substring(3)
                if (setExcludePattern.matcher(setField).find()) {
                    return
                }
                val setBean = section.methodExpression.qualifierExpression ?: return
                val setEnd = section.methodExpression.referenceNameElement?.textRange?.endOffset ?: return
                val getMap = buildGetMap(section, setEnd, setField, setBean) ?: return
                if (checkRef) {
                    val getRefsMap = buildGetRefMap(section, setEnd, setField) ?: return
                    SetGetInspectionRegister.problemRefs(getRefsMap, setBean, setField, holder, this@SetGetInspection)
                }
                SetGetInspectionRegister.problem(getMap, setBean, setField, holder, this@SetGetInspection)
            }

            private fun buildGetMap(
                section: PsiMethodCallExpression,
                setEnd: Int,
                setField: String,
                setBean: PsiExpression,
            ): Map<String, PsiMethodCallExpression>? {
                val getCalls = PsiTreeUtil.findChildrenOfType(section, PsiMethodCallExpression::class.java)
                val getMap = mutableMapOf<String, PsiMethodCallExpression>()
                for (call in getCalls) {
                    val getBean = call.methodExpression.qualifierExpression ?: continue
                    if (getBean.textRange.startOffset < setEnd) {
                        continue
                    }
                    val getName = call.methodExpression.referenceName ?: continue
                    val getField = substringField(getName) ?: continue
                    if (getExcludePattern.matcher(getField).find()) {
                        continue
                    }
                    if (paramMatch(call, setField)) continue
                    getMap[getName] = call
                    if (getField == setField) {
                        if (section == call.parent.parent) {
                            if (setBean.text == getBean.text) {
                                val tip = arrayOf(I18n.message("inspection.SetGet.problem.descriptor.same", section))
                                ProblemUtils.register(holder, section, tip, this@SetGetInspection, DeleteFix())
                            }
                        }
                        return null
                    }
                    if (startOrEndWith(getField, setField)) return null
                }
                return getMap
            }

            private fun paramMatch(call: PsiMethodCallExpression, setField: String): Boolean {
                val children = call.children
                if (children.size != 2) return false
                val param = children[1].children
                if (param.size < 3) return false
                // get("keyCode")
                if (keyMatch && param.size == 3 && param[1] is PsiLiteralExpression) {
                    val key = param[1].text
                    if (key.startsWith("\"") && key.endsWith("\"")) {
                        val keyCode = notLettersPattern.matcher(key).replaceAll("")
                        if (startOrEndWith(keyCode, setField)) return true
                        val setUnderLine = came.replace(setField, "$1_$2").toLowerCase()
                        val s = '"' + setUnderLine + '"'
                        val tip = arrayOf(I18n.message("inspection.SetGet.problem.descriptor.diff", key, s))
                        ProblemUtils.register(holder, param[1], tip, this@SetGetInspection, ReplaceFix(s))
                    }
                }
                // getXXX(have something)
                return true
            }

            private fun buildGetRefMap(
                section: PsiMethodCallExpression?,
                setEnd: Int,
                setField: String,
            ): Map<String, PsiMethodReferenceExpression>? {
                val getRefs = PsiTreeUtil.findChildrenOfType(section, PsiMethodReferenceExpression::class.java)
                val getRefsMap = mutableMapOf<String, PsiMethodReferenceExpression>()
                for (call in getRefs) {
                    val getBean = call.qualifierExpression ?: continue
                    if (getBean.textRange.startOffset < setEnd) {
                        continue
                    }
                    val getName = call.referenceName ?: continue
                    val getField = substringField(getName) ?: continue
                    if (getExcludePattern.matcher(getField).find()) {
                        continue
                    }
                    getRefsMap[getName] = call
                    if (getField == setField) {
                        return null
                    }
                    if (startOrEndWith(getField, setField)) return null
                }
                return getRefsMap
            }

            private fun substringField(getName: String): String? {
                return if (getName.startsWith("get")) {
                    getName.substring(3)
                } else if (getName.startsWith("is")) {
                    getName.substring(2)
                } else {
                    null
                }
            }

            private fun startOrEndWith(getField: String, setField: String): Boolean {
                if (getField.startsWith(setField, true) ||
                    setField.startsWith(getField, true) ||
                    getField.endsWith(setField, true) ||
                    setField.endsWith(getField, true)
                ) return true
                return false
            }
        }
    }
}