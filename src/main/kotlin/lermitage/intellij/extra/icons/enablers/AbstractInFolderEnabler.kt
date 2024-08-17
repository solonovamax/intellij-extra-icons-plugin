// SPDX-License-Identifier: MIT
package lermitage.intellij.extra.icons.enablers

import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessModuleDir
import com.intellij.openapi.util.io.toNioPathOrNull
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.toNioPathOrNull
import com.intellij.psi.search.FilenameIndex
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.util.ui.EDT
import lermitage.intellij.extra.icons.cfg.services.SettingsIDEService
import java.nio.file.Path
import java.util.Arrays
import java.util.Locale
import kotlin.io.path.Path
import kotlin.io.path.exists
import kotlin.io.path.isDirectory
import kotlin.io.path.useDirectoryEntries

@Suppress("HardCodedStringLiteral")
abstract class AbstractInFolderEnabler : IconEnabler {
    /**
     * Parent folder(s) where files or folders should be located in order to
     * activate Enabler.
     */
    protected var enabledFolders: Set<Path> = emptySet()

    protected abstract val filenamesToSearch: Array<String>

    /**
     * The name of this icon enabler. Used to identify disabled icon enabler if
     * an error occurred.
     */
    abstract val name: String

    /**
     * A boolean flag used to obtain a match if any of the specified files
     * exists in the project.
     */
    open val requiredSearchedFiles: Boolean
        get() = true

    @Synchronized
    override fun init(project: Project) {
        try {
            enabledFolders = if (SettingsIDEService.getInstance().getUseIDEFilenameIndex2())
                initWithIDEFileIndex(project, filenamesToSearch)
            else
                initWithRegularFS(project, filenamesToSearch)
        } catch (e: Throwable) {
            LOGGER.warn("Canceled init of $name Enabler", e)
        }
    }

    /**
     * Look for given files in modules base path and level-1 sub-folders (by
     * querying regular FS, not IDE filename index), then return folders
     * containing at least one of these files.
     */
    private fun initWithRegularFS(project: Project, filenamesToSearch: Array<String>): Set<Path> {
        val moduleManager = ModuleManager.getInstance(project)
        val modulePaths = moduleManager.modules.asSequence().mapNotNull { module ->
            module.guessModuleDir()?.toNioPathOrNull()
        }.toSet()

        val foldersToEnable = mutableSetOf<Path>()
        modulePaths.forEach { modulePath: Path ->

            // look in modules root
            for (filenameToSearch in filenamesToSearch) {
                try {
                    if (modulePath.resolve(filenameToSearch).exists())
                        foldersToEnable.add(modulePath.normalize())
                } catch (e: Exception) {
                    LOGGER.warn("Failed to check '$modulePath/$filenameToSearch' existence", e)
                }
            }

            // look in modules level-1 sub-folders
            modulePath.useDirectoryEntries { entries ->
                entries.filter { it.isDirectory() }.forEach { dir ->
                    for (filenameToSearch in filenamesToSearch) {
                        try {
                            if (dir.resolve(filenameToSearch).exists())
                                foldersToEnable.add(dir.normalize())
                        } catch (e: Exception) {
                            LOGGER.warn("Failed to check '$modulePath/$filenameToSearch' existence", e)
                        }
                    }
                }
            }
        }

        return foldersToEnable
    }

    /**
     * Look for given files in project (by querying IDE filename index), then
     * return folders containing at least one of these files.
     */
    private fun initWithIDEFileIndex(project: Project, filenamesToSearch: Array<String>): Set<Path> {
        if (EDT.isCurrentThreadEdt()) { // we can no longer read index in EDT. See com.intellij.util.SlowOperations documentation
            LOGGER.warn("$name Enabler's init has been called while in EDT thread. Will try again later. Some icons override may not work.")
            return emptySet()
        }
        if (!project.isInitialized) {
            LOGGER.warn(
                "$name Enabler can't query IDE filename index: project ${project.name} is not initialized. Will try again later. " +
                        "Some icons override may not work."
            )
            return emptySet()
        }

        val allRequired = requiredSearchedFiles
        val virtualFilesByName = mutableListOf<VirtualFile>()

        for (filename in filenamesToSearch) {
            try {
                virtualFilesByName += FilenameIndex.getVirtualFilesByName(filename, true, GlobalSearchScope.projectScope(project))
                if (virtualFilesByName.isNotEmpty())
                    break
            } catch (e: Exception) {
                LOGGER.warn("$name Enabler failed to query IDE filename index. Will try again later. Some icons override may not work.", e)
                if (allRequired)
                    return emptySet()
            }
        }

        val additionalFilenamesToSearch = if (filenamesToSearch.size > 1 && allRequired)
            Arrays.copyOfRange(filenamesToSearch, 1, filenamesToSearch.size)
        else
            arrayOf()

        return virtualFilesByName.asSequence().mapNotNull { virtualFile: VirtualFile ->
            virtualFile.path.toNioPathOrNull()?.parent?.normalize()
        }.filter { folder: Path ->
            for (additionalFilenameToSearch in additionalFilenamesToSearch) {
                try {
                    if (!folder.resolve(additionalFilenameToSearch).exists())
                        return@filter false
                } catch (e: Exception) {
                    LOGGER.warn("$name Enabler failed to check $folder/$additionalFilenameToSearch existence", e)
                }
            }
            true
        }.toSet()
    }

    override fun verify(project: Project, absolutePathToVerify: String): Boolean {
        val normalizedPathToVerify = Path(absolutePathToVerify)
        return enabledFolders.any { normalizedPathToVerify.startsWith(it) }
    }

    protected fun normalizePath(path: String): String {
        return path.lowercase(Locale.getDefault())
            .replace("\\\\".toRegex(), "/")
            .replace("//".toRegex(), "/")
    }

    override fun terminatesConditionEvaluation() = false

    companion object {
        private val LOGGER = thisLogger()
    }
}
