package lermitage.intellij.extra.icons

import com.intellij.openapi.util.IconLoader

object ExtraIcons {
    @JvmField
    val GUTTER_CHECKBOX = getIcon("extra-icons/plugin-internals/gutterCheckBox.svg")

    @JvmField
    val GUTTER_CHECKBOX_SELECTED = getIcon("extra-icons/plugin-internals/gutterCheckBoxSelected.svg")

    @JvmField
    val REFRESH = getIcon("extra-icons/plugin-internals/refresh.svg")

    @JvmField
    val IMPORT = getIcon("extra-icons/plugin-internals/import.svg")

    @JvmField
    val WEB = getIcon("extra-icons/plugin-internals/web.svg")

    @JvmField
    val EXPORT = getIcon("extra-icons/plugin-internals/export.svg")

    @JvmField
    val CONTEXT_HELP = getIcon("extra-icons/plugin-internals/contextHelp.svg")

    @JvmField
    val REMOVE = getIcon("extra-icons/plugin-internals/remove.svg")

    @JvmField
    val REBOOT = getIcon("extra-icons/plugin-internals/reboot.svg")

    @JvmStatic
    fun getIcon(path: String) = IconLoader.getIcon(path, ExtraIcons::class.java)
}
