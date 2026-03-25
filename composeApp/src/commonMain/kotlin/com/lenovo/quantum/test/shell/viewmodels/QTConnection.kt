package com.lenovo.quantum.test.shell.viewmodels

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
import com.lenovo.quantum.test.client.FileAttachment
import com.lenovo.quantum.test.shell.models.ChatMessage
import com.lenovo.quantum.test.shell.models.ToolInfo
import com.lenovo.quantum.test.shell.models.ToolStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonPrimitive
import java.lang.Thread.sleep

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
data class SessionListResponse(
    val sessionID : String,
    val sessionTitle : String,
)

@Serializable
data class AgentResponse(
    val type : String,
    val response : String,
    val compact : Boolean
)

class QTConnection(
    private val platformContext : Any?,
    private val onConnect : () -> Unit,
    private val onGetSessions : (List<SessionListResponse>) -> Unit,
    private val onMessageUpdate : (Long, String, List<ToolInfo>) -> Unit,
) : QTCallback {

    val ioScope = CoroutineScope(Dispatchers.IO)

    val qtClient = getQuantumClient()

    val callbacks = mutableMapOf<Long, (OutputData) -> Unit>()
    var isConnected : Boolean = false

    init {
        qtClient.connectService(this, platformContext)
    }

    fun getSystemInstructions(onReceive : (String) -> Unit) {
        sendCommand(
            InputData(
                command = "tools",
                data = DataContainer(Json.encodeToString(
                    JsonObject(
                        mapOf(
                            "action" to JsonPrimitive("get_sys_instructions"),
                        )
                    )
                ))
            )
        ) { outputData ->
            if (outputData.status == UseCaseResponse.COMPLETE) {
                val instructions = outputData.data?.text ?: ""
                logD { "Loaded sys instructions: $instructions" }
                onReceive(instructions)
            }
        }
    }

    fun saveSystemInstructions(sysInstructions : String) {
        sendCommand(
            InputData(
                command = "tools",
                data = DataContainer(Json.encodeToString(
                    JsonObject(
                        mapOf(
                            "action" to JsonPrimitive("update_sys_instructions"),
                            "systemInstructions" to JsonPrimitive(sysInstructions)
                        )
                    )
                ))
            )
        ) { outputData ->

        }
    }

    fun getSessions(retryCount : Int = 0) {
        if (retryCount == 500) {
            return
        }

        sendCommand(
            InputData(
                command = "session",
                data = DataContainer(Json.encodeToString(
                    JsonObject(
                        mapOf(
                            "action" to JsonPrimitive("list")
                        )
                    )
                ), null)
            )
        ) { outputData ->
            if (outputData.status == UseCaseResponse.FAILED) {
                ioScope.launch {
                    sleep(500)
                    scheduleReconnect()
                }
            } else if (outputData.status == UseCaseResponse.COMPLETE) {
                try {
                    val text = outputData.data?.text ?: "[]"
                    val sessionInfo = Json.decodeFromString<List<SessionListResponse>>(text)
                    onGetSessions(sessionInfo)
                } catch (e : Exception) {
                    logE { "Session refresh successful, but invalid data returned:\n${e.stackTraceToString()}" }
                }
            }
        }
    }

    fun createSession(callback : (String) -> Unit) {
        sendCommand(
            InputData(
                command = "session",
                data = DataContainer(Json.encodeToString(
                    JsonObject(
                        mapOf(
                            "action" to JsonPrimitive("create")
                        )
                    )
                ), null)
            )
        ) { outputData ->
            if (outputData.status == UseCaseResponse.COMPLETE) {
                try {
                    val text = outputData.data?.text ?: "{}"
                    logD { "Created session: $text" }
                    val sessionInfo = Json.decodeFromString<JsonObject>(text)
                    sessionInfo["sessionID"]?.let { callback(it.jsonPrimitive.content) }
                } catch (e : Exception) {
                    logE { "Session created successfully, but invalid data returned:\n${e.stackTraceToString()}" }
                }
            }
        }
    }

    fun refreshTools(callback : (String) -> Unit) {
        sendCommand(
            InputData(
                command = "tools",
                data = DataContainer(Json.encodeToString(
                    JsonObject(
                        mapOf(
                        )
                    )
                ), null)
            )
        ) { outputData ->
            if (outputData.status == UseCaseResponse.COMPLETE) {
                callback(outputData.data?.text ?: "[]")
            }
        }
    }

    fun getMessages(sessionId : String, callback : (List<ChatMessage>) -> Unit) {
        sendCommand(
            InputData(
                command = "session",
                data = DataContainer(Json.encodeToString(
                    JsonObject(
                        mapOf(
                            "action" to JsonPrimitive("get"),
                            "sessionID" to JsonPrimitive(sessionId)
                        )
                    )
                ), null)
            )
        ) { outputData ->
            if (outputData.status == UseCaseResponse.COMPLETE) {
                try {
                    val text = outputData.data?.text ?: "[]"
                    val messages = Json.decodeFromString<List<JsonObject>>(text)
                    val chatMessages = messages.map {
                        ChatMessage(
                            id = it["id"]?.jsonPrimitive?.content ?: "",
                            text = it["message"]?.jsonPrimitive?.content ?: "",
                            isFromUser = (it["author"]?.jsonPrimitive?.content?.equals("me")) ?: false,
                            timestamp = System.currentTimeMillis(),
                        )
                    }
                    callback(chatMessages)
                } catch (e : Exception) {
                    logE { "Session get successful, but invalid data returned:\n${e.stackTraceToString()}" }
                }
            }
        }
    }

    fun sendQuery(
        message : String,
        sessionId : String,
        fileAttachment: FileAttachment?,
        handler : String = "nova"
    ) {

        val binaryAttachment = fileAttachment?.let { file ->
            listOf(
                BlobData(
                    mime = file.fileName,
                    data = file.data ?: "".toByteArray()
                )
            )
        }

        logD { "Attaching file? ${binaryAttachment != null}" }

        sendCommand(
            InputData(
                command = "query",
                sessionId = sessionId,
                data = DataContainer(
                    text = Json.encodeToString(
                        QueryRequest(
                            handler = handler,
                            modelName = "",
                            modelVersion = "",
                            query = message,
                        )
                    ),
                    binary = binaryAttachment
                )
            )
        ) { outputData ->
            logD { "Message response: ${outputData.data?.text}" }
            if (outputData.status == UseCaseResponse.IN_PROGRESS || outputData.status == UseCaseResponse.COMPLETE) {
                try {
                    val response =
                        Json.decodeFromString<AgentResponse>(outputData.data?.text ?: "{}")
                    if (response.type == "text") {
                        onMessageUpdate(outputData.jobId, response.response, listOf())
                    } else if (response.type == "tool") {
                        onMessageUpdate(outputData.jobId, response.response, listOf(ToolInfo(response.response, ToolStatus.Running)))
                    }
                } catch (e : Exception) {
                    logE { "Error parsing agent response: ${e.message}" }
                    logE { "JSON input: ${outputData.data?.text}" }

                    // Fallback: Treat as plain text if JSON parsing fails
                    // This handles cases where orchestration returns plain text instead of JSON
                    outputData.data?.text?.let { plainText ->
                        if (plainText.isNotBlank() && plainText != "{}") {
                            logD { "Using plain text fallback for response" }
                            onMessageUpdate(outputData.jobId, plainText, listOf())
                        }
                    }
                }
            }
        }
    }

    private fun sendCommand(input : InputData, callback : (OutputData) -> Unit) {
        ioScope.launch {
            val jobId = qtClient.sendCommand(input)

            if (jobId < 0) {
                callback(
                    OutputData(
                        -1,
                        UseCaseResponse.FAILED,
                        DataContainer("Error connecting to core")
                    )
                )
            } else {
                callbacks[jobId] = callback
            }
        }
    }

    private fun scheduleReconnect() {
        ioScope.launch {
            sleep(500)
            qtClient.connectService(this@QTConnection, platformContext)
        }
    }

    override fun onConnectionStatus(status: ConnectionStatus) {

        if (status != ConnectionStatus.CONNECTED) {
            scheduleReconnect()
        } else {
            onConnect()
            getSessions() // refresh/load
        }

        isConnected = status == ConnectionStatus.CONNECTED
    }

    override fun onResult(result: OutputData) {
        callbacks[result.jobId]?.invoke(result)
    }

}