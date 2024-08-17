// SPDX-License-Identifier: MIT
@file:JvmName("FileChooserUtils")

package lermitage.intellij.extra.icons.utils

import com.intellij.openapi.fileChooser.FileChooser
import com.intellij.openapi.fileChooser.FileChooserDescriptor
import com.intellij.openapi.util.registry.Registry
import com.intellij.openapi.vfs.VirtualFile
import java.awt.Component
import java.util.Arrays
import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter

private val IS_IDE_SLOW_OPERATIONS_ASSERTION_ENABLED = Registry.`is`("ide.slow.operations.assertion", false)

fun chooseFile(
    diagTitle: String,
    parent: Component,
    filterTitle: String?,
    vararg filterExtensions: String,
): String? = chooseFileOrFolder(FileChooserType.FILE, diagTitle, parent, filterTitle, *filterExtensions)

fun chooseFolder(
    diagTitle: String,
    parent: Component,
): String? = chooseFileOrFolder(FileChooserType.FOLDER, diagTitle, parent, null)

private fun chooseFileOrFolder(
    fileChooserType: FileChooserType,
    diagTitle: String,
    parent: Component,
    filterTitle: String?,
    vararg filterExtensions: String,
): String? {
    var filePath: String? = null
    if (IS_IDE_SLOW_OPERATIONS_ASSERTION_ENABLED) {
        // FIXME temporary workaround for "Slow operations are prohibited on EDT" issue
        //  https://github.com/jonathanlermitage/intellij-extra-icons-plugin/issues/126
        //  We should be able to use FileChooser.chooseFile instead of JFileChooser.showOpenDialog
        val dialogue = JFileChooser()
        dialogue.dialogTitle = diagTitle
        dialogue.isMultiSelectionEnabled = false
        if (fileChooserType == FileChooserType.FILE) {
            dialogue.fileSelectionMode = JFileChooser.FILES_ONLY
            dialogue.fileFilter = FileNameExtensionFilter(filterTitle, *filterExtensions)
        } else {
            dialogue.fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
        }
        dialogue.isAcceptAllFileFilterUsed = false
        if (dialogue.showOpenDialog(parent) == JFileChooser.APPROVE_OPTION) {
            filePath = dialogue.selectedFile.absolutePath
        }
    } else {
        val fileChooserDescriptor = FileChooserDescriptor(
            fileChooserType == FileChooserType.FILE, fileChooserType == FileChooserType.FOLDER, false,
            false, false, false
        )
        fileChooserDescriptor.title = diagTitle
        fileChooserDescriptor.isHideIgnored = false
        fileChooserDescriptor.isShowFileSystemRoots = true
        if (fileChooserType == FileChooserType.FILE) {
            fileChooserDescriptor.withFileFilter { virtualFile: VirtualFile ->
                Arrays.stream(
                    filterExtensions
                ).anyMatch { s: String -> s.equals(virtualFile.extension, ignoreCase = true) }
            }
        }
        val virtualFile = FileChooser.chooseFile(fileChooserDescriptor, null, null)
        filePath = virtualFile?.path
    }
    return filePath
}

private enum class FileChooserType {
    FILE,
    FOLDER
}
