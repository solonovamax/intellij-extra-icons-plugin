// SPDX-License-Identifier: MIT
package lermitage.intellij.extra.icons

import com.intellij.openapi.diagnostic.debug
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.util.IconLoader.installPathPatcher
import com.intellij.openapi.util.IconPathPatcher
import com.intellij.ui.NewUI
import com.intellij.util.system.OS
import lermitage.intellij.extra.icons.cfg.services.SettingsIDEService
import lermitage.intellij.extra.icons.utils.B64_DECODER
import lermitage.intellij.extra.icons.utils.createOrGetTempSVGFile
import java.io.IOException
import kotlin.io.path.Path
import kotlin.io.path.name

class ExtraIconPatcher : IconPathPatcher() {
    private lateinit var icons: Map<String, String>

    init {
        loadConfig()
        installPathPatcher(this)
    }

    override fun getContextClassLoader(path: String, originalClassLoader: ClassLoader?): ClassLoader? {
        return ExtraIconPatcher::class.java.classLoader
    }

    override fun patchPath(path: String, classLoader: ClassLoader?): String? {
        val iconPath = Path(path)


        val simplifiedPath = path.substring(1)

        return when {
            icons.containsKey(path) -> icons[path]
            path.startsWith("/") && path.length > 2 && icons.containsKey(simplifiedPath) -> icons[simplifiedPath]
            else -> icons[iconPath.name]
        }
    }

    private fun loadConfig() {
        icons = getEnabledIcons()
        try {
            icons = convertUserB64IconsToLocalFilesAndKeepBundledIcons(icons)
        } catch (e: IOException) {
            LOGGER.warn("Cannot create temporary directory to store user IDE icons, this feature won't work", e)
        }
        LOGGER.info("Config loaded with success, enabled " + icons.size + " items")
    }

    /**
     * Convert Base64 user icons to local files. This conversion is needed
     * because IconPatcher can't work with in-memory byte arrays; we have to
     * use bundled icons (from /resources) or local files. This method stores
     * in-memory Base64 icons provided by user as temporary local files.
     */
    @Throws(IOException::class)
    private fun convertUserB64IconsToLocalFilesAndKeepBundledIcons(icons: Map<String, String>): Map<String, String> {
        val morphedIcons = mutableMapOf<String, String>()
        for ((key, icon) in icons) {
            morphedIcons[key] = if (icon.startsWith("extra-icons/")) {
                // bundled icon, no icon transformation needed
                icon
            } else {
                // base64 icon provided by user: store as local file
                val svgFile = createOrGetTempSVGFile(B64_DECODER.decode(icon))
                val decodedIconPath = svgFile.absolutePath
                if (OS.CURRENT == OS.Windows) // TODO see if should use VfsUtil.fixURLforIDEA(urlStr)
                    "file:/$decodedIconPath"
                else
                    "file://$decodedIconPath"
            }
        }
        return morphedIcons
    }

    companion object {
        private val LOGGER = thisLogger()

        private fun getEnabledIcons(): Map<String, String> {
            val uiType = if (NewUI.isEnabled()) UIType.NEW_UI else UIType.OLD_UI
            LOGGER.debug { "Detected UI Type: $uiType" }

            val preferNewUI = when (SettingsIDEService.getInstance().getUiTypeIconsPreference()) {
                UITypeIconsPreference.BASED_ON_ACTIVE_UI_TYPE -> NewUI.isEnabled()
                UITypeIconsPreference.PREFER_NEW_UI_ICONS -> true
                UITypeIconsPreference.PREFER_OLD_UI_ICONS -> false
                null -> error("This should be literally impossible. If you see this error, please report it")
            }

            val disabledModelIds = SettingsIDEService.getInstance().getDisabledModelIds()
            return sequenceOf(
                ExtraIconProvider.allModels(),
                SettingsIDEService.getInstance().getCustomModels()
            ).flatten().filter { model ->
                model.modelType == ModelType.ICON
            }.filterNot { model ->
                model.id in disabledModelIds
            }.filter { model ->
                model.isEnabled
            }.distinctBy { model -> model.icon }.associate { model ->
                val iconPathToLoad = if (preferNewUI && model.isAutoLoadNewUIIconVariant)
                    model.icon.replace("extra-icons/", "extra-icons/newui/")
                else
                    model.icon

                model.ideIcon to iconPathToLoad
            }
        }
    }
}
