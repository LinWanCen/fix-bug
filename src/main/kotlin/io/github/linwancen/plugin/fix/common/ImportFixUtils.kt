package io.github.linwancen.plugin.fix.common

import com.intellij.openapi.editor.Document
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiImportList
import com.intellij.psi.PsiPackageStatement
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.containers.stream
import java.util.stream.Collectors

object ImportFixUtils {
    /**
     * use in end or `PsiDocumentManager.getInstance(psiClass.project)?.commitDocument(document)`
     */
    @JvmStatic
    fun add(document: Document, psiClass: PsiClass, vararg importArr: String) {
        val oldImport = PsiTreeUtil.getChildrenOfType(psiClass.parent, PsiImportList::class.java)
        val oldImportStrSet = oldImport.stream()
            .flatMap { it.importStatements.stream().map { s -> s.qualifiedName } }
            .collect(Collectors.toSet())
        val importSet = importArr.subtract(oldImportStrSet)
        if (importSet.isEmpty()) {
            return
        }
        val importStr = "\n\n" + importSet.joinToString("\n") { "import ${it};" }
        val packageStatement = PsiTreeUtil.getPrevSiblingOfType(psiClass, PsiPackageStatement::class.java)
        val insertIndex = packageStatement?.textRange?.endOffset ?: 0
        document.insertString(insertIndex, importStr)
    }
}