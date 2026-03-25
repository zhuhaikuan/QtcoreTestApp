/*
 * Copyright (C) 2025 Lenovo
 * All Rights Reserved.
 * Lenovo Confidential Restricted.
 */
package com.lenovo.quantum.test.util

import com.lenovo.quantum.test.appContext

class AndroidFileSystem : FileSystem() {
    override val appCacheDir: String
        get() = "${appContext.cacheDir.path}/"
}

actual fun getFileSystemCacheDir(): String {
    val fileSystem = AndroidFileSystem()
    return fileSystem.appCacheDir
}