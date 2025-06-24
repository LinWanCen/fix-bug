package io.github.linwancen.plugin.fix.maven

import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.search.FilenameIndex
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.xml.XmlFile
import com.intellij.psi.xml.XmlTag
import org.jetbrains.idea.maven.dom.MavenDomUtil
import org.jetbrains.idea.maven.dom.MavenPropertyResolver
import org.jetbrains.idea.maven.dom.model.MavenDomProjectModel
import java.util.concurrent.ConcurrentHashMap

open class MavenVersionDiffInspection : LocalInspectionTool() {
    private val projectMap = ConcurrentHashMap<Project, ConcurrentHashMap<String, String>>()

    private fun versionMap(project: Project): MutableMap<String, String> {
        if (projectMap[project] == null) {
            synchronized(projectMap) {
                if (projectMap[project] == null) {
                    val map = ConcurrentHashMap<String, String>()
                    val scope = GlobalSearchScope.projectScope(project)
                    val pomFiles = FilenameIndex.getFilesByName(project, "pom.xml", scope)
                    for (pomFile in pomFiles) {
                        if (pomFile is XmlFile) {
                            updateVersion(map, pomFile.rootTag ?: continue)
                        }
                    }
                    projectMap[project] = map
                    return map
                }
            }
        }
        return projectMap[project]!!
    }

    private fun updateVersion(versionMap: MutableMap<String, String>, rootTag: XmlTag) {
        val parent = rootTag.findFirstSubTag("parent")
        val artifactId = rootTag.getSubTagText("artifactId") ?: return
        val groupId = rootTag.getSubTagText("groupId") ?: parent?.getSubTagText("groupId") ?: return
        var version = rootTag.getSubTagText("version") ?: parent?.getSubTagText("version") ?: return
        if (version.startsWith("$")) {
            MavenDomUtil.getMavenDomModel(rootTag.containingFile, MavenDomProjectModel::class.java)?.let {
                version = MavenPropertyResolver.resolve(version, it)
            }
        }
        // not support project which same group and version and diff version
        versionMap["$groupId:$artifactId"] = version
    }

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return object : MavenVersionVisitor(holder, "MavenVersionDiff") {
            var versionMap: MutableMap<String, String>? = null

            override fun beforeCheck(file: XmlFile, rootTag: XmlTag): Boolean {
                versionMap = versionMap(file.project)
                updateVersion(versionMap ?: return false, rootTag)
                return true
            }

            override fun parseSuggest(gav: XmlTag, groupId: String, artifactId: String, version: String): String? {
                return versionMap?.get("$groupId:$artifactId")
            }
        }
    }
}