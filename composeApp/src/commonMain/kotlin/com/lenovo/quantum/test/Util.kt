/*
 * Copyright (C) 2025 Lenovo
 * All Rights Reserved.
 * Lenovo Confidential Restricted.
 */
package com.lenovo.quantum.test

import androidx.compose.ui.graphics.ImageBitmap

open class Util {
    @JvmField
    val UNKNOWN = "Unknown"

    open fun isDevicePrc(): Boolean = false

    open fun isDeviceLenovoTablet() : Boolean = false

    open fun isDevicePrcLenovoTablet() : Boolean = false

    open fun readAssetFile(path: String): String = UNKNOWN

    open fun getLocation(
        context : Any?,
        latLongToLocation : suspend (Double, Double) -> String? = { _: Double, _: Double -> null }
    ) : String? = null
}

expect fun getUtil() : Util

expect fun ByteArray.toImageBitmap() : ImageBitmap