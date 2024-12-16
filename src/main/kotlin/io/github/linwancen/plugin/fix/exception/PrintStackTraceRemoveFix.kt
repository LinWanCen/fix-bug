package io.github.linwancen.plugin.fix.exception

import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.openapi.project.Project
import io.github.linwancen.plugin.fix.common.AbstractFix

open class PrintStackTraceRemoveFix : AbstractFix() {
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