// SPDX-License-Identifier: MIT
package lermitage.intellij.extra.icons

object Globals {
    /**
     * Reflects `notificationGroup id` in `META-INF/settings.xml`.
     */
    const val PLUGIN_GROUP_DISPLAY_ID: String = "ExtraIcons"

    // Icons can be SVG or PNG only. Never allow user to pick GIF, JPEG, etc., otherwise
    // we should convert these files to PNG in IconUtils:toBase64 method.
    @JvmField
    val ALLOWED_ICON_FILE_EXTENSIONS: Array<String> = arrayOf("svg", "png") // NON-NLS
    const val ALLOWED_ICON_FILE_EXTENSIONS_FILE_SELECTOR_LABEL: String = "*.svg, *.png" // NON-NLS
}
