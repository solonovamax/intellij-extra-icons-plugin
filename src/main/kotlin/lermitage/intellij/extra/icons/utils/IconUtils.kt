// SPDX-License-Identifier: MIT
@file:JvmName("IconUtils")

package lermitage.intellij.extra.icons.utils

import com.intellij.core.rwmutex.ReadCancellationException
import com.intellij.openapi.diagnostic.fileLogger
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.NewUI
import com.intellij.util.IconUtil
import com.intellij.util.system.OS
import com.intellij.util.ui.ImageUtil
import lermitage.intellij.extra.icons.ExtraIcons
import lermitage.intellij.extra.icons.IconType
import lermitage.intellij.extra.icons.Model
import lermitage.intellij.extra.icons.UITypeIconsPreference
import lermitage.intellij.extra.icons.cfg.services.SettingsService
import org.apache.commons.codec.digest.DigestUtils
import org.apache.commons.io.FileUtils
import java.awt.Image
import java.io.ByteArrayInputStream
import java.io.File
import java.io.IOException
import java.util.UUID
import javax.imageio.ImageIO
import javax.swing.Icon

private val ICON_LOGGER = fileLogger()

private const val SCALING_SIZE = 16

private val TMP_FOLDER = File(System.getProperty("java.io.tmpdir"))

private val IDE_INSTANCE_UNIQUE_ID = UUID.randomUUID().toString()

fun getIcon(model: Model, additionalUIScale: Double, uiTypeIconsPreference: UITypeIconsPreference): Icon? {
    return if (model.iconType == IconType.PATH) {
        val iconPathToLoad = getIconPathToLoad(model, uiTypeIconsPreference)
        ExtraIcons.getIcon(iconPathToLoad)
    } else {
        val base64 = loadImageFromBase64(model.icon, model.iconType, additionalUIScale) ?: return null
        IconUtil.createImageIcon(base64.image)
    }
}

private fun getIconPathToLoad(model: Model, uiTypeIconsPreference: UITypeIconsPreference): String {
    val preferNewUI = when (uiTypeIconsPreference) {
        UITypeIconsPreference.BASED_ON_ACTIVE_UI_TYPE -> NewUI.isEnabled()
        UITypeIconsPreference.PREFER_NEW_UI_ICONS -> true
        UITypeIconsPreference.PREFER_OLD_UI_ICONS -> false
    }

    return if (preferNewUI && model.isAutoLoadNewUIIconVariant)
        model.icon.replace("extra-icons/", "extra-icons/newui/")
    else
        model.icon
}


@Throws(IllegalArgumentException::class)
fun loadFromVirtualFile(virtualFile: VirtualFile): ImageWrapper? {
    return if (virtualFile.extension != null) {
        try {
            val imageBytes = virtualFile.contentsToByteArray()
            val iconType = when {
                virtualFile.extension == "svg" && imageBytes.decodeToString().startsWith("<") -> IconType.SVG
                else -> IconType.IMG
            }
            loadImage(imageBytes, iconType, SettingsService.DEFAULT_ADDITIONAL_UI_SCALE)
        } catch (ex: IOException) {
            throw IllegalArgumentException("IOException while trying to load image.", ex)
        }
    } else null
}

fun loadImageFromBase64(base64: String?, iconType: IconType, additionalUIScale: Double): ImageWrapper.Base64Image? {
    return loadImage(B64_DECODER.decode(base64), iconType, additionalUIScale)
}

/**
 * Creates or retrieves a temporary SVG file based on the provided image
 * bytes. If the SVG file already exists, it is returned. Otherwise, a new
 * file is created with the SHA1 of specified image bytes and returned.
 * The temporary file is deleted when the virtual machine terminates.
 *
 * @param imageBytes the bytes of the image.
 * @return the temporary SVG file.
 * @throws IOException if an I/O error occurs while creating or accessing
 *    the file.
 */
@Synchronized
@Throws(IOException::class)
fun createOrGetTempSVGFile(imageBytes: ByteArray): File {
    val svgFile = File(TMP_FOLDER, "extra-icons-user-icon" + IDE_INSTANCE_UNIQUE_ID + "-" + DigestUtils.sha1Hex(imageBytes) + ".svg")

    if (!svgFile.exists()) {
        svgFile.deleteOnExit()
        FileUtils.writeByteArrayToFile(svgFile, imageBytes)
    }

    return svgFile
}

private fun loadSVGAsImageWrapper(imageBytes: ByteArray, additionalUIScale: Double): ImageWrapper.Base64Image? {
    return try {
        val svgFile = createOrGetTempSVGFile(imageBytes)
        val svgFilePath = svgFile.absolutePath
        try {
            // TODO see if should use VfsUtil.fixURLforIDEA(urlStr)
            val prefix = if (OS.CURRENT == OS.Windows) "file:/" else "file://"
            val icon = ExtraIcons.getIcon(prefix + svgFile.absolutePath.replace("\\\\".toRegex(), "/"))
            val scaleSize = (SCALING_SIZE * additionalUIScale).toInt()
            when {
                icon.iconWidth == 16 -> ImageWrapper.Base64Image(IconType.SVG, IconUtil.toImage(icon), imageBytes)

                additionalUIScale == 1.0 || additionalUIScale == 2.0 -> { // ???
                    var image = IconUtil.toImage(icon)
                    image = ImageUtil.scaleImage(image, scaleSize, scaleSize)
                    ImageWrapper.Base64Image(IconType.SVG, image.scale(SCALING_SIZE), imageBytes)
                }

                else -> {
                    var image = IconUtil.toImage(icon)
                    image = ImageUtil.scaleImage(image, scaleSize, scaleSize)
                    ImageWrapper.Base64Image(IconType.SVG, image, imageBytes)
                }
            }
        } catch (e: Exception) {
            // avoid error report: com.intellij.openapi.application.rw.ReadCancellationException
            // java.lang.Throwable: Control-flow exceptions (e.g. this class com.intellij.openapi.progress.CeProcessCanceledException) should
            // never be logged. Instead, these should have been rethrown if caught.
            if (e is ReadCancellationException || e is ProcessCanceledException)
                ICON_LOGGER.warn("Can't load an SVG user icon (path: $svgFilePath): ${e.message}")
            else
                ICON_LOGGER.error(e)
            return null
        }
    } catch (e: IOException) {
        ICON_LOGGER.error(e)
        null
    }
}

fun loadImage(imageBytes: ByteArray, iconType: IconType, additionalUIScale: Double): ImageWrapper.Base64Image? {
    if (iconType == IconType.SVG) {
        return try {
            loadSVGAsImageWrapper(imageBytes, additionalUIScale)
        } catch (ex: Exception) {
            ICON_LOGGER.info("Can't load ${IconType.SVG} icon: ${ex.message}", ex)
            null
        }
    }

    val image = try {
        ByteArrayInputStream(imageBytes).use { byteArrayInputStream -> ImageIO.read(byteArrayInputStream) }
    } catch (e: IOException) {
        ICON_LOGGER.info("Can't load $iconType icon: ${e.message}", e)
        null
    } ?: return null

    val scaledImage = image.scale((SCALING_SIZE * additionalUIScale).toInt())
    return ImageWrapper.Base64Image(iconType, scaledImage, imageBytes)
}


fun ImageWrapper.toBase64(): String? {
    return when (this) {
        is ImageWrapper.Base64Image -> B64_ENCODER.encodeToString(imageAsByteArray)

        else -> null
    }
}

private fun Image.scale(scalingSize: Int): Image {
    return ImageUtil.scaleImage(this, scalingSize, scalingSize)
}


sealed interface ImageWrapper {
    val iconType: IconType

    data class Base64Image(
        override val iconType: IconType,
        val image: Image,
        val imageAsByteArray: ByteArray,
    ) : ImageWrapper

    data class BundledImage(
        val bundledIconRef: String,
    ) : ImageWrapper {
        override val iconType: IconType = IconType.PATH
    }
}
