package com.lenovo.quantum.test.mcp///*
// * Copyright (C) 2025 Lenovo
// * All Rights Reserved.
// * Lenovo Confidential Restricted.
// */
//package com.lenovo.quantum.testapp.mcp
//
//import androidx.compose.foundation.background
//import androidx.compose.foundation.border
//import androidx.compose.foundation.clickable
//import androidx.compose.foundation.layout.*
//import androidx.compose.foundation.lazy.LazyColumn
//import androidx.compose.foundation.lazy.items
//import androidx.compose.foundation.lazy.rememberLazyListState
//import androidx.compose.foundation.shape.CircleShape
//import androidx.compose.foundation.shape.RoundedCornerShape
//import androidx.compose.material.icons.Icons
//import androidx.compose.material.icons.filled.*
//import androidx.compose.material3.*
//import androidx.compose.runtime.*
//import androidx.compose.ui.Alignment
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.graphics.Color
//import androidx.compose.ui.platform.LocalContext
//import androidx.compose.ui.text.font.FontFamily
//import androidx.compose.ui.text.font.FontWeight
//import androidx.compose.ui.unit.dp
//import androidx.compose.ui.unit.sp
//
///**
// * MCP Test Screen - Interactive UI for testing ActionCore MCP integration
// */
//@OptIn(ExperimentalMaterial3Api::class)
//@Composable
//fun McpTestScreen() {
//    val context = LocalContext.current
//    val viewModel = remember { McpTestViewModel(context) }
//    val scrollState = rememberLazyListState()
//
//    // Auto-scroll to bottom when new logs are added
//    LaunchedEffect(viewModel.testLogs.size) {
//        if (viewModel.testLogs.isNotEmpty()) {
//            scrollState.animateScrollToItem(viewModel.testLogs.size - 1)
//        }
//    }
//
//    Scaffold(
//        topBar = {
//            TopAppBar(
//                title = { Text("ActionCore MCP Test") },
//                colors = TopAppBarDefaults.topAppBarColors(
//                    containerColor = MaterialTheme.colorScheme.primaryContainer,
//                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
//                )
//            )
//        }
//    ) { padding ->
//        Column(
//            modifier = Modifier
//                .fillMaxSize()
//                .padding(padding)
//                .padding(16.dp)
//        ) {
//            // Connection Status Card
//            ConnectionStatusCard(
//                viewModel = viewModel,
//                modifier = Modifier.fillMaxWidth()
//            )
//
//            Spacer(modifier = Modifier.height(16.dp))
//
//            // Action Buttons Row
//            ActionButtonsRow(
//                viewModel = viewModel,
//                modifier = Modifier.fillMaxWidth()
//            )
//
//            Spacer(modifier = Modifier.height(16.dp))
//
//            // Tools Section
//            if (viewModel.connectionStatus == McpTestViewModel.ConnectionStatus.CONNECTED) {
//                ToolsSection(
//                    viewModel = viewModel,
//                    modifier = Modifier.fillMaxWidth()
//                )
//
//                Spacer(modifier = Modifier.height(16.dp))
//
//                // Tool Testing Section
//                if (viewModel.selectedTool != null) {
//                    ToolTestingSection(
//                        viewModel = viewModel,
//                        modifier = Modifier.fillMaxWidth()
//                    )
//
//                    Spacer(modifier = Modifier.height(16.dp))
//                }
//            }
//
//            // Test Logs Section
//            TestLogsSection(
//                viewModel = viewModel,
//                scrollState = scrollState,
//                modifier = Modifier
//                    .fillMaxWidth()
//                    .weight(1f)
//            )
//        }
//    }
//}
//
///**
// * Connection status indicator card
// */
//@Composable
//fun ConnectionStatusCard(
//    viewModel: McpTestViewModel,
//    modifier: Modifier = Modifier
//) {
//    Card(
//        modifier = modifier,
//        colors = CardDefaults.cardColors(
//            containerColor = MaterialTheme.colorScheme.surfaceVariant
//        )
//    ) {
//        Row(
//            modifier = Modifier
//                .fillMaxWidth()
//                .padding(16.dp),
//            horizontalArrangement = Arrangement.SpaceBetween,
//            verticalAlignment = Alignment.CenterVertically
//        ) {
//            Column {
//                Text(
//                    text = "Server: ${viewModel.serverName}",
//                    style = MaterialTheme.typography.titleMedium,
//                    fontWeight = FontWeight.Bold
//                )
//                Spacer(modifier = Modifier.height(4.dp))
//                Row(verticalAlignment = Alignment.CenterVertically) {
//                    StatusIndicator(status = viewModel.connectionStatus)
//                    Spacer(modifier = Modifier.width(8.dp))
//                    Text(
//                        text = viewModel.connectionStatus.name,
//                        style = MaterialTheme.typography.bodyMedium
//                    )
//                }
//            }
//
//            // Connect/Disconnect Button
//            if (viewModel.connectionStatus == McpTestViewModel.ConnectionStatus.DISCONNECTED ||
//                viewModel.connectionStatus == McpTestViewModel.ConnectionStatus.ERROR
//            ) {
//                Button(
//                    onClick = { viewModel.connect() },
//                    enabled = viewModel.connectionStatus != McpTestViewModel.ConnectionStatus.CONNECTING
//                ) {
//                    Icon(Icons.Default.PlayArrow, contentDescription = null)
//                    Spacer(modifier = Modifier.width(4.dp))
//                    Text("Connect")
//                }
//            } else if (viewModel.connectionStatus == McpTestViewModel.ConnectionStatus.CONNECTED) {
//                Button(
//                    onClick = { viewModel.disconnect() },
//                    colors = ButtonDefaults.buttonColors(
//                        containerColor = MaterialTheme.colorScheme.error
//                    )
//                ) {
//                    Icon(Icons.Default.Close, contentDescription = null)
//                    Spacer(modifier = Modifier.width(4.dp))
//                    Text("Disconnect")
//                }
//            }
//        }
//    }
//}
//
///**
// * Status indicator dot
// */
//@Composable
//fun StatusIndicator(status: McpTestViewModel.ConnectionStatus) {
//    val color = when (status) {
//        McpTestViewModel.ConnectionStatus.DISCONNECTED -> Color.Gray
//        McpTestViewModel.ConnectionStatus.CONNECTING -> Color(0xFFFFA500) // Orange
//        McpTestViewModel.ConnectionStatus.CONNECTED -> Color(0xFF4CAF50) // Green
//        McpTestViewModel.ConnectionStatus.ERROR -> Color(0xFFF44336) // Red
//    }
//
//    Box(
//        modifier = Modifier
//            .size(12.dp)
//            .background(color, CircleShape)
//    )
//}
//
///**
// * Action buttons row
// */
//@Composable
//fun ActionButtonsRow(
//    viewModel: McpTestViewModel,
//    modifier: Modifier = Modifier
//) {
//    Row(
//        modifier = modifier,
//        horizontalArrangement = Arrangement.spacedBy(8.dp)
//    ) {
//        Button(
//            onClick = { viewModel.listTools() },
//            enabled = viewModel.connectionStatus == McpTestViewModel.ConnectionStatus.CONNECTED,
//            modifier = Modifier.weight(1f)
//        ) {
//            Icon(Icons.Default.List, contentDescription = null)
//            Spacer(modifier = Modifier.width(4.dp))
//            Text("List Tools")
//        }
//
//        Button(
//            onClick = { viewModel.runAutomatedTests() },
//            enabled = !viewModel.isRunningTests,
//            modifier = Modifier.weight(1f)
//        ) {
//            Icon(Icons.Default.PlayArrow, contentDescription = null)
//            Spacer(modifier = Modifier.width(4.dp))
//            Text(if (viewModel.isRunningTests) "Running..." else "Run Tests")
//        }
//
//        IconButton(
//            onClick = { viewModel.clearLogs() }
//        ) {
//            Icon(Icons.Default.Delete, contentDescription = "Clear Logs")
//        }
//    }
//}
//
///**
// * Available tools section
// */
//@Composable
//fun ToolsSection(
//    viewModel: McpTestViewModel,
//    modifier: Modifier = Modifier
//) {
//    Card(
//        modifier = modifier,
//        colors = CardDefaults.cardColors(
//            containerColor = MaterialTheme.colorScheme.surfaceVariant
//        )
//    ) {
//        Column(
//            modifier = Modifier.padding(16.dp)
//        ) {
//            Text(
//                text = "Available Tools (${viewModel.availableTools.size})",
//                style = MaterialTheme.typography.titleMedium,
//                fontWeight = FontWeight.Bold
//            )
//
//            Spacer(modifier = Modifier.height(8.dp))
//
//            if (viewModel.availableTools.isEmpty()) {
//                Text(
//                    text = "No tools available. Click 'List Tools' to fetch.",
//                    style = MaterialTheme.typography.bodyMedium,
//                    color = MaterialTheme.colorScheme.onSurfaceVariant
//                )
//            } else {
//                Column(
//                    verticalArrangement = Arrangement.spacedBy(4.dp)
//                ) {
//                    viewModel.availableTools.take(5).forEach { tool ->
//                        ToolItem(
//                            tool = tool,
//                            isSelected = viewModel.selectedTool == tool,
//                            onClick = { viewModel.selectTool(tool) }
//                        )
//                    }
//
//                    if (viewModel.availableTools.size > 5) {
//                        Text(
//                            text = "... and ${viewModel.availableTools.size - 5} more tools",
//                            style = MaterialTheme.typography.bodySmall,
//                            color = MaterialTheme.colorScheme.onSurfaceVariant,
//                            modifier = Modifier.padding(8.dp)
//                        )
//                    }
//                }
//            }
//        }
//    }
//}
//
///**
// * Individual tool item
// */
//@Composable
//fun ToolItem(
//    tool: McpTestViewModel.ToolInfo,
//    isSelected: Boolean,
//    onClick: () -> Unit
//) {
//    Surface(
//        modifier = Modifier
//            .fillMaxWidth()
//            .clickable(onClick = onClick),
//        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
//        shape = RoundedCornerShape(4.dp)
//    ) {
//        Row(
//            modifier = Modifier.padding(8.dp),
//            verticalAlignment = Alignment.CenterVertically
//        ) {
//            if (isSelected) {
//                Icon(
//                    Icons.Default.Check,
//                    contentDescription = null,
//                    tint = MaterialTheme.colorScheme.primary,
//                    modifier = Modifier.size(16.dp)
//                )
//                Spacer(modifier = Modifier.width(4.dp))
//            }
//            Column {
//                Text(
//                    text = tool.name,
//                    style = MaterialTheme.typography.bodyMedium,
//                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
//                )
//                if (tool.description != null) {
//                    Text(
//                        text = tool.description,
//                        style = MaterialTheme.typography.bodySmall,
//                        color = MaterialTheme.colorScheme.onSurfaceVariant
//                    )
//                }
//            }
//        }
//    }
//}
//
///**
// * Tool testing section (for calling selected tool)
// */
//@Composable
//fun ToolTestingSection(
//    viewModel: McpTestViewModel,
//    modifier: Modifier = Modifier
//) {
//    Card(
//        modifier = modifier,
//        colors = CardDefaults.cardColors(
//            containerColor = MaterialTheme.colorScheme.surfaceVariant
//        )
//    ) {
//        Column(
//            modifier = Modifier.padding(16.dp)
//        ) {
//            Text(
//                text = "Test Tool: ${viewModel.selectedTool?.name}",
//                style = MaterialTheme.typography.titleMedium,
//                fontWeight = FontWeight.Bold
//            )
//
//            Spacer(modifier = Modifier.height(8.dp))
//
//            Text(
//                text = "Arguments (JSON):",
//                style = MaterialTheme.typography.bodySmall
//            )
//
//            Spacer(modifier = Modifier.height(4.dp))
//
//            OutlinedTextField(
//                value = viewModel.toolArguments,
//                onValueChange = { viewModel.toolArguments = it },
//                modifier = Modifier.fillMaxWidth(),
//                minLines = 3,
//                textStyle = LocalTextStyle.current.copy(fontFamily = FontFamily.Monospace)
//            )
//
//            Spacer(modifier = Modifier.height(8.dp))
//
//            Button(
//                onClick = { viewModel.callSelectedTool() },
//                modifier = Modifier.fillMaxWidth()
//            ) {
//                Icon(Icons.Default.Send, contentDescription = null)
//                Spacer(modifier = Modifier.width(4.dp))
//                Text("Call Tool")
//            }
//
//            Spacer(modifier = Modifier.height(8.dp))
//
//            // Show schema
//            Text(
//                text = "Input Schema:",
//                style = MaterialTheme.typography.bodySmall
//            )
//            Surface(
//                modifier = Modifier.fillMaxWidth(),
//                color = MaterialTheme.colorScheme.surface,
//                shape = RoundedCornerShape(4.dp),
//                tonalElevation = 2.dp
//            ) {
//                Text(
//                    text = viewModel.selectedTool?.inputSchema ?: "{}",
//                    style = MaterialTheme.typography.bodySmall,
//                    fontFamily = FontFamily.Monospace,
//                    modifier = Modifier.padding(8.dp)
//                )
//            }
//        }
//    }
//}
//
///**
// * Test logs section
// */
//@Composable
//fun TestLogsSection(
//    viewModel: McpTestViewModel,
//    scrollState: androidx.compose.foundation.lazy.LazyListState,
//    modifier: Modifier = Modifier
//) {
//    Card(
//        modifier = modifier,
//        colors = CardDefaults.cardColors(
//            containerColor = MaterialTheme.colorScheme.surface
//        )
//    ) {
//        Column(
//            modifier = Modifier.padding(16.dp)
//        ) {
//            Row(
//                modifier = Modifier.fillMaxWidth(),
//                horizontalArrangement = Arrangement.SpaceBetween,
//                verticalAlignment = Alignment.CenterVertically
//            ) {
//                Text(
//                    text = "Test Logs (${viewModel.testLogs.size})",
//                    style = MaterialTheme.typography.titleMedium,
//                    fontWeight = FontWeight.Bold
//                )
//            }
//
//            Spacer(modifier = Modifier.height(8.dp))
//
//            Surface(
//                modifier = Modifier.fillMaxSize(),
//                color = Color(0xFF1E1E1E),
//                shape = RoundedCornerShape(4.dp)
//            ) {
//                if (viewModel.testLogs.isEmpty()) {
//                    Box(
//                        modifier = Modifier.fillMaxSize(),
//                        contentAlignment = Alignment.Center
//                    ) {
//                        Text(
//                            text = "No logs yet. Start testing!",
//                            color = Color.Gray,
//                            style = MaterialTheme.typography.bodyMedium
//                        )
//                    }
//                } else {
//                    LazyColumn(
//                        state = scrollState,
//                        modifier = Modifier
//                            .fillMaxSize()
//                            .padding(8.dp)
//                    ) {
//                        items(viewModel.testLogs) { log ->
//                            LogEntry(log = log)
//                        }
//                    }
//                }
//            }
//        }
//    }
//}
//
///**
// * Individual log entry
// */
//@Composable
//fun LogEntry(log: McpTestViewModel.TestLog) {
//    val color = when (log.level) {
//        McpTestViewModel.LogLevel.INFO -> Color(0xFF2196F3) // Blue
//        McpTestViewModel.LogLevel.SUCCESS -> Color(0xFF4CAF50) // Green
//        McpTestViewModel.LogLevel.ERROR -> Color(0xFFF44336) // Red
//        McpTestViewModel.LogLevel.DEBUG -> Color(0xFF9E9E9E) // Gray
//    }
//
//    val prefix = when (log.level) {
//        McpTestViewModel.LogLevel.INFO -> "[INFO]"
//        McpTestViewModel.LogLevel.SUCCESS -> "[✓]"
//        McpTestViewModel.LogLevel.ERROR -> "[✗]"
//        McpTestViewModel.LogLevel.DEBUG -> "[DEBUG]"
//    }
//
//    Text(
//        text = "$prefix ${log.message}",
//        color = color,
//        fontFamily = FontFamily.Monospace,
//        fontSize = 12.sp,
//        modifier = Modifier.padding(vertical = 2.dp)
//    )
//}
