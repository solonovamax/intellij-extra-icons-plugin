// SPDX-License-Identifier: MIT
package lermitage.intellij.extra.icons.utils

import lermitage.intellij.extra.icons.ModelTag

/**
 * [ComboBoxWithImageRenderer] item: PNG or SVG image + text.
 */
data class ComboBoxWithImageItem @JvmOverloads constructor(
    /**
     * item's text
     */
    val title: String,

    /**
     * item's image path relative to 'resources' folder, without leading `/`.
     * Example: `extra-icons/image.svg`
     */
    val imagePath: String? = null,
) {
    constructor(title: String, modelTag: ModelTag) : this(modelTag.icon, title)
}
