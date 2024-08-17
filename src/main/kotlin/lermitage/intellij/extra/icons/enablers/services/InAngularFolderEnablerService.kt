// SPDX-License-Identifier: MIT
package lermitage.intellij.extra.icons.enablers.services

import com.intellij.openapi.components.Service
import lermitage.intellij.extra.icons.enablers.AbstractInFolderEnabler
import lermitage.intellij.extra.icons.enablers.IconEnabler

@Service(Service.Level.PROJECT)
class InAngularFolderEnablerService : AbstractInFolderEnabler(), IconEnabler {
    override fun getFilenamesToSearch() = arrayOf("angular.json")
    override fun getName() = "Angular icons"
}
