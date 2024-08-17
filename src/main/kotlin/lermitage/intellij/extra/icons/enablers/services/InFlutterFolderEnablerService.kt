// SPDX-License-Identifier: MIT
package lermitage.intellij.extra.icons.enablers.services

import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project
import lermitage.intellij.extra.icons.enablers.IconEnabler
import java.io.File
import java.io.IOException
import java.nio.file.Files

@Service(Service.Level.PROJECT)
class InFlutterFolderEnablerService : IconEnabler {
    private var isFlutterProject = false

    override fun init(project: Project) {
        val pubspec = File(project.basePath, "pubspec.yaml")
        if (pubspec.exists()) {
            try {
                val pubspecContent = Files.readString(pubspec.toPath())
                isFlutterProject = pubspecContent.contains("sdk: flutter") || pubspecContent.contains("sdk:flutter")
            } catch (e: IOException) {
                LOGGER.warn("Canceled init of Flutter icons Enabler", e)
            }
        }
    }

    override fun verify(project: Project, absolutePathToVerify: String) = isFlutterProject
    override fun terminatesConditionEvaluation() = false

    companion object {
        private val LOGGER = thisLogger()
    }
}
