package com.lenovo.quantum.test.shell.models

import com.lenovo.quantum.test.client.FileAttachment

data class ThreadPreview(
    val id: String,
    val title: String
)

enum class ToolStatus { Cloud, Local, Running, Complete, Error }

data class ToolInfo(
    val text: String,
    val status : ToolStatus
)

data class ChatMessage(
    val id: String,
    val text: String,
    val isFromUser: Boolean,
    val timestamp : Long,
    val toolInfo : List<ToolInfo> = listOf()
)

enum class NovaTab { Chat, Tools, Memories, Settings }

data class ScreenModel(
    val currentScreen : NovaTab = NovaTab.Chat,
    val chatScreenModel : ChatScreenModel = ChatScreenModel(),
    val toolsScreenModel : ToolsScreenModel = ToolsScreenModel(),
    val settingsScreenModel : SettingsScreenModel = SettingsScreenModel()
)

data class ToolsScreenModel(
    val toolServers : List<ToolServer> = listOf(),
)

data class ChatScreenModel(
    val previewThreads : List<ThreadPreview> = listOf(),
    val selectedThreadId : String? = null,
    val messages : List<ChatMessage> = listOf(),
    val textInput : String = "",
    val isSidebarCollapsed : Boolean = false,
    val attachment : FileAttachment? = null
)