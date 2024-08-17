// SPDX-License-Identifier: MIT
package lermitage.intellij.extra.icons.utils

import com.intellij.ide.BrowserUtil
import com.intellij.ide.plugins.PluginManagerCore.getPlugin
import com.intellij.openapi.application.ApplicationInfo
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.diagnostic.ErrorReportSubmitter
import com.intellij.openapi.diagnostic.IdeaLoggingEvent
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.diagnostic.SubmittedReportInfo
import com.intellij.openapi.extensions.PluginId
import com.intellij.openapi.util.NlsActions.ActionText
import com.intellij.openapi.util.SystemInfo
import com.intellij.util.Consumer
import com.intellij.util.ModalityUiUtil
import org.apache.http.client.utils.URIBuilder
import java.awt.Component
import java.net.URI
import java.net.URISyntaxException
import java.util.stream.Collectors
import kotlin.math.max
import kotlin.streams.asStream

/**
 * Error reporter which prefills an issue on Extra Icons' GitHub
 * repository. Highly inspired from **git-machete-intellij-plugin**'s code.
 */
class ExtraIconsErrorReportSubmitter : ErrorReportSubmitter() {
    @ActionText
    override fun getReportActionText(): String {
        return RESOURCE_BUNDLE.getString("error.report.btn.title")
    }

    override fun submit(
        events: Array<IdeaLoggingEvent>,
        additionalInfo: String?,
        parentComponent: Component,
        consumer: Consumer<in SubmittedReportInfo?>,
    ): Boolean {
        try {
            val uri = constructNewGitHubIssueUri(events, additionalInfo)
            ModalityUiUtil.invokeLaterIfNeeded(ModalityState.any()) {
                BrowserUtil.browse(uri)
            }
        } catch (e: Exception) {
            LOGGER.error("Failed to prepare Extra Icons error reporter", e)
            return false
        }
        return true
    }

    @Throws(URISyntaxException::class)
    fun constructNewGitHubIssueUri(events: Array<IdeaLoggingEvent>, additionalInfo: String?): URI {
        val uriBuilder = URIBuilder("https://github.com/jonathanlermitage/intellij-extra-icons-plugin/issues/new")
        val title = events.joinToString(separator = "; ") { event ->
            val throwable = event.throwable
            val exceptionMessage = event.throwableText.lineSequence().asStream().findFirst().orElse("")
            (if (throwable != null) exceptionMessage else event.message).trimEnd()
        }

        uriBuilder.setParameter("title", title)
        uriBuilder.setParameter("labels", "bug")

        var uri: URI
        var reportBodyLines = getReportBody(events, additionalInfo).lineSequence().asStream().collect(Collectors.toList())
        do {
            // Let's cut the body gradually line-by-line until the resulting URI fits into the GitHub limits.
            // It's hard to predict the perfect exact cut in advance due to URL encoding.
            reportBodyLines = reportBodyLines.subList(0, reportBodyLines.size - 1)
            uriBuilder.setParameter("body", reportBodyLines.stream().collect(Collectors.joining(System.lineSeparator())))
            uri = uriBuilder.build()
        } while (uri.toString().length > MAX_GITHUB_URI_LENGTH)

        return uri
    }

    private fun getReportBody(
        events: Array<IdeaLoggingEvent>,
        additionalInfo: String?,
    ): String {
        var reportBody = BUG_TEMPLATE.trimIndent()
        for ((key, value) in getTemplateVariables(events, additionalInfo))
            reportBody = reportBody.replace("%$key%", value)

        return reportBody
    }

    @Suppress("HardCodedStringLiteral")
    private fun getTemplateVariables(
        events: Array<IdeaLoggingEvent>,
        additionalInfo: String?,
    ): Map<String, String> {
        val templateVariables: MutableMap<String, String> = HashMap()

        templateVariables["ide"] = ApplicationInfo.getInstance().fullApplicationName

        val pluginDescriptor = getPlugin(PluginId.getId("lermitage.intellij.extra.icons"))
        val pluginVersion = if (pluginDescriptor == null) "<unknown>" else pluginDescriptor.version
        templateVariables["pluginVersion"] = pluginVersion

        templateVariables["os"] = SystemInfo.getOsNameAndVersion() // ApplicationInfo.getInstance().

        templateVariables["additionalInfo"] = additionalInfo ?: "N/A"

        val nl = System.lineSeparator()
        templateVariables["stacktraces"] = events.joinToString(separator = "\n\n") { event ->
            val messagePart = if (event.message != null) (event.message + nl + nl) else ""
            val throwablePart = shortenExceptionsStack(event.throwableText.trimEnd())
            "\n$messagePart$throwablePart\nl"
        }

        return templateVariables
    }

    @Suppress("HardCodedStringLiteral")
    private fun shortenExceptionsStack(stackTrace: String): String {
        val nl = System.lineSeparator()
        val rootCauseIndex = max(
            stackTrace.lastIndexOf("Caused by:").toDouble(),
            stackTrace.lastIndexOf("\tSuppressed:").toDouble()
        ).toInt()

        if (rootCauseIndex != -1) {
            val rootCauseStackTrace = stackTrace.substring(rootCauseIndex)
            val lines = stackTrace.substring(0, rootCauseIndex).split(nl.toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()

            val resultString = StringBuilder()
            for (i in lines.indices) {
                if (lines[i].contains("Caused by:") || lines[i].contains("Suppressed:") || i == 0) {
                    resultString.append(lines[i]).append(nl)
                    if (i + 1 < lines.size) {
                        resultString.append(lines[i + 1]).append("...").append(nl)
                    }
                }
            }
            return resultString.append(rootCauseStackTrace).toString()
        }
        return stackTrace
    }

    companion object {
        private val LOGGER: Logger = Logger.getInstance(ExtraIconsErrorReportSubmitter::class.java)
        private const val BUG_TEMPLATE: String = """
            ## Running environment
            - Extra Icons plugin version - %pluginVersion%
            - IDE - %ide%
            - OS - %os%
    
            ## Bug description
            Please include steps to reproduce (like `go to...`/`click on...` etc.) + expected and actual behaviour.  
            Please attach **IDE logs**. Open your IDE and go to <kbd>Help</kbd>, <kbd>Show Log in Explorer</kbd>, then pick `idea.log`.
    
            ## IDE - additional info
            %additionalInfo%
    
            ## IDE - stack trace
            ```%stacktraces%
        """
        private const val MAX_GITHUB_URI_LENGTH = 8192
    }
}
