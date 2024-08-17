// SPDX-License-Identifier: MIT
package lermitage.intellij.extra.icons.enablers

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import lermitage.intellij.extra.icons.enablers.services.GitSubmoduleFolderEnablerService
import lermitage.intellij.extra.icons.enablers.services.HelmFolderEnablerService
import lermitage.intellij.extra.icons.enablers.services.InAngularFolderEnablerService
import lermitage.intellij.extra.icons.enablers.services.InFlutterFolderEnablerService
import lermitage.intellij.extra.icons.enablers.services.InGraphQLFolderEnablerService
import lermitage.intellij.extra.icons.enablers.services.InHelmFolderEnablerService
import lermitage.intellij.extra.icons.enablers.services.InWritersideFolderEnablerService

object IconEnablerProvider {
    @JvmStatic
    fun Project?.getIconEnabler(type: IconEnablerType?): IconEnabler? {
        if (this == null || type == null)
            return null

        return when (type) {
            IconEnablerType.IS_GIT_SUBMODULE_FOLDER -> service<GitSubmoduleFolderEnablerService>()
            IconEnablerType.IS_HELM_FOLDER -> service<HelmFolderEnablerService>()
            IconEnablerType.IS_IN_ANGULAR_FOLDER -> service<InAngularFolderEnablerService>()
            IconEnablerType.IS_IN_FLUTTER_FOLDER -> service<InFlutterFolderEnablerService>()
            IconEnablerType.IS_IN_GRAPHQL_FOLDER -> service<InGraphQLFolderEnablerService>()
            IconEnablerType.IS_IN_HELM_FOLDER -> service<InHelmFolderEnablerService>()
            IconEnablerType.IS_IN_WRITERSIDE_FOLDER -> service<InWritersideFolderEnablerService>()
        }
    }
}
