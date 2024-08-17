// SPDX-License-Identifier: MIT
@file:JvmName("IconPackUtils")

package lermitage.intellij.extra.icons.utils

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import lermitage.intellij.extra.icons.cfg.IconPack
import java.io.File
import java.io.IOException
import kotlin.io.path.readText
import kotlin.io.path.writeText

private val gson: Gson = GsonBuilder()
    .disableHtmlEscaping()
    .create()

/**
 * Read an icon pack from a JSON file.
 *
 * @param file icon pack file.
 * @return icon pack.
 * @throws IOException if any error occurs reading the local file.
 */
@Throws(IOException::class)
fun readPackFromJsonFile(file: File): IconPack {
    val modelsAsJson = file.toPath().readText()
    return gson.fromJson(modelsAsJson, IconPack::class.java)
}

/**
 * Export an icon pack to a JSON file.
 *
 * @param file icon pack file.
 * @param iconPack icon pack.
 * @throws IOException if any error occurs writing the local file.
 */
@Throws(IOException::class)
fun writePackToJsonFile(file: File, iconPack: IconPack?) {
    val json = gson.toJson(iconPack, IconPack::class.java)
    file.toPath().writeText(json)
}
