// SPDX-License-Identifier: MIT
package lermitage.intellij.extra.icons.enablers.services

import com.intellij.openapi.components.Service
import lermitage.intellij.extra.icons.enablers.AbstractInFolderEnabler
import lermitage.intellij.extra.icons.enablers.IconEnabler

@Service(Service.Level.PROJECT)
class InHelmFolderEnablerService : AbstractInFolderEnabler(), IconEnabler {
    override val filenamesToSearch = arrayOf("Chart.yaml", "values.yaml")
    override val name = "Helm icons"
}
