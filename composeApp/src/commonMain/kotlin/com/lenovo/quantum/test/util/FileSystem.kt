/*
 * Copyright (C) 2025 Lenovo
 * All Rights Reserved.
 * Lenovo Confidential Restricted.
 */
package com.lenovo.quantum.test.util

open class FileSystem {
    open val appCacheDir: String = ""
}

expect fun getFileSystemCacheDir(): String