/*
 * Copyright (C) 2025 Lenovo
 * All Rights Reserved.
 * Lenovo Confidential Restricted.
 */
package com.lenovo.quantum.test.util.audio

expect suspend fun writeBytesToPlatformFile(filePath: String, data: ByteArray): Boolean