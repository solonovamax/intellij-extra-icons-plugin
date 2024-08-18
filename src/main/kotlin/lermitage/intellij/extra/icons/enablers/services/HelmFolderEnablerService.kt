// SPDX-License-Identifier: MIT
package lermitage.intellij.extra.icons.enablers.services

import com.intellij.openapi.components.Service
import lermitage.intellij.extra.icons.enablers.AbstractFolderEnabler
import lermitage.intellij.extra.icons.enablers.IconEnabler

@Service(Service.Level.PROJECT)
class HelmFolderEnablerService : AbstractFolderEnabler(), IconEnabler {
    override val filenamesToSearch = arrayOf("Chart.yaml", "values.yaml")
    override val name = "Helm    folder icon"
}
