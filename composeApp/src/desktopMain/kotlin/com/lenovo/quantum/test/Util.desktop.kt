/*
 * Copyright (C) 2025 Lenovo
 * All Rights Reserved.
 * Lenovo Confidential Restricted.
 */
package com.lenovo.quantum.test

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.jetbrains.skia.Image

object DesktopUtil : Util() {

    private val locationScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var cachedLocation : String? = null

    override fun readAssetFile(path: String): String {
        val fullPath = "assets/$path"
        val stream = object {}.javaClass.classLoader.getResourceAsStream(fullPath)
            ?: throw IllegalArgumentException("Asset not found: $fullPath")
        return stream.bufferedReader().use { it.readText() }
    }

    override fun getLocation(
        context : Any?,
        latLongToLocation : suspend (Double, Double) -> String?
    ) : String {

        val os = System.getProperty("os.name").lowercase()
        if (!os.contains("windows")) return ""

        // launch location refresh in background
        if (cachedLocation.isNullOrEmpty()) {
            locationScope.launch {
                val coords = WindowsLocationHelper.getCurrentLatLng()
                coords?.let {
                    cachedLocation = latLongToLocation(coords.latitude, coords.longitude)
                }
            }
        }

        return cachedLocation ?: ""
    }
}

actual fun getUtil() : Util = DesktopUtil

actual fun ByteArray.toImageBitmap(): ImageBitmap {
    return Image.makeFromEncoded(this).toComposeImageBitmap()
}