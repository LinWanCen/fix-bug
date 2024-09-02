package io.github.linwancen.plugin.fix.exception

import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiExpression
import com.siyeh.ig.PsiReplacementUtil
import com.siyeh.ig.psiutils.CommentTracker

open class PrintStackTraceLogFix : UnusedExceptionLogFix() {
    companion object {
        val INSTANCE = PrintStackTraceLogFix()
    }

    override fun getFamilyName(): String {
        return """=> LOG.error("", e);"""
    }

    override fun fix(project: Project?, descriptor: ProblemDescriptor?) {
        val printStackTrace = descriptor?.psiElement ?: return
        val exp = printStackTrace.parent ?: return
        val replaceExp = exp.parent ?: return
        if (replaceExp !is PsiExpression) return
        val e = exp.firstChild.reference?.resolve()?.lastChild ?: return
        val (document, catchBlock, msg) = msgAndCatchBlock(e) ?: return
        val logValName = logValName(document, catchBlock)
        val notLog = logValName == null
        val newExp = """${logValName ?: "LOG"}.error("$msg", e)"""
        val commentTracker = CommentTracker()
        PsiReplacementUtil.replaceExpressionAndShorten(replaceExp, newExp, commentTracker)
        PsiDocumentManager.getInstance(project ?: return).doPostponedOperationsAndUnblockDocument(document)
        if (notLog) importLog(catchBlock, document)
    }
}