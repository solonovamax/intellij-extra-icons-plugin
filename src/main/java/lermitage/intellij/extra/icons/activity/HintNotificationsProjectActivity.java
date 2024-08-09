// SPDX-License-Identifier: MIT

package lermitage.intellij.extra.icons.activity;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationAction;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.ProjectActivity;
import com.intellij.openapi.ui.Messages;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import lermitage.intellij.extra.icons.Globals;
import lermitage.intellij.extra.icons.cfg.SettingsForm;
import lermitage.intellij.extra.icons.cfg.services.SettingsIDEService;
import lermitage.intellij.extra.icons.messaging.RefreshIconsNotifierService;
import lermitage.intellij.extra.icons.utils.I18nUtils;
import lermitage.intellij.extra.icons.utils.IJUtils;
import lermitage.intellij.extra.icons.utils.ProjectUtils;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.ResourceBundle;

/**
 * Display some useful hints in notifications on startup, a single time only.
 */
public class HintNotificationsProjectActivity implements ProjectActivity {

    private static final @NonNls Logger LOGGER = Logger.getInstance(HintNotificationsProjectActivity.class);

    private static final ResourceBundle i18n = I18nUtils.getResourceBundle();

    @Nullable
    @Override
    public Object execute(@NotNull Project project, @NotNull Continuation<? super Unit> continuation) {
        if (!ProjectUtils.isProjectAlive(project)) {
            LOGGER.info(this.getClass().getName() + " started before project is ready. Will not show startup notifications this time");
            return null;
        }
        SettingsIDEService settingsIDEService = SettingsIDEService.getInstance();

        boolean alwaysShowNotifications = System.getProperty("extra-icons.always.show.notifications", "false").equals("true"); //NON-NLS

        try {
            if (!settingsIDEService.getPluginIsConfigurableHintNotifDisplayed() || alwaysShowNotifications) {
                Notification notif = new Notification(Globals.PLUGIN_GROUP_DISPLAY_ID,
                    i18n.getString("notif.tips.plugin.config.title"),
                    i18n.getString("notif.tips.plugin.config.content"),
                    NotificationType.INFORMATION);
                notif.addAction(new NotificationAction(i18n.getString("notif.tips.plugin.config.btn")) {
                    @Override
                    public void actionPerformed(@NotNull AnActionEvent e, @NotNull Notification notification) {
                        ShowSettingsUtil.getInstance().showSettingsDialog(project, SettingsForm.class);
                    }
                });
                Notifications.Bus.notify(notif);
            }
        } finally {
            settingsIDEService.setPluginIsConfigurableHintNotifDisplayed(true);
        }

        // TODO uncomment once Lifetime licences are available
        /*if (!IJUtils.isExtraIconsLifetimeLoaded()) {
            try {
                if (!settingsIDEService.getLifetimeLicIntroHintNotifDisplayed() || alwaysShowNotifications) {
                    Notification notif = new Notification(Globals.PLUGIN_GROUP_DISPLAY_ID,
                        i18n.getString("notif.tips.lifetime.lic.intro.title"),
                        i18n.getString("notif.tips.lifetime.lic.intro.content"),
                        NotificationType.INFORMATION);
                    notif.addAction(new NotificationAction(i18n.getString("notif.tips.lifetime.lic.intro.btn")) {
                        @Override
                        public void actionPerformed(@NotNull AnActionEvent e, @NotNull Notification notification) {
                            BrowserUtil.browse("https://github.com/jonathanlermitage/intellij-extra-icons-plugin/blob/master/docs/LICENSE_FAQ.md#how-to-get-a-lifetime-license");
                        }
                    });
                    Notifications.Bus.notify(notif);
                }
            } finally {
                settingsIDEService.setLifetimeLicIntroHintNotifDisplayed(true);
            }
        }*/

        if (IJUtils.isIconViewer2Loaded()) {
            try {
                if (!settingsIDEService.getIconviewerShouldRenderSVGHintNotifDisplayed() || alwaysShowNotifications) {
                    List<String> disabledModelIds = settingsIDEService.getDisabledModelIds();
                    if (!disabledModelIds.contains("ext_svg") || !disabledModelIds.contains("ext_svg_alt")) { //NON-NLS
                        Notification notif = new Notification(Globals.PLUGIN_GROUP_DISPLAY_ID,
                            i18n.getString("notif.tips.plugin.config.title"),
                            i18n.getString("notif.tips.iconviewer.should.render.svg"),
                            NotificationType.INFORMATION);
                        notif.addAction(new NotificationAction(i18n.getString("notif.tips.iconviewer.should.render.svg.accept.btn")) {
                            @Override
                            public void actionPerformed(@NotNull AnActionEvent e, @NotNull Notification notification) {
                                disabledModelIds.add("ext_svg"); //NON-NLS
                                disabledModelIds.add("ext_svg_alt"); //NON-NLS
                                settingsIDEService.setDisabledModelIds(disabledModelIds);
                                RefreshIconsNotifierService.getInstance().triggerAllIconsRefreshAndIconEnablersReinit();
                                Messages.showInfoMessage(
                                    i18n.getString("configured.iconviewer.for.svg.rendering"),
                                    i18n.getString("configured.iconviewer.for.svg.rendering.title")
                                );
                                notif.hideBalloon();
                                notif.expire();
                            }
                        });
                        Notifications.Bus.notify(notif);
                    }
                }
            } finally {
                settingsIDEService.setIconviewerShouldRenderSVGHintNotifDisplayed(true);
            }
        }

        return null;
    }
}
