import com.adarshr.gradle.testlogger.theme.ThemeType
import com.github.benmanes.gradle.versions.reporter.PlainTextReporter
import com.github.benmanes.gradle.versions.reporter.result.Result
import com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask
import com.palantir.gradle.gitversion.VersionDetails
import groovy.lang.Closure
import org.apache.commons.io.FileUtils
import org.jetbrains.changelog.Changelog
import org.w3c.dom.Document
import java.io.BufferedReader
import java.io.ByteArrayInputStream
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.nio.file.Files
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.xpath.XPathConstants
import javax.xml.xpath.XPathFactory

plugins {
    id("java")
    id("org.jetbrains.intellij") version "1.16.1" // https://github.com/JetBrains/gradle-intellij-plugin
    id("org.jetbrains.changelog") version "2.2.0" // https://github.com/JetBrains/gradle-changelog-plugin
    id("com.github.ben-manes.versions") version "0.50.0" // https://github.com/ben-manes/gradle-versions-plugin
    id("com.adarshr.test-logger") version "4.0.0" // https://github.com/radarsh/gradle-test-logger-plugin
    id("com.palantir.git-version") version "3.0.0" // https://github.com/palantir/gradle-git-version
    id("com.github.andygoossens.modernizer") version "1.9.0" // https://github.com/andygoossens/gradle-modernizer-plugin
    id("biz.lermitage.oga") version "1.1.1" // https://github.com/jonathanlermitage/oga-gradle-plugin
}

val pluginXmlFile = projectDir.resolve("src/main/resources/META-INF/plugin.xml")
val pluginXmlFileBackup = projectDir.resolve("plugin.backup.xml")

val pluginLogoFile = projectDir.resolve("src/main/resources/META-INF/pluginIcon.svg")
val pluginLogoFileBackup = projectDir.resolve("pluginIcon.backup.svg")
val pluginLogoLifetimeFile = projectDir.resolve("misc/pluginIcon.lifetime.svg")
val pluginLogoFreeFile = projectDir.resolve("misc/pluginIcon.free.svg")

// Import variables from gradle.properties file
val pluginDownloadIdeaSources: String by project
val pluginVersion: String by project
val pluginJavaVersion: String by project
val testLoggerStyle: String by project
val pluginLicenseType: String by project
val pluginLanguage: String by project
val pluginCountry: String by project
val pluginEnableDebugLogs: String by project
val pluginEnforceIdeSlowOperationsAssertion: String by project
val pluginClearSandboxedIDESystemLogsBeforeRun: String by project
val pluginIdeaVersion = detectBestIdeVersion()
val pluginLifetimeLicenseDescriptionHeader: String by project
val pluginLifetimeLicenseId: String by project
val pluginLifetimeLicenseCode: String by project
val pluginLifetimeLicenseName: String by project
val pluginFreeId: String by project
val pluginFreeName: String by project

version = if (pluginVersion == "auto") {
    val versionDetails: Closure<VersionDetails> by extra
    val lastTag = versionDetails().lastTag
    if (lastTag.startsWith("v", ignoreCase = true)) {
        lastTag.substring(1)
    } else {
        lastTag
    }
} else {
    pluginVersion
}

logger.quiet("Will use IDEA $pluginIdeaVersion and Java $pluginJavaVersion. Plugin version set to $version")

group = "lermitage.intellij.extra.icons"

repositories {
    mavenCentral()
}

val junitVersion = "5.10.1"
val junitPlatformLauncher = "1.10.1"
val archunitVersion = "1.2.1"

dependencies {
    // needed because may not be bundled in Gateway's client
    implementation("org.apache.commons:commons-collections4:4.4")
    implementation("commons-codec:commons-codec:1.16.0")
    implementation("commons-io:commons-io:2.15.1")

    testImplementation("org.junit.jupiter:junit-jupiter-api:$junitVersion")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:$junitVersion")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:$junitPlatformLauncher")
    testImplementation("com.tngtech.archunit:archunit:$archunitVersion")
    testImplementation("com.github.weisj:jsvg:1.3.0")
}

intellij {
    when (pluginLicenseType) {
        "free" -> {
            pluginName.set("Extra Icons Free")
        }

        "lifetime" -> {
            pluginName.set("Extra Icons Lifetime")
        }

        else -> {
            pluginName.set("Extra Icons")
        }
    }
    downloadSources.set(pluginDownloadIdeaSources.toBoolean() && !System.getenv().containsKey("CI"))
    instrumentCode.set(true)
    sandboxDir.set("${rootProject.projectDir}/.idea-sandbox/${shortenIdeVersion(pluginIdeaVersion)}")
    updateSinceUntilBuild.set(false)
    version.set(pluginIdeaVersion)
}

changelog {
    headerParserRegex.set("(.*)".toRegex())
    itemPrefix.set("*")
}

modernizer {
    includeTestClasses = true
    // Find exclusion names at https://github.com/gaul/modernizer-maven-plugin/blob/master/modernizer-maven-plugin/src/main/resources/modernizer.xml
    exclusions = setOf("java/util/Optional.get:()Ljava/lang/Object;")
}

testlogger {
    try {
        theme = ThemeType.valueOf(testLoggerStyle)
    } catch (e: Exception) {
        theme = ThemeType.PLAIN
        logger.warn("Invalid testLoggerRichStyle value '$testLoggerStyle', " +
            "will use PLAIN style instead. Accepted values are PLAIN, STANDARD and MOCHA.")
    }
    showSimpleNames = true
}

tasks {
    register("verifyProductDescriptor") {
        // Ensure generated plugin requires a paid license
        doLast {
            val pluginXmlStr = pluginXmlFile.readText()
            if (!pluginXmlStr.contains("<product-descriptor")) {
                throw GradleException("plugin.xml: Product Descriptor is missing")
            }
            if (pluginXmlStr.contains("//FREE_LIC//")) {
                throw GradleException("plugin.xml: Product Descriptor is commented")
            }
        }
    }

    register("updatePluginCompatibilityRulesFromPluginXml") {
        doLast {
            var pluginXmlStr = pluginXmlFile.readText()
            when (pluginLicenseType) {
                "free" -> {
                    pluginXmlStr = pluginXmlStr.replace("<incompatible-with>REPLACED_BY_GRADLE</incompatible-with>",
                        "<!-- none -->")
                }

                "lifetime" -> {
                    pluginXmlStr = pluginXmlStr.replace("<incompatible-with>REPLACED_BY_GRADLE</incompatible-with>",
                        "<incompatible-with>lermitage.extra.icons.free</incompatible-with>")
                }

                else -> {
                    pluginXmlStr = pluginXmlStr.replace("<incompatible-with>REPLACED_BY_GRADLE</incompatible-with>",
                        "<incompatible-with>lermitage.extra.icons.lifetime</incompatible-with>" +
                            "<incompatible-with>lermitage.extra.icons.free</incompatible-with>")
                }
            }
            FileUtils.delete(pluginXmlFile)
            FileUtils.write(pluginXmlFile, pluginXmlStr, "UTF-8")
        }
    }

    register("backupPluginLogo") {
        doLast {
            if (pluginLogoFileBackup.exists()) {
                if (!pluginLogoFileBackup.delete()) {
                    throw GradleException("Failed to remove existing plugin logo file backup '${pluginLogoFileBackup}'")
                }
            }
            FileUtils.copyFile(pluginLogoFile, pluginLogoFileBackup)
        }
    }
    register("restorePluginLogoFromBackup") {
        doLast {
            if (!pluginLogoFileBackup.exists()) {
                throw GradleException("Plugin logo file backup '${pluginLogoFileBackup}' is missing")
            }
            if (pluginLogoFile.exists()) {
                if (!pluginLogoFile.delete()) {
                    throw GradleException("Failed to remove non-original plugin logo file backup '${pluginLogoFile}'")
                }
            }
            FileUtils.moveFile(pluginLogoFileBackup, pluginLogoFile)
        }
    }
    register("usePluginLogoFree") {
        doLast {
            if (pluginLogoFile.exists()) {
                if (!pluginLogoFile.delete()) {
                    throw GradleException("Failed to remove existing plugin logo file backup '${pluginLogoFile}'")
                }
            }
            FileUtils.copyFile(pluginLogoFreeFile, pluginLogoFile)
        }
    }
    register("usePluginLogoLifetime") {
        doLast {
            if (pluginLogoFile.exists()) {
                if (!pluginLogoFile.delete()) {
                    throw GradleException("Failed to remove existing plugin logo file backup '${pluginLogoFile}'")
                }
            }
            FileUtils.copyFile(pluginLogoLifetimeFile, pluginLogoFile)
        }
    }

    register("backupPluginXml") {
        doLast {
            if (pluginXmlFileBackup.exists()) {
                if (!pluginXmlFileBackup.delete()) {
                    throw GradleException("Failed to remove existing plugin xml file backup '${pluginXmlFileBackup}'")
                }
            }
            FileUtils.copyFile(pluginXmlFile, pluginXmlFileBackup)
        }
    }
    register("restorePluginXmlFromBackup") {
        doLast {
            if (!pluginXmlFileBackup.exists()) {
                throw GradleException("Plugin xml file backup '${pluginXmlFileBackup}' is missing")
            }
            if (pluginXmlFile.exists()) {
                if (!pluginXmlFile.delete()) {
                    throw GradleException("Failed to remove non-original plugin xml file backup '${pluginXmlFile}'")
                }
            }
            FileUtils.moveFile(pluginXmlFileBackup, pluginXmlFile)
        }
    }

    register("removeLicenseRestrictionFromPluginXml") {
        // Remove paid license requirement
        doLast {
            logger.warn("----------------------------------------------------------------")
            logger.warn("/!\\ Will build a plugin which doesn't ask for a paid license /!\\")
            logger.warn("----------------------------------------------------------------")
            var pluginXmlStr = pluginXmlFile.readText()
            pluginXmlStr = pluginXmlStr.replace("<id>lermitage.intellij.extra.icons</id>", "<id>${pluginFreeId}</id>")
            pluginXmlStr = pluginXmlStr.replace("<name>Extra Icons</name>", "<name>${pluginFreeName}</name>")
            val paidLicenceBlockRegex = "<product-descriptor code=\"\\w+\" release-date=\"\\d+\" release-version=\"\\d+\"/>".toRegex()
            val paidLicenceBlockStr = paidLicenceBlockRegex.find(pluginXmlStr)!!.value
            pluginXmlStr = pluginXmlStr.replace(paidLicenceBlockStr, "<!-- LICENSE REMOVED -->")
            FileUtils.delete(pluginXmlFile)
            FileUtils.write(pluginXmlFile, pluginXmlStr, "UTF-8")
        }
    }
    register("renameDistributionNoLicense") {
        // Rename generated plugin file to mention the fact that no paid license is needed
        doLast {
            val baseName = "build/distributions/Extra Icons-$version"
            val noLicPluginFile = projectDir.resolve("${baseName}-no-license.zip")
            val originalPluginFile = projectDir.resolve("${baseName}.zip")
            noLicPluginFile.delete()
            if (originalPluginFile.exists()) {
                FileUtils.moveFile(projectDir.resolve("${baseName}.zip"), noLicPluginFile)
            }
        }
    }

    register("renamePluginInfoToLifetimeInPluginXml") {
        doLast {
            var pluginXmlStr = pluginXmlFile.readText()
            pluginXmlStr = pluginXmlStr.replace("<id>lermitage.intellij.extra.icons</id>", "<id>$pluginLifetimeLicenseId</id>")
            pluginXmlStr = pluginXmlStr.replace("<name>Extra Icons</name>", "<name>$pluginLifetimeLicenseName</name>")
            pluginXmlStr = pluginXmlStr.replace("<product-descriptor code=\"PEXTRAICONS\"", "<product-descriptor code=\"$pluginLifetimeLicenseCode\"")
            pluginXmlStr = pluginXmlStr.replace("<description><![CDATA[", "<description><![CDATA[$pluginLifetimeLicenseDescriptionHeader")
            FileUtils.delete(pluginXmlFile)
            FileUtils.write(pluginXmlFile, pluginXmlStr, "UTF-8")
        }
    }
    register("renameDistributionLifetimeLicense") {
        doLast {
            val baseName = "build/distributions/Extra Icons-$version"
            val noLicPluginFile = projectDir.resolve("${baseName}-lifetime.zip")
            val originalPluginFile = projectDir.resolve("${baseName}.zip")
            noLicPluginFile.delete()
            if (originalPluginFile.exists()) {
                FileUtils.moveFile(projectDir.resolve("${baseName}.zip"), noLicPluginFile)
            }
        }
    }

    register("showGeneratedPlugin") {
        doLast {
            logger.quiet("--------------------------------------------------\n" +
                "Generated: " + projectDir.resolve("build/distributions/").list().contentToString()
                .replace("[", "").replace("]", " ").trim()
                + "\n--------------------------------------------------")
        }
    }
    register("clearSandboxedIDESystemLogs") {
        doFirst {
            if (pluginClearSandboxedIDESystemLogsBeforeRun.toBoolean()) {
                val sandboxLogDir = File("${rootProject.projectDir}/.idea-sandbox/${shortenIdeVersion(pluginIdeaVersion)}/system/log/")
                try {
                    if (sandboxLogDir.exists() && sandboxLogDir.isDirectory) {
                        FileUtils.deleteDirectory(sandboxLogDir)
                        logger.quiet("Deleted sandboxed IDE's log folder $sandboxLogDir")
                    }
                } catch (e: Exception) {
                    logger.warn("Failed do delete sandboxed IDE's log folder $sandboxLogDir - ignoring")
                }
            }
        }
    }
    withType<JavaCompile> {
        sourceCompatibility = pluginJavaVersion
        targetCompatibility = pluginJavaVersion
        options.compilerArgs = listOf("-Xlint:deprecation")
        options.encoding = "UTF-8"
    }
    withType<Test> {
        useJUnitPlatform()

        // avoid JBUIScale "Must be precomputed" error, because IDE is not started (LoadingState.APP_STARTED.isOccurred is false)
        jvmArgs("-Djava.awt.headless=true")

        // classpath indexing is not needed during unit tests
        // also, disabled to avoid useless 'NoSuchFileException: build/instrumented/instrumentCode/classpath.index.tmp' warnings
        systemProperties("idea.classpath.index.enabled" to false)
    }
    withType<DependencyUpdatesTask> {
        checkForGradleUpdate = true
        gradleReleaseChannel = "current"
        revision = "release"
        rejectVersionIf {
            isNonStable(candidate.version)
        }
        outputFormatter = closureOf<Result> {
            unresolved.dependencies.removeIf {
                val coordinates = "${it.group}:${it.name}"
                coordinates.startsWith("unzipped.com") || coordinates.startsWith("com.jetbrains:ideaI")
            }
            PlainTextReporter(project, revision, gradleReleaseChannel)
                .write(System.out, this)
        }
    }
    runIde {
        dependsOn("clearSandboxedIDESystemLogs")

        maxHeapSize = "1g" // https://docs.gradle.org/current/dsl/org.gradle.api.tasks.JavaExec.html

        if (pluginLanguage.isNotBlank()) {
            jvmArgs("-Duser.language=$pluginLanguage")
        }
        if (pluginCountry.isNotBlank()) {
            jvmArgs("-Duser.country=$pluginCountry")
        }
        if (System.getProperty("extra-icons.enable.chinese.ui", "false") == "true") {
            // force Chinese UI in plugin
            jvmArgs("-Dextra-icons.enable.chinese.ui=true")
        }
        if (System.getProperty("extra-icons.always.show.notifications", "false") == "true") {
            // show notifications on startup
            jvmArgs("-Dextra-icons.always.show.notifications=true")
        }

        // force detection of slow operations in EDT when playing with sandboxed IDE (SlowOperations.assertSlowOperationsAreAllowed)
        if (pluginEnforceIdeSlowOperationsAssertion.toBoolean()) {
            jvmArgs("-Dide.slow.operations.assertion=true")
        }

        if (pluginEnableDebugLogs.toBoolean()) {
            systemProperties(
                "idea.log.debug.categories" to "#lermitage.intellij.extra.icons"
            )
        }

        autoReloadPlugins.set(false)

        // If any warning or error with missing --add-opens, wait for the next gradle-intellij-plugin's update that should sync
        // with https://raw.githubusercontent.com/JetBrains/intellij-community/master/plugins/devkit/devkit-core/src/run/OpenedPackages.txt
        // or do it manually
    }
    buildSearchableOptions {
        enabled = false
    }
    patchPluginXml {
        when (pluginLicenseType) {
            "free" -> {
                dependsOn("backupPluginLogo", "usePluginLogoFree", "backupPluginXml", "removeLicenseRestrictionFromPluginXml", "updatePluginCompatibilityRulesFromPluginXml")
            }

            "lifetime" -> {
                dependsOn("backupPluginLogo", "usePluginLogoLifetime", "backupPluginXml", "renamePluginInfoToLifetimeInPluginXml", "updatePluginCompatibilityRulesFromPluginXml", "verifyProductDescriptor")
            }

            else -> {
                dependsOn("backupPluginXml", "updatePluginCompatibilityRulesFromPluginXml", "verifyProductDescriptor")
            }
        }
        changeNotes.set(provider {
            with(changelog) {
                renderItem(getLatest(), Changelog.OutputType.HTML)
            }
        })
    }
    buildPlugin {
        when (pluginLicenseType) {
            "free" -> {
                finalizedBy("restorePluginLogoFromBackup", "restorePluginXmlFromBackup", "renameDistributionNoLicense")
            }

            "lifetime" -> {
                finalizedBy("restorePluginLogoFromBackup", "restorePluginXmlFromBackup", "renameDistributionLifetimeLicense")
            }

            else -> {
                finalizedBy("restorePluginXmlFromBackup")
            }
        }
    }
    publishPlugin {
        token.set(System.getenv("JLE_IJ_PLUGINS_PUBLISH_TOKEN"))
    }
}

fun isNonStable(version: String): Boolean {
    if (listOf("RELEASE", "FINAL", "GA").any { version.uppercase().endsWith(it) }) {
        return false
    }
    return listOf("alpha", "Alpha", "ALPHA", "b", "beta", "Beta", "BETA", "rc", "RC", "M", "EA", "pr", "atlassian").any {
        "(?i).*[.-]${it}[.\\d-]*$".toRegex().matches(version)
    }
}

/** Return an IDE version string without the optional PATCH number.
 * In other words, replace IDE-MAJOR-MINOR(-PATCH) by IDE-MAJOR-MINOR. */
fun shortenIdeVersion(version: String): String {
    if (version.contains("SNAPSHOT", ignoreCase = true)) {
        return version
    }
    val matcher = Regex("[A-Za-z]+[\\-]?[0-9]+[\\.]{1}[0-9]+")
    return try {
        matcher.findAll(version).map { it.value }.toList()[0]
    } catch (e: Exception) {
        logger.warn("Failed to shorten IDE version $version: ${e.message}")
        version
    }
}

/** Find latest IntelliJ stable version from JetBrains website. Result is cached locally for 24h. */
fun findLatestStableIdeVersion(): String {
    val t1 = System.currentTimeMillis()
    val definitionsUrl = URL("https://www.jetbrains.com/updates/updates.xml")
    val cachedLatestVersionFile = File(System.getProperty("java.io.tmpdir") + "/jle-ij-latest-version.txt")
    var latestVersion: String
    try {
        if (cachedLatestVersionFile.exists()) {

            val cacheDurationMs = Integer.parseInt(project.findProperty("pluginIdeaVersionCacheDurationInHours") as String) * 60 * 60_000
            if (cachedLatestVersionFile.exists() && cachedLatestVersionFile.lastModified() < (System.currentTimeMillis() - cacheDurationMs)) {
                logger.quiet("Cache expired, find latest stable IDE version from $definitionsUrl then update cached file $cachedLatestVersionFile")
                latestVersion = getOnlineLatestStableIdeVersion(definitionsUrl)
                cachedLatestVersionFile.delete()
                Files.writeString(cachedLatestVersionFile.toPath(), latestVersion, Charsets.UTF_8)

            } else {
                logger.quiet("Find latest stable IDE version from cached file $cachedLatestVersionFile")
                latestVersion = Files.readString(cachedLatestVersionFile.toPath())!!
            }

        } else {
            logger.quiet("Find latest stable IDE version from $definitionsUrl")
            latestVersion = getOnlineLatestStableIdeVersion(definitionsUrl)
            Files.writeString(cachedLatestVersionFile.toPath(), latestVersion, Charsets.UTF_8)
        }

    } catch (e: Exception) {
        if (cachedLatestVersionFile.exists()) {
            logger.warn("Error: ${e.message}. Will find latest stable IDE version from cached file $cachedLatestVersionFile")
            latestVersion = Files.readString(cachedLatestVersionFile.toPath())!!
        } else {
            throw RuntimeException(e)
        }
    }
    if (logger.isDebugEnabled) {
        val t2 = System.currentTimeMillis()
        logger.debug("Operation took ${t2 - t1} ms")
    }
    return latestVersion
}

/** Find latest IntelliJ stable version from given url. */
fun getOnlineLatestStableIdeVersion(definitionsUrl: URL): String {
    val definitionsStr = readRemoteContent(definitionsUrl)
    val builderFactory = DocumentBuilderFactory.newInstance()
    val builder = builderFactory.newDocumentBuilder()
    val xmlDocument: Document = builder.parse(ByteArrayInputStream(definitionsStr.toByteArray()))
    val xPath = XPathFactory.newInstance().newXPath()
    val expression = "/products/product[@name='IntelliJ IDEA']/channel[@id='IC-IU-RELEASE-licensing-RELEASE']/build[1]/@version"
    return xPath.compile(expression).evaluate(xmlDocument, XPathConstants.STRING) as String
}

/** Read a remote file as String. */
fun readRemoteContent(url: URL): String {
    val t1 = System.currentTimeMillis()
    val content = StringBuilder()
    val conn = url.openConnection() as HttpURLConnection
    conn.requestMethod = "GET"
    BufferedReader(InputStreamReader(conn.inputStream)).use { rd ->
        var line: String? = rd.readLine()
        while (line != null) {
            content.append(line)
            line = rd.readLine()
        }
    }
    val t2 = System.currentTimeMillis()
    logger.quiet("Download $url, took ${t2 - t1} ms (${content.length} B)")
    return content.toString()
}

/** Get IDE version from gradle.properties or, of wanted, find latest stable IDE version from JetBrains website. */
fun detectBestIdeVersion(): String {
    val pluginIdeaVersionFromProps = project.findProperty("pluginIdeaVersion")
    if (pluginIdeaVersionFromProps.toString() == "IC-LATEST-STABLE") {
        return "IC-${findLatestStableIdeVersion()}"
    }
    if (pluginIdeaVersionFromProps.toString() == "IU-LATEST-STABLE") {
        return "IU-${findLatestStableIdeVersion()}"
    }
    return pluginIdeaVersionFromProps.toString()
}
