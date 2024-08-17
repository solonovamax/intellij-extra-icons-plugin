// SPDX-License-Identifier: MIT
package lermitage.intellij.extra.icons.messaging

import com.intellij.openapi.project.Project
import com.intellij.util.messages.Topic
import com.intellij.util.messages.Topic.ProjectLevel

/**
 * This notifier is used to refresh icons for projects. See
 * [Messaging Infrastructure documentation](https://plugins.jetbrains.com/docs/intellij/messaging-infrastructure.html).
 */
interface RefreshIconsNotifier {
    /**
     * Refresh the icons for the specified project.
     *
     * @param project the project whose icons need to be refreshed.
     */
    fun refreshProjectIcons(project: Project?)

    /**
     * Re-initialize the icon enablers for the specified project.
     *
     * @param project the project whose icon enablers need to be initialized
     *    again.
     */
    fun reinitProjectIconEnablers(project: Project?)

    companion object {
        /**
         * The topic used for notifying the icons refresh handled by Extra Icons
         * plugin.
         */
        @JvmField
        @ProjectLevel
        val EXTRA_ICONS_REFRESH_ICONS_NOTIFIER_TOPIC = Topic.create("extra-icons refresh icons", RefreshIconsNotifier::class.java)
    }
}
