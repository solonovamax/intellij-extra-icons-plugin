// SPDX-License-Identifier: MIT
package lermitage.intellij.extra.icons.utils

import com.intellij.openapi.diagnostic.thisLogger
import lermitage.intellij.extra.icons.ExtraIcons
import java.awt.Component
import javax.swing.Icon
import javax.swing.ImageIcon
import javax.swing.JLabel
import javax.swing.JList
import javax.swing.ListCellRenderer

/**
 * A [JComboBox] renderer with PNG/SVG image + optional text. Example:
 * ```
 * myComboBox.setRenderer(new ComboBoxWithImageRenderer());
 * myComboBox.addItem(new ComboBoxWithImageItem("first item", "extra-icons/first_item.svg"));
 * ```
 * *
 */
class ComboBoxWithImageRenderer : JLabel(), ListCellRenderer<Any?> {
    init {
        isOpaque = true
        horizontalAlignment = LEFT
        verticalAlignment = CENTER
    }

    override fun getListCellRendererComponent(
        list: JList<*>,
        value: Any?,
        index: Int,
        isSelected: Boolean,
        cellHasFocus: Boolean,
    ): Component {
        var text: String? = null
        var icon: Icon? = null
        if (value == null) {
            return this
        }
        try {
            if (value is BundledIcon) {
                text = value.description
                icon = ExtraIcons.getIcon(value.iconPath)
            } else if (value is ComboBoxWithImageItem) {
                text = value.title
                val imagePath = value.imagePath
                icon = if (imagePath == null) {
                    ImageIcon()
                } else {
                    ExtraIcons.getIcon(imagePath)
                }
            } else if (value is String) {
                text = value
                icon = ImageIcon()
            }
            setText(text)
            setIcon(icon)
        } catch (e: Exception) {
            setText("(error, failed to display icon)") // NON-NLS
            setIcon(null)
            LOGGER.warn("failed to display icon $text: $icon", e)
        }

        if (isSelected) {
            background = list.selectionBackground
            foreground = list.selectionForeground
        } else {
            background = list.background
            foreground = list.foreground
        }
        iconTextGap = 6
        return this
    }

    companion object {
        private val LOGGER = thisLogger()
    }
}
