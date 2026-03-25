/*
 * Copyright (C) 2025 Lenovo
 * All Rights Reserved.
 * Lenovo Confidential Restricted.
 */
package com.lenovo.quantum.test.client

import androidx.compose.ui.graphics.ImageBitmap
import org.json.JSONObject

data class FileAttachment(
    val uri : String,
    val fileName : String,
    val mimeType : String,
    val data : ByteArray? = null,
    val imageBitmap : ImageBitmap? = null,
    val jsonObject : JSONObject? = null
)