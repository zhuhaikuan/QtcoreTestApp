/*
 * Copyright (C) 2025 Lenovo
 * All Rights Reserved.
 * Lenovo Confidential Restricted.
 */
package com.lenovo.quantum.test

object MimeTypes {
    val image = listOf(
        "image/jpeg",
        "image/png",
        "image/webp",
        "image/heic",
        "image/heif"
    )

    val document = listOf(
        "application/pdf",
        "application/x-javascript",
        "text/javascript",
        "application/x-python",
        "text/x-python",
        "text/plain",
        "text/html",
        "text/css",
        "text/md",
        "text/csv",
        "text/xml",
        "text/rtf",
        "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
        "application/vnd.openxmlformats-officedocument.presentationml.presentation",
        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
        "application/vnd.ms-powerpoint", //.ppt
        "application/msword", //.doc
        "application/vnd.ms-word", //.doc
        "application/vnd.ms-excel", //.xls
    )

    val audio = listOf(
        "audio/wav",
        "audio/mp3",
        "audio/aiff",
        "audio/aac",
        "audio/ogg",
        "audio/flac",
        "audio/mpeg",
        "audio/l16;rate=24000"
    )

    val video = listOf(
        "video/x-flv",
        "video/quicktime",
        "video/mpeg",
        "video/mpegs",
        "video/mpg",
        "video/mp4",
        "video/webm",
        "video/wmv",
        "video/3gpp",
    )
}