package com.lenovo.quantum.test.shell.viewmodels

import androidx.lifecycle.ViewModel
import com.lenovo.quantum.sdk.apibridge.dataV1.OutputData
import com.lenovo.quantum.sdk.logging.logD
import com.lenovo.quantum.test.FilePicker
import com.lenovo.quantum.test.shell.models.ChatMessage
import com.lenovo.quantum.test.shell.models.NovaTab
import com.lenovo.quantum.test.shell.models.ScreenModel
import com.lenovo.quantum.test.shell.models.ThreadPreview
import com.lenovo.quantum.test.shell.models.ToolDescriptor
import com.lenovo.quantum.test.shell.models.ToolInfo
import com.lenovo.quantum.test.shell.models.ToolServer
import com.lenovo.quantum.test.shell.models.ToolStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive

class ShellViewModel(platformContext : Any?, private val filePicker : FilePicker) : ViewModel() {

    private val _model = MutableStateFlow(ScreenModel())
    val model = _model.asStateFlow()

    val qtConnection = QTConnection(platformContext, { onConnect() }, { onSessionsRefreshed(it) }) {
        jobId, content, toolInfo ->
        addOrUpdateMessage(jobId, content, toolInfo)
    }

    init {
        addThreadPreview("", "Loading...")
    }

    private fun onConnect() {
        loadSystemInstructions()
    }

    fun loadSystemInstructions() {
        qtConnection.getSystemInstructions { loadedInstructions ->
            _model.update {
                it.copy(
                    settingsScreenModel = it.settingsScreenModel.copy(
                        systemInstructions = loadedInstructions
                    )
                )
            }
        }
    }

    fun onSystemInstructionsType(newText : String) {
        _model.update {
            it.copy(
                settingsScreenModel = it.settingsScreenModel.copy(
                    systemInstructions = newText
                )
            )
        }
    }

    fun onSystemInstructionsSave() {
        qtConnection.saveSystemInstructions(model.value.settingsScreenModel.systemInstructions)
    }

    fun selectFile() {
        CoroutineScope(Dispatchers.Main).launch {
            val chosenFile = filePicker.launch()
            if (chosenFile != null) {
                _model.update {
                    it.copy(
                        chatScreenModel = it.chatScreenModel.copy(
                            attachment = chosenFile
                        )
                    )
                }
            }
        }
    }

    fun toggleSidebar() {
        _model.update {
            it.copy(
                chatScreenModel = it.chatScreenModel.copy(
                    isSidebarCollapsed = !it.chatScreenModel.isSidebarCollapsed
                )
            )
        }
    }

    fun sendMessage(message: String) {
        _model.update {
            it.copy(
                chatScreenModel = it.chatScreenModel.copy(
                    messages = it.chatScreenModel.messages + ChatMessage(
                        "",
                        message,
                        isFromUser = true,
                        timestamp = System.currentTimeMillis()
                    )
                )
            )
        }
        logD { "Added message $message" }

        val attachment = _model.value.chatScreenModel.attachment

        if (_model.value.chatScreenModel.selectedThreadId == null) {
            qtConnection.createSession { sessionId ->
                logD { "Using created session id: ${sessionId}" }
                addThreadPreview(sessionId, "New thread")
                setSelectedThread(sessionId, false)
                qtConnection.sendQuery(
                    message,
                    _model.value.chatScreenModel.selectedThreadId ?: "",
                    attachment
                )
            }
        } else {
            logD { "Using existing session id: ${_model.value.chatScreenModel.selectedThreadId}" }
            qtConnection.sendQuery(
                message,
                _model.value.chatScreenModel.selectedThreadId ?: "",
                attachment
            )
        }

        _model.update {
            it.copy(
                chatScreenModel = it.chatScreenModel.copy(
                    attachment = null
                )
            )
        }
    }

    fun parseGroupedToolsJson(
        jsonString: String,
        defaultCategory: String = "Tools"
    ): List<ToolServer> {
        val json = Json { ignoreUnknownKeys = true }

        val root = try {
            json.parseToJsonElement(jsonString)
        } catch (_: Throwable) {
            return emptyList()
        }

        if (root !is JsonArray) return emptyList()

        return root.mapNotNull { serverElem ->
            val serverObj = serverElem as? JsonObject ?: return@mapNotNull null

            val serverName = serverObj["server"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
            val toolsArray = serverObj["tools"] as? JsonArray ?: JsonArray(emptyList())

            val toolsForServer = toolsArray.mapNotNull { toolElem ->
                val t = toolElem as? JsonObject ?: return@mapNotNull null

                val name = t["name"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
                val description = t["description"]?.jsonPrimitive?.contentOrNull ?: ""

                val argsCount = t["inputSchema"]
                    ?.let { it as? JsonObject }
                    ?.get("properties")
                    ?.let { it as? JsonObject }
                    ?.size ?: 0

                ToolDescriptor(
                    id = name,                 // stable id = name
                    name = name,
                    description = description,
                    category = defaultCategory,
                    argsCount = argsCount
                )
            }
                // Safety: remove duplicates by name within this server
                .distinctBy { it.name }

            ToolServer(
                name = serverName,
                tools = toolsForServer
            )
        }
    }

    fun refreshTools() {
        qtConnection.refreshTools { toolsJson ->
            logD { "Got tools json: $toolsJson" }
            val toolServers = parseGroupedToolsJson(toolsJson)
            _model.update {
                it.copy(
                    toolsScreenModel = it.toolsScreenModel.copy(
                        toolServers = toolServers
                    )
                )
            }
        }
    }

    fun switchScreen(newScreen: NovaTab) {
        _model.update {
            it.copy(currentScreen = newScreen)
        }

        if (newScreen == NovaTab.Tools) {
            refreshTools()
        }
    }

    fun setSelectedThread(newSelectedId : String?, loadMessages : Boolean = true) {
        _model.update {
            it.copy(
                chatScreenModel = it.chatScreenModel.copy(
                    selectedThreadId = newSelectedId,
                    messages = if (loadMessages) listOf() else it.chatScreenModel.messages
                )
            )
        }

        if (loadMessages && newSelectedId != null) {
            qtConnection.getMessages(newSelectedId) { chatMessages ->
                _model.update {
                    it.copy(
                        chatScreenModel = it.chatScreenModel.copy(
                            messages = chatMessages
                        )
                    )
                }
            }
        }
    }

    fun addThreadPreview(id : String, title : String) {
        _model.update {
            it.copy(
                chatScreenModel = it.chatScreenModel.copy(
                    previewThreads = listOf(ThreadPreview(id, title)) + it.chatScreenModel.previewThreads
                )
            )
        }
    }

    private fun onSessionsRefreshed(sessions: List<SessionListResponse>) {
        val previewThreads = sessions.map {
            ThreadPreview(it.sessionID, it.sessionTitle)
        }
        _model.update {
            it.copy(
                chatScreenModel = it.chatScreenModel.copy(
                    previewThreads = previewThreads
                )
            )
        }
    }

    private fun addOrUpdateMessage(
        jobId : Long,
        content : String,
        toolInfo : List<ToolInfo> = listOf()
    ) {
        _model.update { m ->
            var found = false
            m.copy(
                chatScreenModel = m.chatScreenModel.copy(
                    messages = m.chatScreenModel.messages.map { msg ->
                        if (msg.id == jobId.toString()) {
                            found = true

                            val newText = if (toolInfo.isNotEmpty()) {
                                msg.text
                            } else {
                                content
                            }

                            msg.copy(
                                text = newText,
                                timestamp = System.currentTimeMillis(),
                                toolInfo = msg.toolInfo + toolInfo
                            )
                        } else {
                            msg
                        }
                    }.let { messages ->
                        if (!found) {

                            val newText = if (toolInfo.isNotEmpty()) {
                                "..."
                            } else {
                                content
                            }

                            messages + ChatMessage(
                                jobId.toString(),
                                newText,
                                isFromUser = false,
                                timestamp = System.currentTimeMillis(),
                                toolInfo = toolInfo
                            )
                        } else {
                            messages
                        }
                    }
                )
            )
        }
    }

}