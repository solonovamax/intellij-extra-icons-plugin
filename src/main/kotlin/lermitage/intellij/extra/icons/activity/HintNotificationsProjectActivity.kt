// SPDX-License-Identifier: MIT
package lermitage.intellij.extra.icons.activity

import com.intellij.notification.Notification
import com.intellij.notification.NotificationAction
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import com.intellij.openapi.ui.Messages
import lermitage.intellij.extra.icons.Globals
import lermitage.intellij.extra.icons.cfg.SettingsForm
import lermitage.intellij.extra.icons.cfg.services.SettingsIDEService
import lermitage.intellij.extra.icons.messaging.RefreshIconsNotifierService.Companion.instance
import lermitage.intellij.extra.icons.utils.RESOURCE_BUNDLE
import lermitage.intellij.extra.icons.utils.isIconViewer2Loaded
import lermitage.intellij.extra.icons.utils.isProjectAlive

/**
 * Display some useful hints in notifications on startup, a single time
 * only.
 */
class HintNotificationsProjectActivity : ProjectActivity {
    override suspend fun execute(project: Project) {
        if (!project.isProjectAlive()) {
            LOGGER.info("${javaClass.name} started before project is ready. Will not show startup notifications this time")
            return
        }
        val settingsIDEService = SettingsIDEService.getInstance()

        val alwaysShowNotifications = System.getProperty("extra-icons.always.show.notifications", "false").equals("true", ignoreCase = true)

        try {
            if (!settingsIDEService.getPluginIsConfigurableHintNotifDisplayed() || alwaysShowNotifications) {
                val notif = Notification(
                    Globals.PLUGIN_GROUP_DISPLAY_ID,
                    RESOURCE_BUNDLE.getString("notif.tips.plugin.config.title"),
                    RESOURCE_BUNDLE.getString("notif.tips.plugin.config.content"),
                    NotificationType.INFORMATION
                )
                notif.addAction(object : NotificationAction(RESOURCE_BUNDLE.getString("notif.tips.plugin.config.btn")) {
                    override fun actionPerformed(e: AnActionEvent, notification: Notification) {
                        ShowSettingsUtil.getInstance().showSettingsDialog(project, SettingsForm::class.java)
                    }
                })
                Notifications.Bus.notify(notif)
            }
        } finally {
            settingsIDEService.setPluginIsConfigurableHintNotifDisplayed(true)
        }

        if (isIconViewer2Loaded) {
            try {
                if (!settingsIDEService.getIconviewerShouldRenderSVGHintNotifDisplayed() || alwaysShowNotifications) {
                    val disabledModelIds = settingsIDEService.getDisabledModelIds()
                    if (!disabledModelIds.contains("ext_svg") || !disabledModelIds.contains("ext_svg_alt")) {
                        val notif = Notification(
                            Globals.PLUGIN_GROUP_DISPLAY_ID,
                            RESOURCE_BUNDLE.getString("notif.tips.plugin.config.title"),
                            RESOURCE_BUNDLE.getString("notif.tips.iconviewer.should.render.svg"),
                            NotificationType.INFORMATION
                        )
                        notif.addAction(object : NotificationAction(RESOURCE_BUNDLE.getString("notif.tips.iconviewer.should.render.svg.accept.btn")) {
                            override fun actionPerformed(e: AnActionEvent, notification: Notification) {
                                disabledModelIds.add("ext_svg")
                                disabledModelIds.add("ext_svg_alt")
                                settingsIDEService.setDisabledModelIds(disabledModelIds)
                                instance.triggerAllIconsRefreshAndIconEnablersReinit()
                                Messages.showInfoMessage(
                                    RESOURCE_BUNDLE.getString("configured.iconviewer.for.svg.rendering"),
                                    RESOURCE_BUNDLE.getString("configured.iconviewer.for.svg.rendering.title")
                                )
                                notif.hideBalloon()
                                notif.expire()
                            }
                        })
                        Notifications.Bus.notify(notif)
                    }
                }
            } finally {
                settingsIDEService.setIconviewerShouldRenderSVGHintNotifDisplayed(true)
            }
        }
    }

    companion object {
        private val LOGGER = thisLogger()
    }
}
