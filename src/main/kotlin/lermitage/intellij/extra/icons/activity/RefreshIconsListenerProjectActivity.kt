// SPDX-License-Identifier: MIT
package lermitage.intellij.extra.icons.activity

import com.intellij.ide.projectView.ProjectView
import com.intellij.openapi.application.ApplicationInfo
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.debug
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.fileEditor.ex.FileEditorManagerEx
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import com.intellij.ui.IconDeferrer
import lermitage.intellij.extra.icons.enablers.IconEnablerProvider.getIconEnabler
import lermitage.intellij.extra.icons.enablers.IconEnablerType
import lermitage.intellij.extra.icons.messaging.RefreshIconsNotifier
import lermitage.intellij.extra.icons.utils.isIde2023OrOlder
import lermitage.intellij.extra.icons.utils.isProjectAlive
import lermitage.intellij.extra.icons.utils.runInBGT
import lermitage.intellij.extra.icons.utils.runInEDT

// TODO migrate to Listener https://plugins.jetbrains.com/docs/intellij/plugin-listeners.html#defining-project-level-listeners
class RefreshIconsListenerProjectActivity : ProjectActivity {
    override suspend fun execute(project: Project) {
        val projectName = project.name
        project.messageBus.connect()
            .subscribe<RefreshIconsNotifier>(RefreshIconsNotifier.EXTRA_ICONS_REFRESH_ICONS_NOTIFIER_TOPIC, object : RefreshIconsNotifier {
                override fun refreshProjectIcons(project: Project?) {
                    LOGGER.debug { "refreshProjectIcons on project: $projectName" }
                    refreshIcons(project!!)
                }

                override fun reinitProjectIconEnablers(project: Project?) {
                    LOGGER.debug { "reinitProjectIconEnablers on project: $projectName" }
                    reinitIconEnablers(project!!)
                }
            })
    }

    private fun reinitIconEnablers(project: Project) {
        runInBGT("reinit icon enablers", true) {
            if (project.isProjectAlive()) {
                DumbService.getInstance(project).runReadActionInSmartMode {
                    for (iconEnablerType in IconEnablerType.entries) {
                        if (project.isProjectAlive()) {
                            val iconEnabler = project.getIconEnabler(iconEnablerType)
                            iconEnabler?.init(project)
                        }
                    }
                }
            }
        }
    }

    private fun refreshIcons(project: Project) {
        if (ApplicationInfo.getInstance().isIde2023OrOlder)
            IconDeferrer.getInstance().clearCache()

        runInEDT("refresh icons") {
            ApplicationManager.getApplication().runReadAction {
                if (project.isProjectAlive()) {
                    val view = ProjectView.getInstance(project) ?: return@runReadAction LOGGER.debug { "Project view is null" }

                    view.refresh()

                    // IJUtils.runInBGT("refresh ProjectView", view::refresh, true);
                    val currentProjectViewPane = view.currentProjectViewPane
                    currentProjectViewPane?.updateFromRoot(true) ?: LOGGER.debug { "Project view pane is null" }
                    // IJUtils.runInBGT("update AbstractProjectViewPane", () -> currentProjectViewPane.updateFromRoot(true), true);
                    try {
                        val editorWindows = FileEditorManagerEx.getInstanceEx(project).windows
                        for (editorWindow in editorWindows) {
                            try {
                                @Suppress("UnstableApiUsage")
                                editorWindow.manager.refreshIcons()

                                // IJUtils.runInBGT("refresh EditorWindow icons", () -> editorWindow.getManager().refreshIcons(), true);
                            } catch (e: Exception) {
                                LOGGER.warn(
                                    "Failed to refresh editor tabs icon (EditorWindow manager failed to refresh icons)",
                                    e
                                )
                            }
                        }
                    } catch (e: Exception) {
                        LOGGER.warn(
                            "Failed to refresh editor tabs icon (can't get FileEditorManagerEx instance or project's windows)",
                            e
                        )
                    }
                }
            }
        }
    }

    companion object {
        private val LOGGER = thisLogger()
    }
}
