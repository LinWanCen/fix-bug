package io.github.linwancen.plugin.fix.ui

import com.intellij.codeInspection.ui.RegExFormatter
import com.intellij.codeInspection.ui.RegExInputVerifier
import com.intellij.ui.DocumentAdapter
import com.intellij.util.ui.UIUtil
import java.util.function.BiConsumer
import java.util.regex.Pattern
import javax.swing.JFormattedTextField
import javax.swing.event.DocumentEvent

/**
 * Regexp Field
 *
 * @see org.jetbrains.java.generate.inspection.ClassHasNoToStringMethodInspection.createOptionsPanel
 */
object RegexpFieldUtils {

    @JvmStatic
    fun regexpField(initValue: Pattern, consumer: BiConsumer<String, Pattern>): JFormattedTextField {
        val field = JFormattedTextField(RegExFormatter())
        field.value = initValue
        field.inputVerifier = RegExInputVerifier()
        field.focusLostBehavior = 0
        field.minimumSize = field.preferredSize
        UIUtil.fixFormattedField(field)
        val document = field.document
        document.addDocumentListener(object : DocumentAdapter() {
            override fun textChanged(e: DocumentEvent) {
                try {
                    field.commitEdit()
                    val pattern = field.value as Pattern
                    consumer.accept(pattern.pattern(), pattern)
                } catch (_: Exception) {
                }
            }
        })
        return field
    }
}