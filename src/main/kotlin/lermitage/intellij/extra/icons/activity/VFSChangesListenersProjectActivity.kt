// SPDX-License-Identifier: MIT
package lermitage.intellij.extra.icons.activity

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.startup.ProjectActivity
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.vfs.newvfs.BulkFileListener
import com.intellij.openapi.vfs.newvfs.events.VFileEvent
import lermitage.intellij.extra.icons.enablers.IconEnablerProvider.getIconEnabler
import lermitage.intellij.extra.icons.enablers.IconEnablerType
import lermitage.intellij.extra.icons.enablers.services.GitSubmoduleFolderEnablerService
import lermitage.intellij.extra.icons.messaging.RefreshIconsNotifierService.Companion.instance

// TODO migrate to Listener https://plugins.jetbrains.com/docs/intellij/plugin-listeners.html#defining-project-level-listeners
class VFSChangesListenersProjectActivity : ProjectActivity {
    override suspend fun execute(project: Project) {
        project.messageBus.connect().subscribe(VirtualFileManager.VFS_CHANGES, object : BulkFileListener {
            override fun after(events: List<VFileEvent>) {
                refreshGitSubmodules(events, project)
            }
        })
    }

    // TODO big refactoring: update all enablers with modified files, this way there is no need to re-init manually from the UI, which would eliminate the problem of enablers (IDE filename index queries) from EDT
    private fun refreshGitSubmodules(events: List<VFileEvent?>, project: Project) {
        try {
            DumbService.getInstance(project).runReadActionInSmartMode {
                val fileIndex = ProjectRootManager.getInstance(project).fileIndex
                val gitmodulesUpdated = events.stream()
                    .anyMatch { vFileEvent: VFileEvent? ->
                        vFileEvent!!.file != null && vFileEvent.isFromSave
                                && fileIndex.isInProject(vFileEvent.file!!)
                                && vFileEvent.path.endsWith(GitSubmoduleFolderEnablerService.GIT_MODULES_FILENAME)
                    }
                if (gitmodulesUpdated) {
                    val iconEnabler = project.getIconEnabler(IconEnablerType.IS_GIT_SUBMODULE_FOLDER)
                    iconEnabler?.init(project)
                    instance.triggerProjectIconsRefresh(project)
                }
            }
        } catch (e: Exception) {
            LOGGER.warn(e)
        }
    }

    companion object {
        private val LOGGER = Logger.getInstance(
            VFSChangesListenersProjectActivity::class.java
        )
    }
}
