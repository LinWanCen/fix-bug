package io.github.linwancen.plugin.fix.maven

import com.intellij.codeInspection.ProblemsHolder
import com.intellij.codeInspection.XmlInspectionSuppressor
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.psi.PsiFile
import com.intellij.psi.XmlElementVisitor
import com.intellij.psi.xml.XmlFile
import com.intellij.psi.xml.XmlTag
import io.github.linwancen.plugin.fix.common.ReplaceFix
import io.github.linwancen.plugin.fix.ui.I18n
import org.jetbrains.idea.maven.dom.MavenDomUtil
import org.jetbrains.idea.maven.dom.MavenPropertyResolver
import org.jetbrains.idea.maven.dom.model.MavenDomProjectModel
import org.slf4j.LoggerFactory

abstract class MavenVersionVisitor(val holder: ProblemsHolder, private val toolId: String) : XmlElementVisitor() {
    private val log = LoggerFactory.getLogger(this::class.java)
    var file: PsiFile? = null
    var rootTag: XmlTag? = null
    var mdm: MavenDomProjectModel? = null

    open fun beforeCheck(file: XmlFile, rootTag: XmlTag): Boolean = true
    open fun parseCurrent(gav: XmlTag, groupId: String, artifactId: String, version: String): String? {
        if (!version.startsWith("$")) {
            return version
        }
        return MavenPropertyResolver.resolve(version, mdm ?: return version)
    }

    abstract fun parseSuggest(gav: XmlTag, groupId: String, artifactId: String, version: String): String?

    override fun visitXmlFile(file: XmlFile?) {
        super.visitXmlFile(file ?: return)
        try {
            if ("pom.xml" != file.name) {
                return
            }
            rootTag = file.rootTag ?: return
            if (!beforeCheck(file, rootTag!!)) {
                return
            }
            mdm = MavenDomUtil.getMavenDomModel(file, MavenDomProjectModel::class.java) ?: return
            rootTag?.findFirstSubTag("parent")?.let { checkGav(it) }
            checkDep(rootTag)
            rootTag?.findFirstSubTag("dependencyManagement")?.let { checkDep(it) }
        } catch (e: ProcessCanceledException) {
            throw e
        } catch (e: Throwable) {
            log.info("${toolId}Inspection fail: {}", file.name, e)
        }
    }

    private fun checkDep(dependenciesParentTag: XmlTag?) {
        dependenciesParentTag?.findFirstSubTag("dependencies")?.let {
            it.findSubTags("dependency").forEach { dependency ->
                checkGav(dependency)
            }
        }
    }

    private fun checkGav(gav: XmlTag) {
        val version = gav.getSubTagText("version") ?: return
        val groupId = gav.getSubTagText("groupId") ?: return
        val artifactId = gav.getSubTagText("artifactId") ?: return
        checkGavImpl(gav, groupId, artifactId, version)
    }

    open fun checkGavImpl(
        gav: XmlTag,
        groupId: String,
        artifactId: String,
        version: String,
    ) {
        val current = parseCurrent(gav, groupId, artifactId, version) ?: return
        val suggest = parseSuggest(gav, groupId, artifactId, current) ?: return
        if (suggest != current) {
            var elementTag: XmlTag? = null
            if (version.startsWith("$")) {
                val key = version.substring(2, version.length - 1)
                rootTag?.findFirstSubTag("properties")?.let { elementTag = it.findFirstSubTag(key) }
            }
            if (elementTag == null) {
                elementTag = gav.findFirstSubTag(gavTagName())
            }
            val elements = elementTag?.value?.children ?: return
            if (elements.isEmpty()) {
                return
            }
            val suppress = XmlInspectionSuppressor().getSuppressActions(elements.first(), toolId)
            holder.registerProblem(
                elements.first(),
                I18n.message("inspection.$toolId.problem.descriptor", suggest),
                ReplaceFix(suggest),
                *suppress,
            )
        }
    }

    open fun gavTagName() = "version"
}