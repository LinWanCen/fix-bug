package io.github.linwancen.plugin.fix.exception

import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.openapi.project.Project

open class PrintStackTraceRemoveFix : UnusedExceptionLogFix() {
    companion object {
        val INSTANCE = PrintStackTraceRemoveFix()
    }

    override fun getFamilyName(): String {
        return """=> delete it."""
    }

    override fun fix(project: Project?, descriptor: ProblemDescriptor?) {
        val printStackTrace = descriptor?.psiElement ?: return
        val exp = printStackTrace.parent ?: return
        val replaceExp = exp.parent ?: return
        replaceExp.parent?.delete()
    }
}