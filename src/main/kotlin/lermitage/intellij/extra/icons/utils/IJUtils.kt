// SPDX-License-Identifier: MIT
@file:JvmName("IJUtils")

package lermitage.intellij.extra.icons.utils

import com.intellij.ide.plugins.PluginManagerCore.getPlugin
import com.intellij.openapi.application.ApplicationInfo
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.extensions.PluginId
import com.intellij.util.ui.EDT

private val IJLOGGER: Logger = Logger.getInstance("#lermitage.intellij.extra.icons.utils.IJUtils")

/**
 * Indicate if plugin
 * [Icon Viewer 2](https://github.com/jonathanlermitage/IconViewer)
 * is installed and enabled.
 */
val isIconViewer2Loaded: Boolean
    get() = isPluginLoaded("lermitage.intellij.iconviewer")


private fun isPluginLoaded(pluginId: String): Boolean {
    try {
        val id = PluginId.findId(pluginId) ?: return false
        val plugin = getPlugin(id) ?: return false
        return plugin.isEnabled
    } catch (e: Exception) {
        IJLOGGER.warn("Can't determine if plugin '$pluginId' is installed and enabled", e)
        return false
    }
}

/**
 * Run given Runnable in EDT.
 *
 * @param description description of what to run.
 * @param runnable what to run in EDT.
 */
fun runInEDT(description: String, runnable: () -> Unit) {
    if (EDT.isCurrentThreadEdt()) {
        IJLOGGER.info("Already in EDT to run: '$description'")
        runnable()
    } else {
        ApplicationManager.getApplication().invokeLater({
            IJLOGGER.info("Enter in EDT in order to run: '$description'")
            runnable()
        }, ModalityState.defaultModalityState())
    }
}

/**
 * Run given Runnable in BGT (i.e. outside EDT).
 *
 * @param description description of what to run.
 * @param isReadAction is explicitly a Read Action.
 * @param runnable what to run in BGT.
 */
fun runInBGT(description: String, isReadAction: Boolean, runnable: () -> Unit) {
    ApplicationManager.getApplication().executeOnPooledThread {
        if (isReadAction)
            ApplicationManager.getApplication().runReadAction {
                IJLOGGER.info("Enter temporarily in BGT in order to run Read Action: '$description', is in EDT: ${EDT.isCurrentThreadEdt()}")
                runnable()
            }
        else
            ApplicationManager.getApplication().invokeLater {
                IJLOGGER.info("Enter temporarily in BGT in order to invoke later: '$description', is in EDT: ${EDT.isCurrentThreadEdt()}")
                runnable()
            }
    }
}

val ApplicationInfo.isIde2023OrOlder: Boolean
    get() {
        try {
            return majorVersion.toInt() < 2024
        } catch (e: Exception) {
            IJLOGGER.warn("Failed to determine if IDE version is < 2024. Ignoring, and let's say it's < 2024", e)
            return false
        }
    }
