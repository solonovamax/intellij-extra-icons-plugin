// SPDX-License-Identifier: MIT
package lermitage.intellij.extra.icons

import lermitage.intellij.extra.icons.utils.RESOURCE_BUNDLE
import org.jetbrains.annotations.Contract

enum class ModelType {
    FILE,
    DIR,
    ICON;

    val i18nFriendlyName: String
        get() = when (this) {
            FILE -> RESOURCE_BUNDLE.getString("model.type.file")
            DIR -> RESOURCE_BUNDLE.getString("model.type.directory")
            ICON -> RESOURCE_BUNDLE.getString("model.type.icon")
        }

    companion object {
        /**
         * [ModelType] comparator:
         * [ModelType.DIR] > [ModelType.FILE] > [ModelType.ICON].
         */
        @JvmStatic
        @Contract(pure = true)
        fun compare(o1: ModelType?, o2: ModelType?): Int {
            return if (o1 == o2) 0 else if (o1 == null) 1 else if (o2 == null) -1 else if (o1 == DIR && (o2 == FILE || o2 == ICON)) -1 else if (o1 == FILE && o2 == ICON) -1 else 1
        }
    }
}
