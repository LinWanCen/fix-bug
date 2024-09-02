package io.github.linwancen.plugin.fix.exception

import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.openapi.editor.Document
import com.intellij.openapi.project.Project
import com.intellij.psi.*
import com.intellij.psi.util.PsiTreeUtil
import com.siyeh.ig.InspectionGadgetsFix
import org.slf4j.LoggerFactory

open class UnusedExceptionLogFix : InspectionGadgetsFix() {
    companion object {
        private val LOG = LoggerFactory.getLogger(this::class.java)
        val INSTANCE = UnusedExceptionLogFix()
    }

    override fun getFamilyName(): String {
        return """=> LOG.error("", e);"""
    }

    override fun doFix(project: Project?, descriptor: ProblemDescriptor?) {
        try {
            fix(project, descriptor)
        } catch (e: Throwable) {
            LOG.info("{}.fix() catch Throwable but log to record.", this::class.java.simpleName, e)
        }
    }

    open fun fix(project: Project?, descriptor: ProblemDescriptor?) {
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
        return valByName(catchBlock, "log", "LOG", "LOGGER", "logger")?.name
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
        val importIndex = (psiClass.prevSibling?.textRange?.startOffset ?: return) + 1
        document.insertString(importIndex, "import org.slf4j.Logger;\nimport org.slf4j.LoggerFactory;\n")
    }

    open fun valByName(catchBlock: PsiElement, vararg names: String): PsiVariable? {
        val helper = JavaPsiFacade.getInstance(catchBlock.project)?.resolveHelper ?: return null
        for (name in names) {
            val logVal = helper.resolveReferencedVariable(name, catchBlock)
            if (logVal != null) return logVal
        }
        return null
    }
}