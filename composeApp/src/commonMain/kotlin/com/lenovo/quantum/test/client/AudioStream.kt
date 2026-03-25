package com.lenovo.quantum.test.client

data class AudioStream(
    val mimeType : String,
    val data : ByteArray,
    val eos: Boolean,
)
