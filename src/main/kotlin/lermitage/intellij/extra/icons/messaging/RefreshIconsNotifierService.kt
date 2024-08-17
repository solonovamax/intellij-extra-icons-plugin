// SPDX-License-Identifier: MIT
package lermitage.intellij.extra.icons.messaging

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import lermitage.intellij.extra.icons.utils.isProjectAlive

@Service
class RefreshIconsNotifierService {
    fun triggerProjectIconsRefresh(project: Project?) {
        ApplicationManager.getApplication().runReadAction {
            if (project.isProjectAlive()) {
                checkNotNull(project)
                val refreshIconsNotifier = project.messageBus
                    .syncPublisher(RefreshIconsNotifier.EXTRA_ICONS_REFRESH_ICONS_NOTIFIER_TOPIC)
                refreshIconsNotifier.refreshProjectIcons(project)
            } else {
                LOGGER.warn("Project is not alive, can't refresh icons") // NON-NLS
            }
        }
    }

    fun triggerProjectIconEnablersReinit(project: Project?) {
        ApplicationManager.getApplication().runReadAction {
            if (project.isProjectAlive()) {
                checkNotNull(project)
                val refreshIconsNotifier = project.messageBus
                    .syncPublisher(RefreshIconsNotifier.EXTRA_ICONS_REFRESH_ICONS_NOTIFIER_TOPIC)

                refreshIconsNotifier.reinitProjectIconEnablers(project)
            } else {
                LOGGER.warn("Project is not alive, can't reinit icon enablers") // NON-NLS
            }
        }
    }

    fun triggerAllIconsRefreshAndIconEnablersReinit() {
        val projects = ProjectManager.getInstance().openProjects
        for (project in projects) {
            triggerProjectIconEnablersReinit(project)
            triggerProjectIconsRefresh(project)
        }
    }

    companion object {
        @JvmStatic
        val instance: RefreshIconsNotifierService
            get() = ApplicationManager.getApplication().getService(
                RefreshIconsNotifierService::class.java
            )

        val LOGGER = thisLogger()
    }
}
