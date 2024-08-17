// SPDX-License-Identifier: MIT
package lermitage.intellij.extra.icons.enablers.services

import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import lermitage.intellij.extra.icons.enablers.IconEnabler
import java.io.File
import java.io.FileNotFoundException
import java.util.Locale
import java.util.regex.Pattern
import kotlin.time.Duration.Companion.seconds
import kotlin.time.DurationUnit
import kotlin.time.measureTime

@Suppress("HardCodedStringLiteral")
@Service(Service.Level.PROJECT)
class GitSubmoduleFolderEnablerService : IconEnabler {
    private var submoduleFolders = emptySet<String>()

    @Synchronized
    override fun init(project: Project) {
        val duration = measureTime {
            try {
                submoduleFolders = findAllGitModulesFilesRecursively(project)
            } catch (e: Exception) {
                LOGGER.warn("Failed to init Git submodule Enabler", e)
            }
        }

        val logMsg = "Searched for git submodules in project ${project.name} in ${duration.toString(DurationUnit.MILLISECONDS)} ms. " +
                "Found git submodule folders: $submoduleFolders"
        if (duration > 4.seconds)
            LOGGER.warn("$logMsg. Operation should complete faster")
        else
            LOGGER.info(logMsg)
    }

    /**
     * Find .gitmodules at root, then find every nested .gitmodules for every
     * module (don't have to explore the whole project files).
     */
    private fun findAllGitModulesFilesRecursively(project: Project): Set<String> {
        val submoduleFoldersFound = mutableSetOf<String>()
        val projectVirtualDir = project.guessProjectDir() ?: return submoduleFoldersFound
        try {
            submoduleFoldersFound += findGitModulesFilesInFolder(projectVirtualDir.path)
                .map { obj: String -> obj.lowercase(Locale.getDefault()) }
                .toSet()
            submoduleFoldersFound += findNestedGitModulesFilesRecursively(submoduleFoldersFound)
        } catch (e: FileNotFoundException) {
            LOGGER.warn("Error while looking for git submodules", e)
        }
        return submoduleFoldersFound
    }

    private fun findNestedGitModulesFilesRecursively(parentModules: Set<String>): Set<String> {
        val nestedModules = mutableSetOf<String>()
        for (parentModule in parentModules) {
            try {
                val submoduleFoldersFound = findGitModulesFilesInFolder(parentModule)
                    .map { it.lowercase(Locale.getDefault()) }
                    .toSet()
                if (submoduleFoldersFound.isNotEmpty()) {
                    nestedModules += submoduleFoldersFound
                    nestedModules += findNestedGitModulesFilesRecursively(submoduleFoldersFound)
                }
            } catch (e: FileNotFoundException) {
                LOGGER.warn("Error while looking for nested git submodules (parent git module: '$parentModule')", e)
            } catch (e: StackOverflowError) {
                LOGGER.warn(
                    "Error while looking for nested git submodules (parent git module: '$parentModule'), the git submodules tree is too deep",
                    e
                )
            }
        }
        return nestedModules
    }

    /**
     * Find Git submodules in given folder.
     *
     * @param folderPath folder's path.
     * @return submodule paths relative to folderPath.
     * @throws FileNotFoundException if folderPath doesn't exist.
     */
    @Throws(FileNotFoundException::class)
    private fun findGitModulesFilesInFolder(folderPath: String): Sequence<String> {
        val rootGitModules = File(folderPath, GIT_MODULES_FILENAME)
        if (!rootGitModules.exists())
            return emptySequence()

        return rootGitModules.useLines { lines ->
            lines.map { line ->
                val matcher = GIT_MODULES_PATH_PATTERN.matcher(line)
                if (matcher.find()) "$folderPath/${matcher.group(1)}" else null
            }.filterNotNull()
        }
    }

    override fun verify(project: Project, absolutePathToVerify: String): Boolean {
        return submoduleFolders.contains(absolutePathToVerify.lowercase(Locale.getDefault()))
    }

    override fun terminatesConditionEvaluation() = true

    companion object {
        private val LOGGER = thisLogger()
        const val GIT_MODULES_FILENAME: String = ".gitmodules"
        private val GIT_MODULES_PATH_PATTERN: Pattern = Pattern.compile("\\s*path\\s*=\\s*([^\\s]+)\\s*")
    }
}
