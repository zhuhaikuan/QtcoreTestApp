/*
 * Copyright (C) 2025 Lenovo
 * All Rights Reserved.
 * Lenovo Confidential Restricted.
 */
package com.lenovo.quantum.test

import com.lenovo.quantum.sdk.logging.logD
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

enum class MediaType {
    AUDIO,
    VIDEO,
    IMAGE,
    UNKNOWN
}

class MediaItem {
    var uri: String = ""
        private set
    var fileName: String = ""
        private set
    var type: MediaType = MediaType.UNKNOWN
        private set
    var binary: ByteArray? = null
    private var mimeType: String = ""
    var imageUrl: String = ""
        private set
    var thumbnailUrl: String = ""
        private set
    var contextUrl: String = ""
        private set

    constructor(uri: String) {
        setProperties(uri, fileName, mimeType)
    }

    constructor(uri: String, fileName: String, mimeType: String) {
        setProperties(uri, fileName, mimeType)
    }

    constructor(binary: ByteArray, fileName: String, mimeType: String) {
        setProperties("", fileName, mimeType, binary)
    }

    constructor(path: String, binary: ByteArray, fileName: String, mimeType: String) {
        setProperties(path, fileName, mimeType, binary)
    }

    constructor(imageUrl: String, thumbnailUrl: String, fileName: String, mimeType: String, contextUrl: String = "") {
        setProperties(imageUrl, fileName, mimeType)
        this.imageUrl = imageUrl
        this.thumbnailUrl = thumbnailUrl
        this.contextUrl = contextUrl
    }

    private fun setProperties(p0: String, p1: String, p2: String, p3: ByteArray? = null) {
        uri = p0
        fileName = p1
        mimeType = p2
        binary = p3

        // If not provided, try to extract filename from URI
        if (fileName.isBlank()) {
            fileName = uri.split("/").last()
        }

        // If URI is an URL, adjust the filename, if necessary
        if (uri.startsWith("http")) {
            fileName = URLDecoder.decode(fileName, StandardCharsets.UTF_8.toString())
        }

        // If not provided, try to extract mime type from URI
        if (mimeType.isBlank()) {
            mimeType = uri.substringAfter("data:").substringBefore(";")
        }


        // Get possible file formats from mime types
        val audioFormats = MimeTypes.audio.map { it.split("/").last() }
        val videoFormats = MimeTypes.video.map { it.split("/").last() }
        val imageFormats = MimeTypes.image.map { it.split("/").last() }

        // If provided, get the media format from mime. Otherwise from filename
        val mediaFormat = if (mimeType.isNotBlank()) {
            mimeType.split("/").last()
        } else {
            fileName.split(".").last()
        }

        // If media type is UNKNOWN, there is probably something wrong with the data informed
        type = when(mediaFormat) {
            in audioFormats -> MediaType.AUDIO
            in videoFormats -> MediaType.VIDEO
            in imageFormats -> MediaType.IMAGE
            else -> MediaType.UNKNOWN
        }

        logD { "name: \"$fileName\" - type: $type - mime: \"$mimeType\" - uri: \"$uri\" - binary: ${binary != null}" }
    }
}