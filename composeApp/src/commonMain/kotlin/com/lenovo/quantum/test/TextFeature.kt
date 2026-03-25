package com.lenovo.quantum.test

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class TextFeature(var id: Long? = null,
                       var text: String? = null,
                       var textEmb: MutableList<Float?>? = null,
                       var document: Document? = null,
                       var bbox: MutableList<MutableList<Float?>?>? = null,
                       var page: Int? = null,
                       var chunkNum: Int? = null,
                       var chunkKeywords: MutableList<String?>? = null,
                       var properties: Map<String, JsonElement>? = null,
                       var initScore: Float? = null,
                       var hybridScore: Float? = null,
                       var parentId: Long? = null,
                       var parentChunkNum: Int? = null,
                       var layer: Long? = null,
                       var tokenNum: Long? = null,
                       var overlap: Int? = null,
                       var pageSize: MutableList<Long?>? = null,
                       var source: String? = null,)
