// SPDX-License-Identifier: MIT
package lermitage.intellij.extra.icons.enablers

import com.intellij.openapi.project.Project

interface IconEnabler {
    fun init(project: Project)

    fun verify(project: Project, absolutePathToVerify: String): Boolean

    fun terminatesConditionEvaluation(): Boolean
}
