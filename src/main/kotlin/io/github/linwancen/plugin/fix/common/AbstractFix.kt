package io.github.linwancen.plugin.fix.common

import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.openapi.editor.Document
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.siyeh.ig.InspectionGadgetsFix
import org.slf4j.LoggerFactory

abstract class AbstractFix : InspectionGadgetsFix() {
    companion object {
        private val LOG = LoggerFactory.getLogger(this::class.java)
    }

    override fun doFix(project: Project?, descriptor: ProblemDescriptor?) {
        try {
            fix(project, descriptor)
        } catch (e: Throwable) {
            LOG.info("{}.fix() catch Throwable but log to record.", this::class.java.simpleName, e)
        }
    }

    open fun fix(project: Project?, descriptor: ProblemDescriptor?) {
        val element = descriptor?.psiElement ?: return
        val viewProvider = element.containingFile?.viewProvider ?: return
        val document = viewProvider.document ?: return
        documentFix(element, document)
    }

    open fun documentFix(element: PsiElement, document: Document) {}

}