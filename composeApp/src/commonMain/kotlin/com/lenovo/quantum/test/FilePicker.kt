/*
 * Copyright (C) 2025 Lenovo
 * All Rights Reserved.
 * Lenovo Confidential Restricted.
 */
package com.lenovo.quantum.test

import com.lenovo.quantum.test.client.FileAttachment

open class FilePicker {

    open suspend fun launch() : FileAttachment? = null

    open suspend fun launchForList() : List<FileAttachment>? = null

    fun getValidMimeTypes() : List<String> {
        return MimeTypes.image + MimeTypes.document + MimeTypes.audio + MimeTypes.video
    }

    fun shouldConvertToBase64(mimeType : String) : Boolean {
        return mimeType in (MimeTypes.image + MimeTypes.document + MimeTypes.audio + MimeTypes.video)
    }

}