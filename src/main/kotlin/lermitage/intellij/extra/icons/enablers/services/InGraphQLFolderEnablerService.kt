// SPDX-License-Identifier: MIT
package lermitage.intellij.extra.icons.enablers.services

import com.intellij.openapi.components.Service
import lermitage.intellij.extra.icons.enablers.AbstractInFolderEnabler
import lermitage.intellij.extra.icons.enablers.IconEnabler

@Service(Service.Level.PROJECT)
class InGraphQLFolderEnablerService : AbstractInFolderEnabler(), IconEnabler {
    override fun getFilenamesToSearch() = arrayOf("schema.graphql", "schema.gql", "codegen.yml", ".graphqlconfig", "schema.graphql.json")
    override fun getName() = "GraphQL icons"
    override fun getRequiredSearchedFiles() = false
}
