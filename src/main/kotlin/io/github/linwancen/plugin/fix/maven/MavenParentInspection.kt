package io.github.linwancen.plugin.fix.maven

import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.codeInspection.XmlInspectionSuppressor
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.XmlElementVisitor
import com.intellij.psi.xml.XmlFile
import com.intellij.psi.xml.XmlTag
import io.github.linwancen.plugin.fix.ui.I18n
import org.slf4j.LoggerFactory

open class MavenParentInspection : LocalInspectionTool() {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return object : XmlElementVisitor() {
            private val LOG = LoggerFactory.getLogger(this::class.java)

            override fun visitXmlFile(file: XmlFile?) {
                super.visitXmlFile(file ?: return)
                try {
                    visit(file)
                } catch (e: Throwable) {
                    LOG.info("MavenParentInspection fail: {}", file.name, e)
                }
            }

            private fun visit(file: XmlFile) {
                if ("pom.xml" != file.name) {
                    return
                }
                val rootTag = file.rootTag ?: return
                val parent = rootTag.findFirstSubTag("parent") ?: return
                if (parent.text.contains("relativePath")) {
                    return
                }
                val artifactId = parent.getSubTagText("artifactId") ?: return
                val groupId = parent.getSubTagText("groupId") ?: parent.getSubTagText("groupId") ?: return

                val parentDir = file.parent?.parent ?: return
                val parentPomFile = parentDir.findFile("pom.xml")
                if (parentPomFile !is XmlFile) {
                    // null should add, see spring-cloud-dependencies
                    register(parent)
                    return
                }
                val parentRootTag = parentPomFile.rootTag ?: return

                val parentParent = parentRootTag.findFirstSubTag("parent")
                val parentArtifactId = parentRootTag.getSubTagText("artifactId") ?: return
                val parentGroupId =
                    parentRootTag.getSubTagText("groupId") ?: parentParent?.getSubTagText("groupId") ?: return
                if ("$groupId:$artifactId" != "$parentGroupId:$parentArtifactId") {
                    register(parent)
                }
            }

            private fun register(parent: XmlTag) {
                val suppress = XmlInspectionSuppressor().getSuppressActions(parent, "MavenParent")
                holder.registerProblem(
                    parent,
                    I18n.message("inspection.MavenParent.problem.descriptor"),
                    MavenParentFix.INSTANCE,
                    *suppress,
                )
            }
        }
    }
}