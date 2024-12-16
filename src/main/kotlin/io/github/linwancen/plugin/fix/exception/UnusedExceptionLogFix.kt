package io.github.linwancen.plugin.fix.exception

import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.openapi.editor.Document
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMethod
import com.intellij.psi.util.PsiTreeUtil
import io.github.linwancen.plugin.fix.common.AbstractFix
import io.github.linwancen.plugin.fix.common.ImportFixUtils
import io.github.linwancen.plugin.fix.common.VarUtils

open class UnusedExceptionLogFix : AbstractFix() {
    companion object {
        val INSTANCE = UnusedExceptionLogFix()
    }

    override fun getFamilyName(): String {
        return """=> LOG.error("", e);"""
    }

    override fun fix(project: Project?, descriptor: ProblemDescriptor?) {
        val e = descriptor?.psiElement ?: return
        val (document, catchBlock, msg) = msgAndCatchBlock(e) ?: return

        val rBrace = catchBlock.lastChild ?: return
        var startSpace = rBrace.prevSibling?.text
        if (startSpace == null || startSpace.contains('{')) {
            startSpace = "\n        "
        }
        val addSpace = if (startSpace.endsWith('\t')) "\t" else "    "

        insertIntoCatch(catchBlock, document, startSpace, addSpace, msg, e)
    }

    /**
     * @param e ref should `e.reference?.resolve()?.lastChild`
     */
    open fun msgAndCatchBlock(e: PsiElement): Triple<Document, PsiElement, String>? {
        val viewProvider = e.containingFile?.viewProvider ?: return null
        val document = viewProvider.document ?: return null
        val eParam = e.parent ?: return null
        val catchSection = eParam.parent ?: return null
        val catchBlock = catchSection.lastChild ?: return null
        val method = PsiTreeUtil.getParentOfType(catchSection, PsiMethod::class.java)
        val msg = if (method == null) "fail: " else "${method.name} fail: "
        return Triple(document, catchBlock, msg)
    }

    open fun insertIntoCatch(
        catchBlock: PsiElement, document: Document,
        startSpace: String, addSpace: String, msg: String, e: PsiElement,
    ) {
        val logValName = logValName(document, catchBlock)
        val notLog = logValName == null
        val lBrace = catchBlock.firstChild ?: return
        val insertIndex = (lBrace.textRange?.startOffset ?: return) + 1
        document.insertString(insertIndex, """${startSpace}${addSpace}${logValName ?: "LOG"}.error("$msg", e);""")
        if (notLog) importLog(catchBlock, document)
    }

    open fun logValName(document: Document, catchBlock: PsiElement): String? {
        return VarUtils.varByName(catchBlock, "log", "LOG", "LOGGER", "logger")?.name
    }

    open fun importLog(catchBlock: PsiElement, document: Document) {
        val psiClass = PsiTreeUtil.getParentOfType(catchBlock, PsiClass::class.java) ?: return
        val lBrace = psiClass.lBrace ?: return
        val space = lBrace.nextSibling?.text ?: "\n    "
        val insertIndex = (lBrace.textRange?.startOffset ?: return) + 1
        document.insertString(
            insertIndex,
            "${space}private static final Logger LOG = LoggerFactory.getLogger(${psiClass.name}.class);\n"
        )
        ImportFixUtils.add(document, psiClass, "org.slf4j.Logger", "org.slf4j.LoggerFactory")
    }
}