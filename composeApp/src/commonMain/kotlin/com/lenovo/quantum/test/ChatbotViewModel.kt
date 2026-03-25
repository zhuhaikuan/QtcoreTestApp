/*
 * Copyright (C) 2025 Lenovo
 * All Rights Reserved.
 * Lenovo Confidential Restricted.
 */
package com.lenovo.quantum.test

import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lenovo.quantum.sdk.QTCallback
import com.lenovo.quantum.sdk.apibridge.dataV1.BlobData
import com.lenovo.quantum.sdk.apibridge.dataV1.DataContainer
import com.lenovo.quantum.sdk.apibridge.dataV1.InputData
import com.lenovo.quantum.sdk.apibridge.dataV1.OutputData
import com.lenovo.quantum.sdk.apibridge.status.UseCaseResponse
import com.lenovo.quantum.sdk.connection.ConnectionStatus
import com.lenovo.quantum.sdk.getQuantumClient
import com.lenovo.quantum.sdk.logging.logD
import com.lenovo.quantum.sdk.logging.logE
import com.lenovo.quantum.sdk.logging.logI
import com.lenovo.quantum.sdk.logging.logW
import com.lenovo.quantum.test.client.FileAttachment
import com.lenovo.quantum.test.client.AudioStream
import com.lenovo.quantum.test.util.audio.saveRawAudioToWavFile
import com.lenovo.quantum.test.util.getFileSystemCacheDir
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonPrimitive
import kotlin.Boolean
import kotlin.Int
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileWriter
import java.net.URI
import java.time.LocalDateTime

// Document/PkbTool command actions:
const val ADD_ACTION = "add"
const val LIST_ACTION = "list"
const val DELETE_ACTION = "delete"
const val PARSE_ACTION = "parse"
const val COLLECT_ACTION = "collect"
const val STATISTIC_ACTION = "statistic"
const val RE_PARSE_ACTION = "reparse"
const val COLLECT_ALL_ACTION = "collect_all"
const val SYNC_ACTION = "sync"
const val ADD_AND_PARSE_ACTION = "add_and_parse"

const val KBQA_ACTION = "kbqa"
const val IMAGE_SEARCH_ACTION = "image_search"
const val DOCUMENT_SEARCH_ACTION = "document_search"
const val SUMMARY_ACTION = "summary"

// Image search data classes for parsing responses
@Serializable
data class ImageSearchResult(
    val imageUrl: String,
    val thumbnailUrl: String,
    val title: String,
    val contextUrl: String,
    val width: Int,
    val height: Int
)

@Serializable
data class ImageEnhancedResponse(
    val textResponse: String,
    val images: List<ImageSearchResult> = emptyList(),
    val hasImages: Boolean = false
)

@Serializable
data class FkbMemoryRequest(
    val action: String,
    val model: String,
    val superId: String? = null,
    val content: String? = null,
    val shortContent: String? = null,
    val userText: String? = null,
    val bucket: String? = null,
    val url: String? = null,
    val date: String? = null,
    val language: String? = null,
    val saveReason: String? = null,
    val entries: List<Long>? = null,
    val query: String? = null,
    val topK: Int? = null,
    val fields: List<String>? = null
)

@Serializable
data class BlobsyncRequestResponse(
val action: String,
val data: String? = null,
val blobId: String? = null,
val blobType: String? = "fe29e1f2-217e-44e9-9bdf-5b02cfaaf882",
val metaData: String? = null,
val kid: String? = null,
val date: String = Clock.System.now().toString(),
val error: String? = null
)

class ResettableSignal {
    private var channel = Channel<Unit>(Channel.UNLIMITED)

    suspend fun await() {
        channel.receive()
    }

    fun signal() {
        channel.trySend(Unit)
    }

    fun reset() {
        channel.close()
        channel = Channel<Unit>(Channel.UNLIMITED)
    }
}

class ChatbotViewModel(
    private val filePicker : FilePicker,
    private val filePicker2 : FilePicker,
    private val platformContext: Any?
): ViewModel(), QTCallback {
    val qtClient = getQuantumClient()
    private val _messages = mutableStateListOf<Message>()
    val messages = derivedStateOf { _messages }
    var chosenFile : FileAttachment? by mutableStateOf(null)
    var currentJobID by mutableStateOf<Long?>(null)
        private set

    var fkbBatchJsonArray = JSONArray()
    var fkbBatchIndex = 0
    val fkbBatchDelayInMs = 2000L
    var fkbBatchTag = ""

    private var sessionId : String? = null

    val commands = mutableStateListOf<String>()
    var selectedCommand by mutableStateOf("")

    val actions = mutableStateListOf<String>()
    var selectedAction by mutableStateOf("")

    val handlers = mutableStateListOf<String>()
    var selectedHandler by mutableStateOf("")

    val models = mutableStateListOf<String>()
    var selectedModel by mutableStateOf("")

    val modelVersions = mutableStateListOf<String>()
    var selectedModelVersion by mutableStateOf("")

    var isDocumentScreen by mutableStateOf(false)

//    val docLists = mutableListOf<Document>()

    init {
        setupConnection()
    }

    data class MSLanguage(
        val locale: String,
        val voiceLocale: String,
        val localAvailable: Boolean
    )
    val mSLanguages = mutableStateListOf<MSLanguage>()
    var selectedMSLanguage by mutableStateOf("")

    data class MSTTSVoice(
        val name: String,
        val gender: String
    )
    val mSTTSVoices = mutableStateListOf<MSTTSVoice>()
    var selectedMSTTSVoice by mutableStateOf("")
    private var ttsData = mutableMapOf<Long, ByteArray>()
    private var sttFullUpdateCount = 0L
    private val sttSignal = ResettableSignal()

    private var cachedCapabilities : JsonArray? = null

    data class Command(
        val name: String,
        val model: String?,
        val parameter: String?
    )
    private var jobIdToCommand = mutableMapOf<Long, Command>()

    data class CDNCapability(
        val component: String,
        val commands: List<String>,
        val names: List<String>
    )
    val cdnCapabilities = mutableStateListOf<CDNCapability>()
    val cdnCommands = mutableStateListOf<String>()
    val cdnNames = mutableStateListOf<String>()
    var selectedCDNCapabilities by mutableStateOf("")
    var selectedCDNCommand by mutableStateOf("")
    var selectedCDNName by mutableStateOf("")

    init {
        setupConnection()
    }

    var scrollToBottom : () -> Unit = {}

    fun parseDocuments(uris: List<String>) {
        viewModelScope.launch {
            val jsonObject = JSONObject()
            jsonObject.put("action", "add")
            val body = JSONObject()
            val docPaths = JSONArray()
            uris.let { it ->
                docPaths.put(it)
            }
            body.put("doc_paths", docPaths)
            jsonObject.put("body", body)

            qtClient.sendCommand(
                InputData(
                    "document",
                    sessionId = "",
                    data = DataContainer(
                        text = jsonObject.toString(),
                        binary = null
                    )
                )
            )
        }
    }

    fun sendQuery(query: String, imageSearchEnabled: Boolean? = null, imageSearchCount: Int? = null,
                  inputAudioStream: AudioStream? = null) {
        logI(tag = TAG) { "sendQuery::E - $query; command=$selectedCommand - data: ${inputAudioStream?.data?.size}; - action = $selectedAction" }
        // send message
        viewModelScope.launch {

            @Serializable
            data class QueryRequest(
                val handler : String,
                val modelName : String,
                val modelVersion : String,
                val query: String,
                val imageSearchEnabled: Boolean? = null,
                val imageSearchCount: Int? = null
            )

            @Serializable
            data class CDNCapabilitiesRequest(
                val component : String,
                val command : String,
                val name : String,
                val value : String
            )

            val blob = when {
                chosenFile != null -> {
                    chosenFile?.let { it.data?.let { data -> BlobData(it.mimeType, data) } }
                }
                inputAudioStream != null -> {
                    BlobData( inputAudioStream.mimeType, inputAudioStream.data)
                }
                else -> null
            }
            val uri = chosenFile?.uri
            currentJobID = when (selectedCommand) {
                "query" -> {
                    qtClient.sendCommand(
                        InputData(
                            command = "query",
                            sessionId = sessionId ?: "",
                            data = DataContainer(
                                text = Json.encodeToString(
                                    QueryRequest(
                                        handler = selectedHandler,
                                        modelName = "gemini", //selectedModel,
                                        modelVersion = "2.5-flash",
                                        query = query,
                                        //imageSearchEnabled = imageSearchEnabled, // API-level control
                                        //imageSearchCount = imageSearchCount

                                    )
                                ),
                                uri = uri?.let { listOf(it) },
                                binary = blob?.let { listOf(blob) }
                            )
                        )
                    )
                }
                "retrieval" -> {
                    qtClient.sendCommand(
                        InputData(
                            command = "retrieval",
                            sessionId = sessionId ?: "",
                            data = DataContainer(
                                text = JSONObject().put("query", query).toString()
                            )
                        )
                    )
                }
                "capabilities" -> {
                    sendCapabilitiesWithRetry()
                }
                "cdn" -> {
                    qtClient.sendCommand(
                        InputData(
                            "cdn",
                            sessionId = sessionId ?: "",
                            data = DataContainer(
                                text = if (selectedCDNCapabilities.isEmpty() && selectedCDNCommand.isEmpty() &&
                                    selectedCDNName.isEmpty()
                                ) {
                                    null
                                } else {

                                    Json.encodeToString(
                                        CDNCapabilitiesRequest(
                                            component = selectedCDNCapabilities,
                                            command = selectedCDNCommand,
                                            name = selectedCDNName,
                                            value = query
                                        )
                                    )
                                },
                                binary = blob?.let { listOf(blob) }
                            )
                        )
                    )
                }
                "model_call", "text_det", "layout" -> {
                    val inputJSONObj = when (selectedModel) {
                        "mstts" -> {
                            JsonObject(
                                mapOf(
                                    "modelName" to JsonPrimitive(selectedModel),
                                    "modelVersion" to JsonPrimitive(selectedModelVersion),
                                    "prompt" to JsonPrimitive(query),
                                    "locale" to JsonPrimitive(selectedMSLanguage),
                                    "voiceName" to JsonPrimitive(selectedMSTTSVoice)
                                )
                            )
                        }
                        "faiss", "lucenememory" -> {
                            JsonObject(
                                mapOf(
                                    "modelName" to JsonPrimitive(selectedModel),
                                    "modelVersion" to JsonPrimitive(selectedModelVersion),
                                    "userText" to JsonPrimitive(query),
                                    "action" to JsonPrimitive(selectedAction)
                                )
                            )
                        }
                        "msstt" -> {
                            if (inputAudioStream?.eos == true) {
                                val inputData = InputData(
                                    command = "session",
                                    data = DataContainer(
                                        text = Json.encodeToString(
                                            mapOf(
                                                "action" to "finalize",
                                                "sessionID" to sessionId.toString()
                                            )
                                        )
                                    )
                                )
                                sttSignal.reset()
                                val jobId = qtClient.sendCommand(inputData)
                                logD { "eos set for finalize with jobId $jobId, and data: $inputData"}
                                jobIdToCommand[jobId] = Command("session", null, null)
                                sttSignal.await()
                                logD { "eos set for finalize finished waiting"}
                            }

                            JsonObject(
                                mapOf(
                                    "modelName" to JsonPrimitive(selectedModel),
                                    "modelVersion" to JsonPrimitive(selectedModelVersion),
                                    "prompt" to JsonPrimitive(""),
                                    "locale" to JsonPrimitive(selectedMSLanguage)
                                )
                            )
                        }
                        else -> {
                            JsonObject(
                                mapOf(
                                    "modelName" to JsonPrimitive(selectedModel),
                                    "modelVersion" to JsonPrimitive(selectedModelVersion),
                                    "prompt" to JsonPrimitive(query)
                                )
                            )
                        }
                    }
                    qtClient.sendCommand(
                        InputData(
                            command = selectedCommand,
                            sessionId = sessionId ?: "",
                            data = DataContainer(
                                text = Json.encodeToString(
                                    inputJSONObj
                                ),
                                binary = blob?.let { listOf(blob) }
                            )
                        )
                    )
                }
                "fkb_memory" -> {
                    logD { "selected action: $selectedAction" }
                    val fkbJson = when(selectedAction) {
                        "add_memory" -> Json.encodeToString(
                            FkbMemoryRequest(
                                action = "add_memory",
                                model = selectedModel,
                                userText = query,
                                date = LocalDateTime.now().toString()
                            )
                        )
                        "restore_memory" -> {
                            val jsonObject = JSONObject(query)
                            logD { "Retrieved json = $jsonObject" }
                            val contentFromJson = jsonObject.optString("content")
                            val shortContentFromJson = jsonObject.optString("shortContent")
                            val userTextFromJson = jsonObject.optString("userText")
                            val bucketFromJson = jsonObject.optString("shortContent")
                            val saveReasonFromJson = jsonObject.optString("saveReason")
                            val urlFromJson = jsonObject.optString("url")
                            val dateFromJson = jsonObject.optString("date")
                            val languageFromJson = jsonObject.optString("language")

                            Json.encodeToString(
                                FkbMemoryRequest(
                                    action = "restore_memory",
                                    model = selectedModel,
                                    content = contentFromJson,
                                    shortContent = shortContentFromJson,
                                    userText = userTextFromJson,
                                    bucket = bucketFromJson,
                                    saveReason = saveReasonFromJson,
                                    url = urlFromJson,
                                    date = dateFromJson,
                                    language = languageFromJson
                                )
                            )
                        }
                        "get_memory" -> {
                            val jsonObject = JSONObject(query)
                            logD { "Retrieved json = $jsonObject" }
                            val queryFromJson = jsonObject.optString("query")
                            val topKFromJson = jsonObject.optInt("topK")
                            val topK = if (topKFromJson == 0) null else topKFromJson
                            if (queryFromJson.isNotEmpty()) {
                                logD { "get_memory: query=$queryFromJson; topK=$topK" }
                                Json.encodeToString(
                                    FkbMemoryRequest(
                                        action = "get_memory",
                                        model = selectedModel,
                                        query = queryFromJson,
                                        topK = topK
                                    )
                                )
                            } else {
                                val entriesJsonArr = jsonObject.optJSONArray("entries")
                                if (entriesJsonArr != null) {
                                    logD { "get_memory: entries = $entriesJsonArr" }
                                    val list = mutableListOf<Long>()
                                    for (i in 0 until entriesJsonArr.length()) {
                                        val id = entriesJsonArr[i].toString().toLong()
                                        logD { "id = $id" }
                                        list.add(id)
                                    }
                                    Json.encodeToString(
                                        FkbMemoryRequest(
                                            action = "get_memory",
                                            model = selectedModel,
                                            entries = list
                                        )
                                    )
                                } else {}
                            }
                        }
                        "get_all_memory" -> {
                            Json.encodeToString(
                                FkbMemoryRequest(
                                    action = "get_all_memory",
                                    model = selectedModel,
                                    fields = listOf("id", "content", "bucket")
                                )
                            )
                        }
                        "delete_memory" -> {
                            val jsonObject = JSONObject(query)
                            logD { "Retrieved json = $jsonObject" }
                            val entriesJsonArr = jsonObject.optJSONArray("entries")
                            if (entriesJsonArr != null) {
                                logD { "delete_memory: entries = $entriesJsonArr" }
                                val list = mutableListOf<Long>()
                                for (i in 0 until entriesJsonArr.length()) {
                                    val id = entriesJsonArr[i].toString().toLong()
                                    logD { "id = $id" }
                                    list.add(id)
                                }
                                Json.encodeToString(
                                    FkbMemoryRequest(
                                        action = "delete_memory",
                                        model = selectedModel,
                                        entries = list
                                    )
                                )
                            } else {}
                        }
                        "delete_all_memory" -> {
                            Json.encodeToString(
                                FkbMemoryRequest(
                                    action = "delete_all_memory",
                                    model = selectedModel
                                )
                            )
                        }
                        else -> {
                            logE { "Invalid command/action received: $selectedCommand / $selectedAction" }
                        }
                    }
                    logD { "Sending command: $fkbJson" }
                    qtClient.sendCommand(
                        InputData(
                            command = "fkb_memory",
                            sessionId = sessionId ?: "",
                            data = DataContainer(
                                text = fkbJson.toString(),
                                binary = blob?.let { listOf(blob) }
                            )
                        )
                    )
                }
                "document" -> {
                    val jsonObject = JSONObject()
                    jsonObject.put("action", selectedAction)
                    val body = JSONObject()
                    when (selectedAction) {
                        ADD_ACTION -> {
                            val docPaths = JSONArray()
                            if (isDocumentScreen) {
                                chosenFiles?.let { files ->
                                    files.forEach { docPaths.put(it.uri) }
                                }
                            } else {
                                chosenFile?.let { docPaths.put(it.uri) }
                            }

                            body.put("doc_paths", docPaths)
                        }
                        LIST_ACTION -> {
                            body.put("folder_path", query)
                        }
                        DELETE_ACTION -> {
                            val docIds = JSONObject(query).getJSONArray("doc_ids")
                            body.put("doc_ids", docIds)
                        }
                        PARSE_ACTION -> {
                            val docIds = JSONObject(query).getJSONArray("doc_ids")
                            body.put("doc_ids", docIds)
                        }
                        COLLECT_ACTION -> {
                            val jsonInput = JSONObject(query)
                            val docId = jsonInput.getLong("doc_id")
                            val flag = jsonInput.getBoolean("flag")
                            body.put("doc_id", docId)
                            body.put("flag", flag)
                        }
                        RE_PARSE_ACTION -> {
                            // do nothing, just send the re_parse action
                        }
                        COLLECT_ALL_ACTION -> {
                            // do nothing, just send the collect_all action
                        }
                        SYNC_ACTION -> {

                        }
                        ADD_AND_PARSE_ACTION -> {
                            val docPaths = JSONArray()
                            logD { "add_and_parse, uri = $uri" }
                            docPaths.put(uri)
                            body.put("doc_paths", docPaths)
                            logD { "add_and_parse, docPaths = $docPaths" }
                        }
                    }
                    if (body.length() > 0) {
                        logD { "document command: body = $body" }
                        jsonObject.put("body", body)
                    }
                    logD { "document command: sending $jsonObject" }
                    qtClient.sendCommand(
                        InputData(
                            "document",
                            sessionId = sessionId ?: "",
                            data = DataContainer(
                                text = jsonObject.toString(),
                                binary = blob?.let { listOf(blob) }
                            )
                        )
                    )
                }
                "pkbtool" -> {
                    val jsonObject = JSONObject()
                    jsonObject.put("action", selectedAction)
                    val body = JSONObject()
                    when (selectedAction) {
                        KBQA_ACTION -> {
                            val parsedQuery = JSONObject(query).getString("query")
                            body.put("query", parsedQuery)
                            val docIds = JSONObject(query).optJSONArray("doc_ids") ?: JSONArray()
                            body.put("documentIdList", docIds)
                        }
                        IMAGE_SEARCH_ACTION -> {
                            val parsedQuery = JSONObject(query).getString("query")
                            body.put("query", parsedQuery)
                            val imageDescription = JSONObject(query).optString("imageDescription") ?: ""
                            body.put("imageDescription", imageDescription)
                        }
                        DOCUMENT_SEARCH_ACTION -> {
                            val parsedQuery = JSONObject(query).getString("query")
                            body.put("query", parsedQuery)
                            val fileTypes = JSONObject(query).optJSONArray("fileTypes") ?: JSONArray()
                            body.put("fileType", fileTypes)
                        }
                        SUMMARY_ACTION -> {
                            val parsedQuery = JSONObject(query).getString("query")
                            body.put("query", parsedQuery)
                            val docIds = JSONObject(query).optJSONArray("doc_ids") ?: JSONArray()
                            body.put("documentIdList", docIds)
                        }
                    }
                    if (body.length() > 0) {
                        logD { "pkbtool command: body = $body" }
                        jsonObject.put("body", body)
                    }
                    logD { "pkbtool command: sending $jsonObject" }
                    qtClient.sendCommand(
                        InputData(
                            "pkbtool",
                            sessionId = sessionId ?: "",
                            data = DataContainer(
                                text = jsonObject.toString(),
                                binary = blob?.let { listOf(blob) }
                            )
                        )
                    )
                }
                "blobsync" -> {
                    val jsonObject = JSONObject(query)
                    val action = jsonObject.optString("action").toString()
                    val blobData = jsonObject.optString("data").toString().ifBlank { null }
                    val blobId = jsonObject.optString("blobId").toString().ifBlank { null }
                    val blobType = jsonObject.optString("blobType").toString().ifBlank { null }
                    val metaData = jsonObject.optString("metaData").toString().ifBlank { null }
                    val kid = jsonObject.optString("kid").toString().ifBlank { null }
                    val date = jsonObject.optString("date").toString().ifBlank { null }

                    val blobJson = Json.encodeToString(BlobsyncRequestResponse(
                        action.ifEmpty { selectedCommand },
                        blobData,
                        blobId,
                        blobType,
                        metaData,
                        kid,
                        if(date.isNullOrEmpty()) Clock.System.now().toString() else date
                    ))
                    logD { "Sending command: $blobJson" }
                    qtClient.sendCommand(
                        InputData(
                            command = selectedCommand,
                            sessionId = sessionId ?: "",
                            data = DataContainer(
                                text = blobJson
                            )
                        )
                    )
                }

                else -> {
                    qtClient.sendCommand(
                        InputData(
                            command = selectedCommand
                        )
                    )
                }
            }

            // track command for this job
            currentJobID?.let {
                jobIdToCommand[it] = Command(selectedCommand, selectedModel, null)
                logD { "currentJobID = $currentJobID; jobIdToCommand = ${jobIdToCommand[it]?.name}" }
            }
        }
    }

    fun sendMemoryBatchCommand(
        entry: JSONObject
    ) {
        viewModelScope.launch {
            logD { "sendMemoryBatchCommand (E), selectedAction = $selectedAction" }
            val fkbObject = when (selectedAction) {
                "add_memory" -> {
                    FkbMemoryRequest(
                        action = selectedAction,
                        model = selectedModel,
                        userText = entry.optString("userText"),
                        date = entry.optString("date")
                    )
                }
                "get_memory" -> {
                    addMessage(
                        Message(
                            author = AI_AUTHOR,
                            content = "query ${fkbBatchIndex}: ${entry.optString("query")}"
                        )
                    )
                    FkbMemoryRequest(
                        action = selectedAction,
                        model = selectedModel,
                        query = entry.optString("query"),
                        fields = listOf("id", "content", "metric"),
                        topK = entry.optInt("topK")
                    )
                }
                else -> {
                    logE { "Unexpected action received: $selectedAction" }
                    throw Exception("Unexpected action received: $selectedAction")
                }
            }

            logD { "serialize fkbMemoryRequest" }
            val fkbJson = Json.encodeToString(fkbObject)
            logD { "fkbJson: $fkbJson" }

            currentJobID = qtClient.sendCommand(
                InputData(
                    command = selectedCommand,
                    sessionId = sessionId ?: "",
                    data = DataContainer(
                        text = fkbJson
                    )
                )
            )

            // track command for this job
            currentJobID?.let { jobIdToCommand[it] = Command(selectedCommand, selectedModel, null) }
            logD { "sendMemoryBatchCommand (X)" }
        }
    }

    private fun parseCapabilities(capabilityData : String) {
        logD { "parseCapabilities (E)" }
        cachedCapabilities = Json.decodeFromString(JsonArray.serializer(), capabilityData)
        logD { "capabilityData: $capabilityData" }
        for (entry in cachedCapabilities!!) {
            if (entry is JsonObject) {
                val entryName = entry["name"]?.jsonPrimitive?.content
                val entryType = entry["type"]?.jsonPrimitive?.content ?: ""
                // ignore model_call command, will use it by default for models
                if (entryName != null && entryName !in commands && entryType == "command") {
                    commands.add(entryName)
                }
            }
        }

        // set selected if not yet set
        if (selectedCommand.isEmpty() && commands.size > 0) {
            setCommand(commands[0])
        }
        logD { "parseCapabilities (X)" }
    }

    private fun parseLanguageCapabilities(capabilityData : String) {
        logD { "parseCapabilities capabilityData: $capabilityData" }
        val response = Json.decodeFromString(JsonArray.serializer(), capabilityData)
        for (entry in response) {
            if (entry is JsonObject) {
                val locale = entry["locale"]?.jsonPrimitive?.content ?: ""
                val voiceLocale = entry["voiceLocale"]?.jsonPrimitive?.content ?: ""
                val localAvailable = entry["localAvailable"]?.jsonPrimitive?.content.toBoolean()

                if (locale.isNotEmpty() && localAvailable) {
                    val language = MSLanguage(
                        locale = locale,
                        voiceLocale = voiceLocale,
                        localAvailable = true
                    )
                    if (language !in mSLanguages) {
                        mSLanguages.add(language)
                    }
                }
                else {
                    val voicesArray = entry["voices"]?.jsonPrimitive?.content ?: ""
                    val voices = Json.decodeFromString(JsonArray.serializer(), voicesArray)
                    for (voiceEntry in voices) {
                        if (voiceEntry is JsonObject) {
                            val voice = MSTTSVoice(
                                name = voiceEntry["name"]?.jsonPrimitive?.content ?: "",
                                gender = voiceEntry["gender"]?.jsonPrimitive?.content ?: ""
                            )
                            if (voice.name.isNotEmpty() && voice.gender.isNotEmpty()) {
                                mSTTSVoices.add(voice)
                            }
                        }
                    }
                }
            }
        }
    }

    fun queryCapabilities() {
        viewModelScope.launch {
            logD { "queryCapabilities" }
            var jobId = qtClient.sendCommand(InputData("capabilities"))

            if (jobId >= 0) {
                jobIdToCommand[jobId] = Command("capabilities", null, null)
            } else {
                // getNewJobId failed, likely due to race condition when starting/querying service
                // retry again in 3 seconds
                delay(3000)
                jobId = qtClient.sendCommand(InputData("capabilities"))
                if (jobId >= 0) {
                    jobIdToCommand[jobId] = Command("capabilities", null, null)
                }
            }
        }
    }

    /**
     * Sends a capabilities command with infinite retry logic.
     * Will keep attempting to send the command until successful.
     *
     * @return The job ID when the command is successfully sent
     */
    suspend fun sendCapabilitiesWithRetry(): Long {
        logD { "sendCapabilitiesWithRetry" }

        // Retry indefinitely until successful
        while (true) {
            // Attempt to send the capabilities command
            val jobId = qtClient.sendCommand(InputData("capabilities"))

            if (jobId >= 0) {
                // Success: store the command mapping and return the job ID
                jobIdToCommand[jobId] = Command("capabilities", null, null)
                return jobId
            } else {
                // Failed: log warning and retry after delay
                logW { "capabilities command failed, retrying in 3 seconds..." }
                delay(3000)
            }
        }
    }

    fun newSession(shouldDelay : Boolean = false) {
        sttFullUpdateCount = 0
        if (sessionId != null) {
            // delete old session
            viewModelScope.launch {
                val inputData = InputData(
                    command = "session",
                    data = DataContainer(
                        text = Json.encodeToString(
                            mapOf(
                                "action" to "delete",
                                "sessionID" to sessionId.toString()
                            )
                        )
                    )
                )
                qtClient.sendCommand(inputData)
            }
        }

        viewModelScope.launch {
            if (shouldDelay) delay(500) // give time for service to start
            val inputData = InputData(
                command = "session",
                data = DataContainer(
                    text = Json.encodeToString(
                        mapOf("action" to "create")
                    )
                )
            )
            val jobId = qtClient.sendCommand(inputData)
            jobIdToCommand[jobId] = Command("session", null, null)
        }
    }

    fun prettyPrintJson(input: String): String {
        val json = Json { prettyPrint = true }
        val element = json.parseToJsonElement(input)
        return json.encodeToString(JsonElement.serializer(), element)
    }

    private fun getActionsForCommand(command : String) : List<String> {
        logD { "getActionsForCommand: (E)" }
        val result = mutableListOf<String>()
        logD { "command: $command" }
        val capEntry = cachedCapabilities?.find { entry ->
            (entry as? JsonObject)?.get("name")?.jsonPrimitive?.content == command
        }
        capEntry?.let { entry ->
            val actionsEntry = (entry as? JsonObject)?.get("actions") as? JsonArray
            actionsEntry?.forEach { actionEntry ->
                (actionEntry as? JsonElement)?.jsonPrimitive?.content?.let {
                    logD { "action: $it" }
                    result.add(it)
                }
            }
        }
        logD { "getActionsForCommand: (X)" }
        return result
    }

    private fun getHandlersForCommand(command : String) : List<String> {
        val result = mutableListOf<String>()

        val capEntry = cachedCapabilities?.find { entry ->
            (entry as? JsonObject)?.get("name")?.jsonPrimitive?.content == command
        }
        capEntry?.let { entry ->
            val handlersEntry = (entry as? JsonObject)?.get("handlers") as? JsonArray
            handlersEntry?.forEach { handlerEntry ->
                (handlerEntry as? JsonElement)?.jsonPrimitive?.content?.let {
                    result.add(it)
                }
            }
        }

        return result
    }

    private fun getModelsForCommand(command : String) : List<String> {
        val result = mutableListOf<String>()

        val capEntry = cachedCapabilities?.find { entry ->
            (entry as? JsonObject)?.get("name")?.jsonPrimitive?.content == command
        }
        capEntry?.let { entry ->
            val modelsEntry = (entry as? JsonObject)?.get("models") as? JsonArray
            modelsEntry?.forEach { modelEntry ->
                (modelEntry as? JsonElement)?.jsonPrimitive?.content?.let {
                    result.add(it)
                }
            }
        }

        return result
    }

    private fun getVersionsForModel(model : String) : List<String> {
        val result = mutableListOf<String>()

        val modelEntry = cachedCapabilities?.find { entry ->
            (entry as? JsonObject)?.get("name")?.jsonPrimitive?.content == model
        }
        modelEntry?.let { entry ->
            val versionsEntry = (entry as? JsonObject)?.get("versions") as? JsonArray
            versionsEntry?.forEach { version ->
                (version as? JsonElement)?.jsonPrimitive?.content?.let {
                    result.add(it)
                }
            }
        }

        return result
    }

    fun setCommand(command: String) {
        if (command == selectedCommand) return

        selectedCommand = command
        _messages.clear()

        // list actions for command
        actions.clear()
        actions.addAll(getActionsForCommand(command))
        selectedAction = if (actions.isNotEmpty()) {
            actions[0]
        } else {
            ""
        }

        // list handlers for command
        handlers.clear()
        handlers.addAll(getHandlersForCommand(command))
        selectedHandler = if (handlers.isNotEmpty()) {
            handlers[0]
        } else {
            ""
        }

        // update list of available models for this command
        models.clear()
        if (selectedCommand != "fkb_memory")
            models.add("default")
        if (command != "cdn") {
            models.add("default")
            cdnCapabilities.clear()
            cdnCommands.clear()
            cdnNames.clear()
            selectedCDNCapabilities = ""
            selectedCDNCommand = ""
            selectedCDNName = ""
        }
        models.addAll(getModelsForCommand(command))
        selectedModel = if (models.isNotEmpty()) {
            models[0]
        } else {
            ""
        }

        // model versions
        refreshModelVersionList()
    }

    fun setAction(action : String) {
        selectedAction = action
    }

    fun setHandler(handler : String) {
        selectedHandler = handler
    }

    fun setModel(model : String) {
        selectedModel = model
        refreshModelVersionList()
        mSLanguages.clear()
        selectedMSLanguage = ""
        mSTTSVoices.clear()
        selectedMSTTSVoice = ""
        actions.clear()
        if (model == "mstts" || model == "msstt") {
            loadMSCapabilities(model, "languages")
        }
        else if (model in listOf("faiss_all-miniLM", "faiss_AAITC-Emb", "lucene_all-miniLM", "lucene_AAITC-Emb", "lucene_all-miniLM_hybrid", "lucene_AAITC-Emb_hybrid")) {
            // list actions for command
            actions.addAll(getActionsForCommand(selectedCommand))
            selectedAction = if (actions.isNotEmpty()) {
                actions[0]
            } else {
                ""
            }
        }
    }

    fun setModelVersion(version : String) {
        selectedModelVersion = version
    }

    private fun refreshModelVersionList() {
        modelVersions.clear()
        modelVersions.addAll(getVersionsForModel(selectedModel)) // will be empty for "default"
        selectedModelVersion = if (modelVersions.isNotEmpty()) {
            modelVersions[0]
        } else {
            ""
        }
    }

    private fun loadMSCapabilities(model: String, capabilityName: String) {
        logD { "loadMSCapabilities - capability: $capabilityName" }
        viewModelScope.launch {
            @Serializable
            data class CapabilitiesRequest(
                val modelName: String,
                val capability : String,
                val locale: String,
                val voiceLocale: String
            )
            val text =
                if (capabilityName == "voices") {
                    val language = mSLanguages.find { it.voiceLocale == selectedMSLanguage }
                    if (language != null) {
                        CapabilitiesRequest(
                            modelName = model,
                            capability = capabilityName,
                            locale = language.locale,
                            voiceLocale = language.voiceLocale
                        )
                    }
                    else {
                        null
                    }
                }
                else {
                    CapabilitiesRequest(
                        modelName = model,
                        capability = capabilityName,
                        locale = "",
                        voiceLocale = ""
                    )
                }

            if (text != null) {
                val input = InputData(
                    "model_call",
                    sessionId = sessionId ?: "",
                    data = DataContainer(
                        text = Json.encodeToString(text)
                    )
                )
                currentJobID = qtClient.sendCommand(input)
            }
            else {
                logE { "Not possible to query MSTTSCapabilities. Invalid input data" }
            }
            logD { "currentJobID: $currentJobID selectedCommand:$selectedCommand" }

            currentJobID?.let { jobIdToCommand[it] = Command(selectedCommand, selectedModel, capabilityName) }
        }
    }

    fun setMSLanguage(language: String) {
        logD { "setMSLanguage $language selectedModel: $selectedModel" }
        selectedMSLanguage = language
        mSTTSVoices.clear()
        selectedMSTTSVoice = ""
        if (selectedModel == "mstts" && language.isNotEmpty()) {
            loadMSCapabilities("mstts", "voices")
        }
    }

    fun setMSTTSVoice(voice: String) {
        logD { "setMSTTSVoice $voice" }
        selectedMSTTSVoice = voice
    }

    fun setCDNCapability(capability: String) {
        selectedCDNCapabilities = capability

        cdnCommands.clear()
        cdnCommands.addAll(cdnCapabilities.filter { it.component == capability }.flatMap { it.commands })
        selectedCDNCommand = if (cdnCommands.isNotEmpty()) {
            cdnCommands[0]
        } else {
            ""
        }

        cdnNames.clear()
        cdnNames.addAll(cdnCapabilities.filter { it.component == capability }.flatMap { it.names})
        selectedCDNName = if (cdnNames.isNotEmpty()) {
            cdnNames[0]
        } else {
            ""
        }
    }

    fun setCDNCommand(command: String) {
        selectedCDNCommand = command
        if (command == "list") {
            selectedCDNName = ""
            sendQuery("")
        }
    }

    fun setCDNName(name: String) {
        selectedCDNName = name
    }

    fun abort() {
        viewModelScope.launch {
            currentJobID?.let {
                qtClient.abort(it)
                currentJobID = null
            }
        }
    }

    private fun chatHistoryToString() : String {
        return _messages.joinToString { "${it.author}: ${it.content}\n\n" }
    }

    fun addMessage(msg: Message) {
        _messages.add(msg) // Add to the beginning of the list
    }

    private fun addOrUpdateMessage(jobId : Long, newContent: String, compact : Boolean) {
        val messageIndex = _messages.indexOfFirst { it.jobId == jobId }
        if (messageIndex >= 0) {
            _messages[messageIndex] = _messages[messageIndex].copy(content = newContent)
        } else {
            // don't update compact messages, create new message instead
            val messageJobId = if (compact) null else jobId
            addMessage(
                Message(
                    author = AI_AUTHOR,
                    content = newContent,
                    compact = compact,
                    jobId = messageJobId
                )
            )
        }

        scrollToBottom()
    }

    private fun setupConnection() {
        qtClient.connectService(this, platformContext)
    }

    fun clearMessages() {
        _messages.clear()
    }

    fun pickFile() {
        CoroutineScope(Dispatchers.Main).launch {
            chosenFile = filePicker.launch()

            if (selectedCommand == "fkb_memory") {

                fkbBatchTag = when(selectedAction) {
                    "add_memory" -> "entries"
                    "get_memory" -> "queries"
                    else -> ""
                }
                if (fkbBatchTag.isNotEmpty()) {
                    val jsonObject = chosenFile?.jsonObject
                    if (jsonObject != null) {
                        fkbBatchJsonArray = jsonObject.optJSONArray(fkbBatchTag) ?: JSONArray()
                        fkbBatchIndex = 0
                    }
                }
            }
        }
    }

    var chosenFiles : List<FileAttachment>? by mutableStateOf(null)
    fun pickFile2() {
        CoroutineScope(Dispatchers.Main).launch {
            chosenFiles = filePicker2.launchForList()

            if (selectedCommand == "fkb_memory") {

                fkbBatchTag = when(selectedAction) {
                    "add_memory" -> "entries"
                    "get_memory" -> "queries"
                    else -> ""
                }
                if (fkbBatchTag.isNotEmpty()) {
                    val jsonArray = JSONArray()
                    chosenFiles?.forEach { jsonArray.put(it.jsonObject) }
                    if (jsonArray.length() > 0) {
                        val jsonObject = JSONObject()
                        fkbBatchJsonArray = jsonObject.optJSONArray(fkbBatchTag) ?: JSONArray()
                        fkbBatchIndex = 0
                    }
                }
            }
        }
    }

    //FIXME: figure out how to handle engine.close()

    companion object {
        private val TAG : String = ChatbotViewModel::class.simpleName.toString()
        private val AI_AUTHOR = "ai"
    }

    override fun onConnectionStatus(status: ConnectionStatus) {
        logD { "received connection callback $status" }
        when (status) {
            ConnectionStatus.UNKNOWN -> {
                logW { "Connection status is unknown, will retry connection" }
                // Optionally retry connection or show user feedback
                viewModelScope.launch {
                    delay(2000)
                    setupConnection()
                }
            }
            ConnectionStatus.CONNECTED -> {
                newSession(false)
                queryCapabilities()
            }
            ConnectionStatus.DISCONNECTED -> {
                logI { "Service disconnected" }
                // Could add user notification here if needed
            }
            ConnectionStatus.ERROR -> {
                logE { "Connection error occurred, attempting to reconnect" }
                viewModelScope.launch {
                    // Add error message to chat
                    addMessage(
                        Message(
                            author = AI_AUTHOR,
                            content = "Connection error occurred. Retrying...",
                            compact = true
                        )
                    )
                    delay(3000)
                    clearMessages()
                    setupConnection()
                }
            }
            ConnectionStatus.DIED -> {
                viewModelScope.launch {
                    logE { "received unexpected server death, try to connect again" }
                    delay(1000)
                    clearMessages()
                    setupConnection()
                }
            }

            else -> {}
        }
    }

    fun addSamples(data: AudioStream) {
        runBlocking {
            logD { "add samples ${data.data.size}, with eos ${data.eos} and mime ${data.mimeType}" }
            //logD { "audio: ${data.data.contentToString()}" }
            sendQuery("", inputAudioStream = data)
            /*if (dataInputChannel.isClosedForSend) {
                logE { "Session is closed, shouldn't send this!!!!" }
                return@runBlocking
            }
            dataInputChannel.send(data)
            if (data.eos) {
                dataInputChannel.close()
            }*/
        }
    }

    var retrievalTimes = 0
    private suspend fun saveResultToFile(result: String) =
        withContext(Dispatchers.IO) {
            logD {"###### save retrieval result start!"}
            retrievalTimes++
            val tempFile = File(getFileSystemCacheDir(), "${System.currentTimeMillis()}-retrieval_result.txt")
            tempFile.createNewFile()
            val fileWriter = FileWriter(tempFile)
            fileWriter.write("Retrieval-$retrievalTimes:\n\n $result \n")
            fileWriter.close()
            logD {"###### save retrieval result succeed!"}
        }

    val queries = listOf("Did Kendra miss the bus?")
    var i = 0
    var startTime = System.currentTimeMillis()
    override fun onResult(result: OutputData) {
        viewModelScope.launch {
            logD { "got result: $result" }

            @Serializable
            data class QueryResponse(
                val response: String,
                val compact: Boolean = false,
            )

            @Serializable
            data class STTFrameData(
                val frameStartTime: Long,
                val text: String?,
                val frameEndTime: Long
            )

            @Serializable
            data class SpeechToTextResponseData(
                val fullText: List<STTFrameData>? = null,
                val partialText: List<STTFrameData>? = null,
            )

            @Serializable
            data class Document(
                val fileName: String = "",
                val status: String = "",
            )

            val command = jobIdToCommand[result.jobId]
            if (command == null) {
                logE { "no command found for job ${result.jobId}" }
            } else if (result.status in listOf(UseCaseResponse.COMPLETE, UseCaseResponse.FAILED)) {
                jobIdToCommand.remove(result.jobId)
                currentJobID = null
            }
            if (command != null) {
                when (command.name) {
                    "retrieval" -> {
                        // 添加你那对result的处理逻辑，然后再发请求
                        logE {"###### got retrieval result, cost times = ${(System.currentTimeMillis() - startTime)} milliseconds!"}

                        val featureInfo: StringBuilder = StringBuilder()
                        featureInfo.append("ChunkNum ----- FileName ----- SheetName ----- text\n\n")
                        val json = Json{
                            ignoreUnknownKeys = true  // 忽略未知字段
                            isLenient = true
                        }
                        result.data?.text?.let {
                            val jsonArray = json.decodeFromString<List<TextFeature>>(it)
                            jsonArray.forEach { feature ->
                                featureInfo.append(feature.chunkNum.toString() + " - " + feature.document?.fileName + " - " + feature.properties + " - " + feature.text + "\n\n")
                            }
                        }
                        featureInfo.toString()

                        val message = featureInfo.toString()//?: "have no result, use this default value"
                        addMessage(
                            Message(
                                author = AI_AUTHOR,
                                content = message
                            )
                        )
                        viewModelScope.launch {
                            saveResultToFile(result.data?.text?:"")
                        }
//                        if (i < queries.size) {
//                            retrieval(queries[i])
//                            i++
//                            startTime = System.currentTimeMillis()
//                        }
                    }
                    "document", "pkbtool" -> {
                        // clean inputted FileAttachment
                        var isParseEnd = true
                        chosenFile = null
                        val message = if (selectedCommand == command.name && result.status == UseCaseResponse.COMPLETE) {
                            if (selectedAction == ADD_ACTION) {
                                selectedCommand = "document"
                                selectedAction = LIST_ACTION
                                getAllDocuments()
                                result.data?.text ?: "Action completed"
                            } else if (selectedAction == LIST_ACTION) {
                                val docInfo: StringBuilder = StringBuilder()
                                docInfo.append("FileName  -------------------------------  Status\n\n")
                                val json = Json{
                                    ignoreUnknownKeys = true  // 忽略未知字段
                                    isLenient = true
                                }
                                result.data?.text?.let {
                                    var addedNum = 0
                                    var completedNum = 0
                                    var failedNum = 0
                                    var deletedNum = 0
                                    var runningNum = 0
                                    val jsonArray = json.decodeFromString<List<Document>>(it)
                                    jsonArray.forEach { doc ->
                                        when (doc.status) {
                                            "ADDED" -> { addedNum++ }
                                            "DELETED" -> { deletedNum++ }
                                            "FAILED" -> { failedNum++ }
                                            "COMPLETED" -> { completedNum++ }
                                            "RUNNING" -> runningNum = 1
                                        }
                                    }
                                    isParseEnd = !(addedNum > 0 || runningNum == 1)
                                    docInfo.append("$addedNum added docs --- $completedNum completed docs --- $failedNum failed docs --- $deletedNum deleted docs \n\n")
                                }

                                if (!isParseEnd) {
                                    getAllDocuments()
                                }

                                docInfo.toString()
                            } else {
                                result.data?.text ?: "Action completed"
                            }
                        } else {
                            result.data?.text ?: result.status
                        }
                        addMessage(
                            Message(
                                author = AI_AUTHOR,
                                content = message
                            )
                        )
                    }
                    "capabilities" -> {
                        result.data?.text?.let { parseCapabilities(it) }
                        if (selectedCommand == command.name && result.status == UseCaseResponse.COMPLETE) {
                            // only show if we're currently on the capabilities tab
                            val resultParsed = QueryResponse(result.data?.text ?: "", false)
                            val message = prettyPrintJson(resultParsed.response)
                            addMessage(
                                Message(
                                    author = AI_AUTHOR,
                                    content = message,
                                    compact = resultParsed.compact
                                )
                            )
                        }
                    }

                    "session" -> {
                        val responseJSON = result.data?.text?.let {
                            Json.decodeFromString(JsonObject.serializer(), it)
                        }

                        responseJSON?.let {
                            sessionId = it["sessionID"]?.jsonPrimitive?.content
                            logD { "Received session ID: $sessionId" }
                        }
                        sttSignal.signal()
                    }

                    "ms_tts_capabilities" -> {
                        result.data?.text?.let { parseLanguageCapabilities(it) }
                    }

                    "fkb_memory" -> {
                        result.data?.text?.let { parseFkbMemory(it) }
                        if (fkbBatchJsonArray.length() > 0) {
                            fkbBatchIndex++
                            if (fkbBatchIndex < fkbBatchJsonArray.length()) {
                                Thread.sleep(fkbBatchDelayInMs)
                                sendMemoryBatchCommand(
                                    entry = fkbBatchJsonArray.getJSONObject(fkbBatchIndex)
                                )
                            } else {
                                fkbBatchJsonArray = JSONArray()
                                chosenFile = null
                            }
                        }
                    }

                    "cdn" -> {
                        if (selectedCDNCapabilities.isEmpty() &&  selectedCDNCommand.isEmpty() &&
                            selectedCDNName.isEmpty()) {
                            @Serializable
                            data class CDNName(
                                val name: String,
                                val type: String,
                                val default: String
                            )
                            @Serializable
                            data class ResponseCapability(
                                val component : String,
                                val command : List<String>,
                                val names : List<CDNName>
                            )
                            result.data?.text?.let { resultText ->
                                val receivedCapabilities: List<ResponseCapability> = try {
                                    Json.decodeFromString<List<ResponseCapability>>(resultText)
                                } catch (e: Exception) {
                                    logE { "unable to parse response as QueryResponse: $e" }
                                    emptyList()
                                }
                                receivedCapabilities.forEach { capability ->
                                    cdnCapabilities.clear()
                                    val responseCapability = CDNCapability (
                                        component = capability.component,
                                        commands = capability.command,
                                        names = capability.names.map { it.name }
                                    )
                                    cdnCapabilities.add(responseCapability)
                                }
                                //Parse CDN Capabilities
                                println("result: $cdnCapabilities")

                                cdnCommands.clear()
                                cdnCapabilities.first().commands.forEach { cdnCommands.add(it) }

                                cdnNames.clear()
                                cdnCapabilities.first().names.forEach { cdnNames.add(it) }
                            }
                        }

                        val resultParsed = QueryResponse(result.data?.text ?: "", false)
                        val message = if (selectedCDNCommand == "list") {
                            prettyPrintJson(Json.decodeFromString(resultParsed.response))
                        }
                        else {
                            prettyPrintJson(resultParsed.response)
                        }
                        addMessage(
                            Message(
                                author = AI_AUTHOR,
                                content = message,
                                compact = resultParsed.compact
                            )
                        )
                    }

                    else -> {
                        when {
                            command.parameter == "voices" || command.parameter == "languages" -> {
                                logD { "MS capabilities received" }
                                result.data?.text?.let { parseLanguageCapabilities(it) }
                            }
                            command.model == "msstt" -> {
                                when (result.status) {
                                    UseCaseResponse.COMPLETE -> {
                                        try {
                                            val json = Json { ignoreUnknownKeys = true }
                                            val textResponse: SpeechToTextResponseData =
                                                json.decodeFromString(result.data?.text ?: "{}")
                                            val message = textResponse.fullText?.joinToString {
                                                it.text ?: ""
                                            } ?: "".ifEmpty {
                                                textResponse.partialText?.joinToString {
                                                    it.text ?: ""
                                                } ?: ""
                                            }
                                            if (message.isNotEmpty()) {
                                                addOrUpdateMessage(
                                                    sttFullUpdateCount,
                                                    message,
                                                    false
                                                )
                                            }
                                            if (textResponse.fullText != null) sttFullUpdateCount++
                                        } catch (e: Exception) {
                                            result.data?.text?.let { resultText ->
                                                logW { "unable to parse MS STT response as JSON object $e" }
                                            }
                                        }
                                    }
                                }
                            }
                            else -> {
                                result.data?.text.orEmpty().let { resultText ->
                                    // First try to parse as QueryResponse (standard format)
                                    //FIXME: this shouldn't always expect to have a QueryResponse...
                                    val queryResponse: QueryResponse = try {
                                        Json.decodeFromString<QueryResponse>(resultText)
                                    } catch (e: Exception) {
                                        logE { "unable to parse response as QueryResponse: $e" }
                                        QueryResponse(resultText, false)
                                    }

                                    // Clean the response if it contains structural headers
                                    val cleanedResponse = cleanResponseHeaders(queryResponse.response)

                                    // Now try to parse the cleaned response as ImageEnhancedResponse
                                    val imageEnhancedResponse = try {
                                        Json.decodeFromString<ImageEnhancedResponse>(cleanedResponse)
                                    } catch (e: Exception) {
                                        logD {"Response is not in ImageEnhancedResponse format, treating as plain text" }
                                        null
                                    }

                                    if (imageEnhancedResponse != null && imageEnhancedResponse.hasImages) {
                                        // Handle image-enhanced response
                                        logD { "Processing image-enhanced response with ${imageEnhancedResponse.images.size} images" }

                                        // Add the text response
                                        addOrUpdateMessage(
                                            result.jobId,
                                            imageEnhancedResponse.textResponse,
                                            false
                                        )

                                        // Add images as separate messages
                                        imageEnhancedResponse.images.take(3)
                                            .forEach { imageResult ->
                                                logD { "Adding image: ${imageResult.title} - URL: ${imageResult.thumbnailUrl}" }
                                                addMessage(
                                                    Message(
                                                        author = AI_AUTHOR,
                                                        content = "",
                                                        compact = true,
                                                        mediaItem = MediaItem(
                                                            imageUrl = imageResult.thumbnailUrl, // Use thumbnail for better performance
                                                            thumbnailUrl = imageResult.thumbnailUrl,
                                                            fileName = imageResult.title,
                                                            mimeType = "image/jpeg",
                                                            contextUrl = imageResult.contextUrl
                                                        )
                                                    )
                                                )
                                            }
                                    } else {
                                        // Handle regular text response - use cleaned response
                                        addOrUpdateMessage(
                                            result.jobId,
                                            cleanedResponse,
                                            queryResponse.compact
                                        )
                                    }
                                    result.data?.binary.orEmpty().forEach { blob ->
                                        if (blob.mime.contains("audio")) {
                                            if (true) {
                                                val filePath = withContext(Dispatchers.IO) {
                                                    return@withContext createAudioFile(
                                                        result.jobId,
                                                        blob.data,
                                                        result.status == UseCaseResponse.IN_PROGRESS
                                                    )
                                                }
                                                if (filePath.isNotEmpty() && result.status == UseCaseResponse.COMPLETE) {
                                                    addMessage(
                                                        Message(
                                                            jobId = result.jobId,
                                                            author = AI_AUTHOR,
                                                            content = "",
                                                            compact = true,
                                                            mediaItem = MediaItem(
                                                                path = filePath,
                                                                binary = blob.data,
                                                                fileName = filePath.split("/")
                                                                    .last(),
                                                                mimeType = blob.mime
                                                            )
                                                        )
                                                    )
                                                } else {
                                                    val numBytes = ttsData[result.jobId]?.size ?: -1
                                                    val existingMessage = _messages.find { it.jobId == result.jobId && it.author == AI_AUTHOR }
                                                    if (existingMessage != null) {
                                                        if (numBytes % (1600 * 10) == 0) { // every 10 chunks, otherwise compose starts flickering
                                                            existingMessage.content =
                                                                "Received $numBytes bytes..."
                                                        }
                                                    } else {
                                                        addMessage(
                                                            Message(
                                                                jobId = result.jobId,
                                                                author = AI_AUTHOR,
                                                                content = "Received $numBytes bytes...",
                                                                compact = true
                                                            )
                                                        )
                                                    }
                                                }

                                            }
                                        } else {
                                            addMessage(
                                                Message (
                                                    author = AI_AUTHOR,
                                                    content = "",
                                                    compact = true,
                                                    mediaItem =  MediaItem(
                                                        binary = blob.data,
                                                        fileName = "image.${blob.mime.split("/").last()}",
                                                        mimeType = blob.mime
                                                    )
                                                )
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun parseFkbMemory(resultText: String) {
        @Serializable
        data class FkbMemoryResponse(
            val action: String,
            val entries: String? = null
        )
        val fkbMemoryResponse: FkbMemoryResponse? = try {
            Json.decodeFromString<FkbMemoryResponse>(resultText)
        } catch (e: Exception) {
            logE {"unable to parse response as FkbMemoryResponse: $e" }
            null
        }
        if (fkbMemoryResponse != null) {
            addMessage(
                Message(
                    author = AI_AUTHOR,
                    content = "FkbMemoryRsp: action = ${fkbMemoryResponse.action}; entries = ${fkbMemoryResponse.entries}"
                )
            )
        }
    }

    private suspend fun createAudioFile(
        jobId : Long,
        audioContent: ByteArray,
        isDelta : Boolean
    ) : String {
        val tempDir = getFileSystemCacheDir()
        val outputFilePath = "${tempDir}mstts_output-${Clock.System.now().toEpochMilliseconds()}.wav"

        val data = if (isDelta) {
            (ttsData[jobId] ?: ByteArray(0)) + audioContent.copyOfRange(0, audioContent.size)
        } else {
            audioContent
        }
        ttsData[jobId] = data

        logD {
            "New data size: ${audioContent.size}, total data size: ${data.size}"
        }

        logD { "Attempting to save output WAV to: $outputFilePath" }
        val success = saveRawAudioToWavFile(data, outputFilePath)

        if (success) {
            logD { "Output WAV file created successfully: $outputFilePath" }

            return outputFilePath
        } else {
            logD {"Failed to create Output WAV file."}
            return ""
        }
    }

    private fun cleanResponseHeaders(response: String): String {
        if (response.isBlank()) return response

        // Remove structural headers that might leak through from orchestration
        var cleaned = response.trim()

        // Remove ## RESPONSE and ##RESPONSE headers
        cleaned = cleaned.removePrefix("## RESPONSE").removePrefix("##RESPONSE").trim()

        // Remove ## MEMORIES and ##MEMORIES headers and everything after them
        val memoryPatterns = listOf("## MEMORIES", "##MEMORIES")
        for (pattern in memoryPatterns) {
            val memoryIndex = cleaned.indexOf(pattern)
            if (memoryIndex != -1) {
                cleaned = cleaned.substring(0, memoryIndex).trim()
                break
            }
        }

        // Remove ## THOUGHT and ##THOUGHT headers (shouldn't be there but just in case)
        cleaned = cleaned.removePrefix("## THOUGHT").removePrefix("##THOUGHT").trim()

        return cleaned
    }

    fun getAllDocuments() {
        val jsonObject = JSONObject()
        jsonObject.put("action", LIST_ACTION)
        val body = JSONObject()
        body.put("folder_path", "")
        if (body.length() > 0) {
            jsonObject.put("body", body)
        }

        viewModelScope.launch {
            delay(30000)
            val jobId = qtClient.sendCommand(
                InputData(
                    "document",
                    sessionId = sessionId ?: "",
                    data = DataContainer(
                        text = jsonObject.toString(),
                        binary = null
                    )
                )
            )
            jobIdToCommand[jobId] = Command("document", null, null)
        }
    }

    fun retrieval(query: String) {
        viewModelScope.launch {
            logE {"###### retrieval starting..., the query is $query"}
            startTime = System.currentTimeMillis()
            val jobId = qtClient.sendCommand(
                InputData(
                    command = "retrieval",
                    sessionId = sessionId ?: "",
                    data = DataContainer(
                        text = JSONObject().put("query", query).toString()
                    )
                )
            )
            logE {"###### jobId = $jobId"}
            jobIdToCommand[jobId] = Command("retrieval", null, null)
        }
    }
}

private fun CoroutineScope.launch(block: suspend (CoroutineScope) -> Unit) {}
