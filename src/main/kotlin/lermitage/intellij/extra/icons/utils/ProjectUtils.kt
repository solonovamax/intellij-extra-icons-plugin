// SPDX-License-Identifier: MIT
@file:JvmName("ProjectUtils")

package lermitage.intellij.extra.icons.utils

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager

val LOGGER = Logger.getInstance("#lermitage.intellij.extra.icons.utils.ProjectUtils")

val ProjectManager.firstOpenedProject: Project?
    get() = openProjects.firstOrNull()

/**
 * Return true if the project can be manipulated. Project is not null, not
 * disposed, etc. Developed to fix
 * [issue #39](https://github.com/jonathanlermitage/intellij-extra-icons-plugin/issues/39).
 */
fun Project?.isProjectAlive(): Boolean {
    if (this != null && !isDisposed) {
        return true
    } else {
        if (this == null)
            LOGGER.debug("Project is null")
        else
            LOGGER.warn("Project '$name' is not alive - Project is disposed: $isDisposed")

        return false
    }
}
