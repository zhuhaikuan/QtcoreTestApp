/*
 * Copyright (C) 2025 Lenovo
 * All Rights Reserved.
 * Lenovo Confidential Restricted.
 */
package com.lenovo.quantum.test.util.audio

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

actual suspend fun writeBytesToPlatformFile(
    filePath: String,
    data: ByteArray
): Boolean {
    return withContext(Dispatchers.IO) {
        try {
            FileOutputStream(File(filePath)).use { fos ->
                fos.write(data)
            }
            true
        } catch (e: Exception) {
            println("Android FileWrite Error: ${e.message}")
            false
        }
    }
}