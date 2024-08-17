// SPDX-License-Identifier: MIT
package lermitage.intellij.extra.icons.enablers

import com.intellij.openapi.project.Project
import kotlin.io.path.Path

abstract class AbstractFolderEnabler : AbstractInFolderEnabler(), IconEnabler {
    override fun verify(project: Project, absolutePathToVerify: String): Boolean {
        val normalizedPathToVerify = Path(absolutePathToVerify).normalize()

        return enabledFolders.any { normalizedPathToVerify == it }
    }

    override fun terminatesConditionEvaluation(): Boolean {
        return true
    }
}
