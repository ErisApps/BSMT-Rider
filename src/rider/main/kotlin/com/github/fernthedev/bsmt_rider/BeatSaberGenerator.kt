package com.github.fernthedev.bsmt_rider

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.dataformat.xml.XmlMapper
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtil
import com.jetbrains.rd.platform.ui.bedsl.extensions.valueOrEmpty
import com.jetbrains.rd.platform.util.idea.ProtocolSubscribedProjectComponent
import com.jetbrains.rider.model.runnableProjectsModel
import com.jetbrains.rider.projectView.hasSolution
import com.jetbrains.rider.projectView.solution
import com.jetbrains.rider.projectView.solutionPath
import java.io.File


class BeatSaberGenerator(project: Project) : ProtocolSubscribedProjectComponent(project) {
    init {
        generate(project)
    }

    companion object {
        // with Jackson 2.10 and later
        private val mapper = XmlMapper.builder() // possible configuration changes
            .build()

        fun generate(project: Project) {
            if (project.projectFile?.extension == "csproj.user") {
                updateFileContent(getUserCsprojFile(project))
                return
            }

            if (project.hasSolution) {

                val solution = project.solution
                val projectsInSolution = solution.runnableProjectsModel.projects;

                projectsInSolution.valueOrEmpty().forEach { t ->
                    println("Project: ${t.name}");
                }

                // Get the folder of the solution, then get the folder of the actual project
                val folder = getProjectFolder(project)
                val userFile = getUserCsprojFile(project)

                if (folder.exists()) {
                    if (userFile.exists()) {
                        updateFileContent(userFile)
                    } else {
                        val content = generateFileContent(getBeatSaberFolder())
                        userFile.createNewFile()
                        VfsUtil.saveText(VfsUtil.findFileByIoFile(userFile, true)!!, content)
                    }
                }
            }
        }

        // This code is problematic since it assumes that project name, folder and csproj name are all equal. We need a fix
        private fun getProjectFolder(project: Project): File {
            return File(File(project.solutionPath).parentFile, project.name)
        }

        private fun getCsprojFile(project: Project): File {
            return File(getProjectFolder(project), "${project.name}.csproj")
        }

        private fun getUserCsprojFile(project: Project): File {
            return File(getProjectFolder(project), "${project.name}.csproj.user")
        }

        private fun updateFileContent(userCsprojFile: File) {
            val file = VfsUtil.findFileByIoFile(userCsprojFile, true)!!

            val contents = VfsUtil.loadText(file)

            val beatSaberFolder = getBeatSaberFolder();

            // Skip if user.csproj already contains reference
            if (contents.trimIndent().contains(
                    """
                    <BeatSaberDir>$beatSaberFolder</BeatSaberDir>
                    """.trimIndent(),
                    ignoreCase = true)) {
                return
            }

            val xmlData = mapper.readTree(contents) as (ObjectNode)

            val projectNode = xmlData["Project"]
            if (projectNode.isNull)
                xmlData.set<JsonNode>("Project", mapper.createObjectNode())

            val propertyGroupNode = projectNode["PropertyGroup"]
            if (propertyGroupNode.isNull)
                xmlData.set<JsonNode>("PropertyGroup", mapper.createObjectNode())

            val node = mapper.createArrayNode();

            node.add(beatSaberFolder)

            xmlData.set<JsonNode>("BeatSaberDir", node)
        }

        private fun getBeatSaberFolder(): String {
            return "F:\\SteamLibrary\\steamapps\\common\\Beat Saber"
        }

        private fun generateFileContent(beatSaberFolder: String): String {
            return """
                <?xml version="1.0" encoding="utf-8"?>
                <Project>
                  <PropertyGroup>
                		<!-- Change this path if necessary. Make sure it ends with a backslash. -->
                		<BeatSaberDir>$beatSaberFolder</BeatSaberDir>
                  </PropertyGroup>
                </Project>
            """.trimIndent()
        }
    }
}