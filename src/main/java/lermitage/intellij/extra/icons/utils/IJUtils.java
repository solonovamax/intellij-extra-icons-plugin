// SPDX-License-Identifier: MIT

package lermitage.intellij.extra.icons.utils;

import com.intellij.ide.plugins.IdeaPluginDescriptor;
import com.intellij.ide.plugins.PluginManagerCore;
import com.intellij.openapi.application.ApplicationInfo;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.extensions.PluginId;
import com.intellij.util.ui.EDT;
import lermitage.intellij.extra.icons.lic.ExtraIconsPluginType;
import org.jetbrains.annotations.NonNls;

public class IJUtils {

    private static final @NonNls Logger LOGGER = Logger.getInstance(IJUtils.class);

    public static final boolean IS_IDE_2023_OR_OLDER = isIde2023OrOlder();

    /**
     * Indicate if plugin <a href="https://github.com/jonathanlermitage/IconViewer">Icon Viewer 2</a>
     * is installed and enabled.
     */
    public static boolean isIconViewer2Loaded() {
        return isPluginLoaded("lermitage.intellij.iconviewer");
    }

    /**
     * Indicate if plugin <b>Extra Icons Lifetime</b> is installed and enabled.
     */
    public static boolean isExtraIconsLifetimeLoaded() {
        return isPluginLoaded(ExtraIconsPluginType.LIFETIME.getPluginId());
    }

    public static boolean isPluginLoaded(String pluginId) {
        try {
            PluginId id = PluginId.findId(pluginId);
            if (id == null) {
                return false;
            }
            IdeaPluginDescriptor plugin = PluginManagerCore.getPlugin(id);
            if (plugin == null) {
                return false;
            }
            return plugin.isEnabled();
        } catch (Exception e) {
            LOGGER.warn("Can't determine if plugin '" + pluginId + "' is installed and enabled", e);
            return false;
        }
    }

    /**
     * Run given Runnable in EDT.
     * @param description description of what to run.
     * @param r what to run in EDT.
     */
    public static void runInEDT(String description, Runnable r) {
        if (EDT.isCurrentThreadEdt()) {
            LOGGER.info("Already in EDT to run: '" + description + "'");
            r.run();
        } else {
            ApplicationManager.getApplication().invokeLater(() -> {
                LOGGER.info("Enter in EDT in order to run: '" + description + "'");
                r.run();
            }, ModalityState.defaultModalityState());
        }
    }

    /**
     * Run given Runnable in BGT (i.e. outside EDT).
     * @param description description of what to run.
     * @param r what to run in BGT.
     * @param isReadAction is explicitly a Read Action.
     */
    public static void runInBGT(String description, Runnable r, boolean isReadAction) {
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            if (isReadAction) {
                ApplicationManager.getApplication().runReadAction(() -> {
                    LOGGER.info("Enter temporarily in BGT in order to run Read Action: '" + description + "', is in EDT: " + EDT.isCurrentThreadEdt());
                    r.run();
                });
            } else {
                ApplicationManager.getApplication().invokeLater(() -> {
                    LOGGER.info("Enter temporarily in BGT in order to invoke later: '" + description + "', is in EDT: " + EDT.isCurrentThreadEdt());
                    r.run();
                });
            }
        });
    }

    private static synchronized boolean isIde2023OrOlder() {
        try {
            return Integer.parseInt(ApplicationInfo.getInstance().getMajorVersion()) < 2024;
        } catch (Exception e) {
            LOGGER.warn("Failed to determine if IDE version is < 2024. Ignoring, and let's say it's < 2024", e);
            return false;
        }
    }
}
