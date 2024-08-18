// SPDX-License-Identifier: MIT
package lermitage.intellij.extra.icons.enablers.services

import com.intellij.openapi.components.Service
import lermitage.intellij.extra.icons.enablers.AbstractInFolderEnabler
import lermitage.intellij.extra.icons.enablers.IconEnabler

@Service(Service.Level.PROJECT)
class InGraphQLFolderEnablerService : AbstractInFolderEnabler(), IconEnabler {
    override val filenamesToSearch = arrayOf("schema.graphql", "schema.gql", "codegen.yml", ".graphqlconfig", "schema.graphql.json")
    override val name = "GraphQL icons"
    override val requiredSearchedFiles = false
}
