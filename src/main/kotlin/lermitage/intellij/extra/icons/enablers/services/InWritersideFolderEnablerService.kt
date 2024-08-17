// SPDX-License-Identifier: MIT
package lermitage.intellij.extra.icons.enablers.services

import com.intellij.openapi.components.Service
import lermitage.intellij.extra.icons.enablers.AbstractInFolderEnabler
import lermitage.intellij.extra.icons.enablers.IconEnabler

@Service(Service.Level.PROJECT)
class InWritersideFolderEnablerService : AbstractInFolderEnabler(), IconEnabler {
    override fun getFilenamesToSearch() = arrayOf("writerside.cfg")
    override fun getName() = "Writerside icons"
}
