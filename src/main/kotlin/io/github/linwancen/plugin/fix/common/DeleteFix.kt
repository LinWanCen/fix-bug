package io.github.linwancen.plugin.fix.common

import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.openapi.project.Project

class DeleteFix : AbstractFix() {
    companion object {
        val INSTANCE = DeleteFix()
    }

    override fun getFamilyName(): String {
        return """=> delete it."""
    }

    override fun fix(project: Project?, descriptor: ProblemDescriptor?) {
        descriptor?.psiElement?.delete()
    }
}