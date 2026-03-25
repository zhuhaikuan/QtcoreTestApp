package com.lenovo.quantum.test.mcp///*
// * Copyright (C) 2025 Lenovo
// * All Rights Reserved.
// * Lenovo Confidential Restricted.
// */

// COMMENTED OUT - this was used for testing, relied on sdk changes we don't want

//package com.lenovo.quantum.testapp.mcp
//
//import android.content.Context
//import androidx.compose.runtime.getValue
//import androidx.compose.runtime.mutableStateListOf
//import androidx.compose.runtime.mutableStateOf
//import androidx.compose.runtime.setValue
//import androidx.lifecycle.ViewModel
//import androidx.lifecycle.viewModelScope
//import com.lenovo.quantum.sdk.logging.logD
//import com.lenovo.quantum.sdk.logging.logE
//import com.lenovo.quantum.sdk.logging.logI
//import com.lenovo.quantum.sdk.toolbox.ActionCoreServerConfig
//import com.lenovo.quantum.sdk.toolbox.ConnectionResult
//import com.lenovo.quantum.sdk.toolbox.McpClientManagerImpl
//import io.modelcontextprotocol.kotlin.sdk.TextContent
//import kotlinx.coroutines.launch
//import kotlinx.serialization.json.buildJsonObject
//
///**
// * ViewModel for MCP testing functionality.
// * Manages connection state, tool discovery, and test execution.
// */
//class McpTestViewModel(context: Context) : ViewModel() {
//
//    private val mcpClientManager = McpClientManagerImpl(context)
//
//    // Connection state
//    var connectionStatus by mutableStateOf(ConnectionStatus.DISCONNECTED)
//        private set
//
//    var serverName by mutableStateOf("actioncore-test")
//        private set
//
//    // Tool list
//    val availableTools = mutableStateListOf<ToolInfo>()
//
//    // Selected tool for testing
//    var selectedTool by mutableStateOf<ToolInfo?>(null)
//        private set
//
//    var toolArguments by mutableStateOf("{}")
//
//    // Test results/logs
//    val testLogs = mutableStateListOf<TestLog>()
//
//    // Test runner state
//    var isRunningTests by mutableStateOf(false)
//        private set
//
//    /**
//     * Connection status enum
//     */
//    enum class ConnectionStatus {
//        DISCONNECTED,
//        CONNECTING,
//        CONNECTED,
//        ERROR
//    }
//
//    /**
//     * Tool information
//     */
//    data class ToolInfo(
//        val name: String,
//        val description: String?,
//        val inputSchema: String
//    )
//
//    /**
//     * Test log entry
//     */
//    data class TestLog(
//        val timestamp: Long = System.currentTimeMillis(),
//        val level: LogLevel,
//        val message: String
//    )
//
//    enum class LogLevel {
//        INFO, SUCCESS, ERROR, DEBUG
//    }
//
//    /**
//     * Connect to ActionCore MCP server
//     */
//    fun connect() {
//        viewModelScope.launch {
//            try {
//                addLog(LogLevel.INFO, "Connecting to ActionCore server...")
//                connectionStatus = ConnectionStatus.CONNECTING
//
//                val config = ActionCoreServerConfig(name = serverName)
//                val result = mcpClientManager.connectToServer(config)
//
//                when (result) {
//                    is ConnectionResult.Success -> {
//                        connectionStatus = ConnectionStatus.CONNECTED
//                        addLog(LogLevel.SUCCESS, "Successfully connected to ActionCore!")
//
//                        // Automatically list tools after connection
//                        listTools()
//                    }
//                    is ConnectionResult.Error -> {
//                        connectionStatus = ConnectionStatus.ERROR
//                        addLog(LogLevel.ERROR, "Connection failed: ${result.message}")
//                        result.throwable?.let {
//                            addLog(LogLevel.ERROR, "Error: ${it.message}")
//                        }
//                    }
//                }
//            } catch (e: Exception) {
//                connectionStatus = ConnectionStatus.ERROR
//                addLog(LogLevel.ERROR, "Connection exception: ${e.message}")
//                logE(throwable = e) { "[McpTestViewModel] Connection failed" }
//            }
//        }
//    }
//
//    /**
//     * Disconnect from ActionCore MCP server
//     */
//    fun disconnect() {
//        viewModelScope.launch {
//            try {
//                addLog(LogLevel.INFO, "Disconnecting from ActionCore server...")
//
//                val result = mcpClientManager.disconnectServer(serverName)
//                if (result) {
//                    connectionStatus = ConnectionStatus.DISCONNECTED
//                    availableTools.clear()
//                    selectedTool = null
//                    addLog(LogLevel.SUCCESS, "Disconnected successfully")
//                } else {
//                    addLog(LogLevel.ERROR, "Disconnect failed - server not found")
//                }
//            } catch (e: Exception) {
//                addLog(LogLevel.ERROR, "Disconnect exception: ${e.message}")
//                logE(throwable = e) { "[McpTestViewModel] Disconnect failed" }
//            }
//        }
//    }
//
//    /**
//     * List all available tools from the MCP server
//     */
//    fun listTools() {
//        viewModelScope.launch {
//            try {
//                addLog(LogLevel.INFO, "Listing available tools...")
//
//                val client = mcpClientManager.getClient(serverName)
//                if (client == null) {
//                    addLog(LogLevel.ERROR, "Client not found. Please connect first.")
//                    return@launch
//                }
//
//                val toolsResponse = client.listTools()
//                if (toolsResponse != null) {
//                    availableTools.clear()
//                    toolsResponse.tools.forEach { tool ->
//                        val toolInfo = ToolInfo(
//                            name = tool.name,
//                            description = tool.description,
//                            inputSchema = tool.inputSchema.toString()
//                        )
//                        availableTools.add(toolInfo)
//
//                        addLog(LogLevel.DEBUG, "Tool: ${tool.name}")
//                        addLog(LogLevel.DEBUG, "  Description: ${tool.description}")
//                        addLog(LogLevel.DEBUG, "  Schema: ${tool.inputSchema}")
//                    }
//                    addLog(LogLevel.SUCCESS, "Found ${availableTools.size} tools")
//                } else {
//                    addLog(LogLevel.ERROR, "Failed to list tools - null response")
//                }
//            } catch (e: Exception) {
//                addLog(LogLevel.ERROR, "List tools exception: ${e.message}")
//                logE(throwable = e) { "[McpTestViewModel] List tools failed" }
//            }
//        }
//    }
//
//    /**
//     * Select a tool for testing
//     */
//    fun selectTool(tool: ToolInfo) {
//        selectedTool = tool
//        addLog(LogLevel.INFO, "Selected tool: ${tool.name}")
//    }
//
//    /**
//     * Call the currently selected tool with the provided arguments
//     */
//    fun callSelectedTool() {
//        val tool = selectedTool
//        if (tool == null) {
//            addLog(LogLevel.ERROR, "No tool selected")
//            return
//        }
//
//        viewModelScope.launch {
//            try {
//                addLog(LogLevel.INFO, "Calling tool: ${tool.name}")
//                addLog(LogLevel.DEBUG, "Arguments: $toolArguments")
//
//                val client = mcpClientManager.getClient(serverName)
//                if (client == null) {
//                    addLog(LogLevel.ERROR, "Client not found")
//                    return@launch
//                }
//
//                // Parse arguments
//                val args = try {
//                    if (toolArguments.trim().isEmpty() || toolArguments.trim() == "{}") {
//                        buildJsonObject { }
//                    } else {
//                        kotlinx.serialization.json.Json.parseToJsonElement(toolArguments).let {
//                            it as? kotlinx.serialization.json.JsonObject ?: buildJsonObject { }
//                        }
//                    }
//                } catch (e: Exception) {
//                    addLog(LogLevel.ERROR, "Invalid JSON arguments: ${e.message}")
//                    return@launch
//                }
//
//                val request = io.modelcontextprotocol.kotlin.sdk.CallToolRequest(
//                    name = tool.name,
//                    arguments = args
//                )
//
//                val response = client.callTool(request)
//                if (response != null) {
//                    val result = response.content.firstOrNull()?.let { content ->
//                        when (content) {
//                            is TextContent -> content.text
//                            else -> content.toString()
//                        }
//                    } ?: "No content"
//
//                    addLog(LogLevel.SUCCESS, "Tool call successful!")
//                    addLog(LogLevel.INFO, "Response: $result")
//                } else {
//                    addLog(LogLevel.ERROR, "Tool call failed - null response")
//                }
//            } catch (e: Exception) {
//                addLog(LogLevel.ERROR, "Tool call exception: ${e.message}")
//                logE(throwable = e) { "[McpTestViewModel] Tool call failed" }
//            }
//        }
//    }
//
//    /**
//     * Run automated test suite
//     */
//    fun runAutomatedTests() {
//        viewModelScope.launch {
//            isRunningTests = true
//            addLog(LogLevel.INFO, "=== Starting Automated Test Suite ===")
//
//            try {
//                // Test 1: Connection
//                addLog(LogLevel.INFO, "Test 1: Connection Test")
//                if (connectionStatus != ConnectionStatus.CONNECTED) {
//                    connect()
//                    kotlinx.coroutines.delay(2000) // Wait for connection
//                }
//
//                if (connectionStatus == ConnectionStatus.CONNECTED) {
//                    addLog(LogLevel.SUCCESS, "✓ Connection test passed")
//                } else {
//                    addLog(LogLevel.ERROR, "✗ Connection test failed")
//                    return@launch
//                }
//
//                // Test 2: Tool Discovery
//                addLog(LogLevel.INFO, "Test 2: Tool Discovery Test")
//                listTools()
//                kotlinx.coroutines.delay(1000)
//
//                if (availableTools.isNotEmpty()) {
//                    addLog(LogLevel.SUCCESS, "✓ Tool discovery test passed (${availableTools.size} tools found)")
//                } else {
//                    addLog(LogLevel.ERROR, "✗ Tool discovery test failed (no tools found)")
//                }
//
//                // Test 3: Tool Invocation (first tool with empty args)
//                if (availableTools.isNotEmpty()) {
//                    addLog(LogLevel.INFO, "Test 3: Tool Invocation Test")
//                    val firstTool = availableTools.first()
//                    selectedTool = firstTool
//                    toolArguments = "{}"
//                    callSelectedTool()
//                    kotlinx.coroutines.delay(1000)
//                }
//
//                addLog(LogLevel.INFO, "=== Automated Test Suite Completed ===")
//
//            } catch (e: Exception) {
//                addLog(LogLevel.ERROR, "Test suite exception: ${e.message}")
//                logE(throwable = e) { "[McpTestViewModel] Test suite failed" }
//            } finally {
//                isRunningTests = false
//            }
//        }
//    }
//
//    /**
//     * Clear all test logs
//     */
//    fun clearLogs() {
//        testLogs.clear()
//        addLog(LogLevel.INFO, "Logs cleared")
//    }
//
//    /**
//     * Add a log entry
//     */
//    private fun addLog(level: LogLevel, message: String) {
//        val log = TestLog(level = level, message = message)
//        testLogs.add(log)
//
//        // Also log to system logger
//        when (level) {
//            LogLevel.INFO -> logI { "[McpTest] $message" }
//            LogLevel.SUCCESS -> logI { "[McpTest] ✓ $message" }
//            LogLevel.ERROR -> logE { "[McpTest] ✗ $message" }
//            LogLevel.DEBUG -> logD { "[McpTest] $message" }
//        }
//    }
//
//    override fun onCleared() {
//        super.onCleared()
//        viewModelScope.launch {
//            mcpClientManager.disconnectAll()
//        }
//    }
//}
