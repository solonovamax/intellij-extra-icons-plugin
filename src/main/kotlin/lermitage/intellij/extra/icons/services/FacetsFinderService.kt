// SPDX-License-Identifier: MIT
package lermitage.intellij.extra.icons.services

import com.intellij.facet.FacetManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import java.util.Locale
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.DurationUnit
import kotlin.time.measureTimedValue

@Service(Service.Level.PROJECT)
class FacetsFinderService(project: Project) {
    /**
     * Get project's facets (as in `Project Structure > Projects Settings >
     * Facets`), like "Spring", "JPA", etc. **Facet names are in lowercase**.
     */
    @JvmField
    val facets = measureTimedValue {
        buildSet {
            try {
                val moduleManager = ModuleManager.getInstance(project)
                moduleManager.modules.forEach { module ->
                    val facetManager = FacetManager.getInstance(module)
                    this.addAll(facetManager.allFacets.map { facet -> facet.name.lowercase(Locale.getDefault()) })
                }
            } catch (e: Exception) {
                LOGGER.error(e)
            }
        }
    }.let { (facetsFound: Set<String>, execTime: Duration) ->
        if (execTime > 50.milliseconds) { // should be instant
            LOGGER.warn("Found facets $facetsFound for project $project in ${execTime.toString(DurationUnit.MILLISECONDS)}ms (should be instant)")
        }
        facetsFound
    }

    companion object {
        private val LOGGER = thisLogger()

        @JvmStatic
        fun getInstance(project: Project): FacetsFinderService {
            return project.getService(FacetsFinderService::class.java)
        }
    }
}
