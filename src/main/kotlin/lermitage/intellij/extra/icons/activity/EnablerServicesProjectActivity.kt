// SPDX-License-Identifier: MIT
package lermitage.intellij.extra.icons.activity

import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import lermitage.intellij.extra.icons.messaging.RefreshIconsNotifierService.Companion.instance

/**
 * Re-init icon enablers on project init, once indexing tasks are done.
 * May fix init issues when querying IDE filename index while indexing. At
 * least, it fixes icons reloading (Enablers) after long indexing tasks
 * (example: after you asked to invalidate caches).
 */
class EnablerServicesProjectActivity : ProjectActivity {
    override suspend fun execute(project: Project) {
        instance.triggerProjectIconEnablersReinit(project)
    }
}
