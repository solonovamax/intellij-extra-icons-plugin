// SPDX-License-Identifier: MIT
@file:JvmName("Base64Utils")

package lermitage.intellij.extra.icons.utils

import java.util.Base64

/**
 * A Base64 thread-safe decoder.
 */
@JvmField
val B64_DECODER: Base64.Decoder = Base64.getDecoder()

/**
 * A Base64 thread-safe encoder.
 */
val B64_ENCODER: Base64.Encoder = Base64.getEncoder()
