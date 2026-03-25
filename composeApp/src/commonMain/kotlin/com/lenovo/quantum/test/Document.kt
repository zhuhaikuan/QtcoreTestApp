package com.lenovo.quantum.test

import kotlinx.serialization.Serializable
import java.nio.file.Path
import java.time.LocalDateTime
@Serializable
data class Document(var id: Long? = null,
                    var docName: String? = null,
                    var status: String? = null,
                    var md5: String? = null,
                    var priority: Int = 0,
                    var score: Float = 0f,
                    var errorDescription: String? = null,
                    var errorCode: String? = null,
                    var fileName: String? = null,
                    var labelNameList: MutableList<String?>? = null,
                    var uriPath: String? = null,
                    var tokenNum: Long? = null,
                    var isDeleted: Int? = null,
                    var source: String? = null,
                    var isTmpFile: Boolean? = null,
                    var scope: String? = null,
                    var description: String? = null,
                    var syncId: String? = null,)
