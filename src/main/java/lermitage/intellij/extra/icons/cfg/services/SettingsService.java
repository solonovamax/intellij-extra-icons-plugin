// SPDX-License-Identifier: MIT

package lermitage.intellij.extra.icons.cfg.services;

import com.intellij.notification.NotificationGroupManager;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.ui.scale.JBUIScale;
import lermitage.intellij.extra.icons.ExtraIconProvider;
import lermitage.intellij.extra.icons.Globals;
import lermitage.intellij.extra.icons.Model;
import lermitage.intellij.extra.icons.utils.I18nUtils;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public abstract class SettingsService {

    // the implementation of PersistentStateComponent works by serializing public fields, so keep them public
    @SuppressWarnings("WeakerAccess")
    public List<String> disabledModelIds = new ArrayList<>();
    @SuppressWarnings("WeakerAccess")
    public String ignoredPattern;
    @SuppressWarnings("WeakerAccess")
    public List<Model> customModels = new ArrayList<>();

    private Pattern ignoredPatternObj;
    private Boolean isIgnoredPatternValid;

    private static final @NonNls Logger LOGGER = Logger.getInstance(SettingsService.class);

    public static final double DEFAULT_ADDITIONAL_UI_SCALE = findSysScale(); // TODO see what fits best: JBUIScale.sysScale or JBUI.pixScale. JBUI.pixScale can take a frame, which may be useful with multiple displays

    private static double findSysScale() {
        try {
            return JBUIScale.sysScale();
        } catch (Throwable t) {
            LOGGER.warn(t);
            return 1;
        }
    }

    public List<String> getDisabledModelIds() {
        if (this.disabledModelIds == null) { // a malformed xml file could make it null
            this.disabledModelIds = new ArrayList<>();
        }
        return this.disabledModelIds;
    }

    public String getIgnoredPattern() {
        return this.ignoredPattern == null ? "" : this.ignoredPattern;
    }

    public Pattern getIgnoredPatternObj() {
        if (this.isIgnoredPatternValid == null) {
            compileAndSetIgnoredPattern(this.ignoredPattern);
        }
        if (this.isIgnoredPatternValid == Boolean.TRUE) {
            return this.ignoredPatternObj;
        }
        return null;
    }

    public void setDisabledModelIds(List<String> disabledModelIds) {
        this.disabledModelIds = disabledModelIds;
    }

    public void setIgnoredPattern(String ignoredPattern) {
        this.ignoredPattern = ignoredPattern;
        compileAndSetIgnoredPattern(ignoredPattern);
    }

    public List<Model> getCustomModels() {
        if (this.customModels == null) { // a malformed xml file could make it null
            this.customModels = new ArrayList<>();
        }
        return this.customModels;
    }

    public void setCustomModels(List<Model> customModels) {
        this.customModels = customModels;
    }

    public static @NotNull List<Model> getAllRegisteredModels() {
        return ExtraIconProvider.allModels();
    }

    private void compileAndSetIgnoredPattern(String regex) {
        if (regex != null && !regex.isEmpty()) {
            try {
                this.ignoredPatternObj = Pattern.compile(regex);
                this.isIgnoredPatternValid = true;
            } catch (PatternSyntaxException e) {
                NotificationGroupManager.getInstance().getNotificationGroup(Globals.PLUGIN_GROUP_DISPLAY_ID)
                    .createNotification(
                        MessageFormat.format(
                                I18nUtils.RESOURCE_BUNDLE.getString("notification.content.cant.compile.regex"),
                            regex,
                            e.getMessage()),
                        NotificationType.WARNING)
                    .setTitle(
                        MessageFormat.format(
                                I18nUtils.RESOURCE_BUNDLE.getString("notification.content.cant.compile.regex.title"),
                                I18nUtils.RESOURCE_BUNDLE.getString("extra.icons.plugin")))
                        .setSubtitle(I18nUtils.RESOURCE_BUNDLE.getString("notification.content.cant.compile.regex.subtitle"))
                    .setImportant(true)
                    .notify(null);
                this.ignoredPatternObj = null;
                this.isIgnoredPatternValid = false;
            }
        }
    }
}
