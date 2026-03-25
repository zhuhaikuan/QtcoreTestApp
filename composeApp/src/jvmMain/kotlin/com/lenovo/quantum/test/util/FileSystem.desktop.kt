/*
 * Copyright (C) 2025 Lenovo
 * All Rights Reserved.
 * Lenovo Confidential Restricted.
 */
package com.lenovo.quantum.testapp.util

import com.lenovo.quantum.sdk.logging.logD
import java.nio.file.Paths
import java.nio.file.Path

class DesktopFileSystem : FileSystem() {
    private val userHome: Path = Paths.get(System.getProperty("user.home"))
    private val musicDir: Path = userHome.resolve("Music")

    //TODO Define Desktop Quantum's app cache dir
    override val appCacheDir: String
        get() = "$musicDir\\"
}

actual fun getFileSystemCacheDir(): String {
    val fileSystem = DesktopFileSystem()
    val path = fileSystem.appCacheDir
    logD { "getFileSystemCacheDir path: '$path'" }
    return path
}