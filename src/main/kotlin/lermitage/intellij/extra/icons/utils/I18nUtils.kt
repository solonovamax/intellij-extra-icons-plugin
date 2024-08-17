// SPDX-License-Identifier: MIT
@file:JvmName("I18nUtils")

package lermitage.intellij.extra.icons.utils

import com.intellij.ide.plugins.PluginManager
import java.util.Locale
import java.util.ResourceBundle


@JvmField
var IS_CHINESE_UI_ENABLED = PluginManager.getLoadedPlugins().any { it.pluginId.idString == "com.intellij.zh" }

private val pluginLocale: Locale =
    if (IS_CHINESE_UI_ENABLED || System.getProperty("extra-icons.enable.chinese.ui", "false").equals("true", true))
        Locale.CHINA
    else
        Locale.ROOT

@JvmField
val RESOURCE_BUNDLE: ResourceBundle = ResourceBundle.getBundle("ExtraIconsI18n", pluginLocale)
