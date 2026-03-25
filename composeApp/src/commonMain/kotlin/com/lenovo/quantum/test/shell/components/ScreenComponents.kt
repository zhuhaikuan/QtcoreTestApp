package com.lenovo.quantum.test.shell.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.lenovo.quantum.test.client.FileAttachment
import com.lenovo.quantum.test.shell.models.ChatMessage
import com.lenovo.quantum.test.shell.models.NovaTab
import com.lenovo.quantum.test.shell.models.ThreadPreview
import com.lenovo.quantum.test.shell.models.ToolDescriptor
import com.lenovo.quantum.test.shell.models.ToolServer

@Composable
fun NovaTheme(content: @Composable () -> Unit) {
    val scheme = lightColorScheme(
        primary = Color(0xFF5B57F3),
        secondary = Color(0xFF6B6A9E),
        surface = Color(0xFFFBF6FF),
        surfaceVariant = Color(0xFFECE6F6),
        onSurface = Color(0xFF2B2940),
        onSurfaceVariant = Color(0xFF6A6780),
    )
    MaterialTheme(colorScheme = scheme, typography = Typography(), content = content)
}

@Composable
fun NovaScreen(
    modifier: Modifier = Modifier,
    sidebarWidth: Dp = 300.dp,
    threads: List<ThreadPreview>,
    selectedThreadId: String?,
    tab: NovaTab,
    messages : List<ChatMessage>,
    tools : List<ToolServer>,
    attachedFile : FileAttachment?,
    onTabChange: (NovaTab) -> Unit,
    onNewThread: () -> Unit,
    onRefreshThreads: () -> Unit,
    onSelectThread: (String) -> Unit,
    onSendMessage: (String) -> Unit,
    isSidebarCollapsed: Boolean,
    onToggleSidebar: () -> Unit,
    onSelectFile: () -> Unit,
    isAndroid : Boolean,
    systemInstructions : String,
    onSystemInstructionsChange : (String) -> Unit,
    onSystemInstructionsSave : () -> Unit
) {
    Surface(modifier.fillMaxSize().padding(top=20.dp), color = MaterialTheme.colorScheme.surface) {
        Column(Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "Nova",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                IconButton(
                    onClick = onToggleSidebar,
                    modifier = Modifier.align(Alignment.CenterStart)
                ) {
                    val icon = if (isSidebarCollapsed) Icons.Default.Menu else Icons.Default.ChevronLeft
                    Icon(
                        icon,
                        contentDescription = if (isSidebarCollapsed) "Expand sidebar" else "Collapse sidebar"
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            NovaTopTabs(current = tab, onChange = onTabChange)
            if (tab == NovaTab.Chat) {
                ChatContainer(
                    Modifier.weight(1f),
                    sidebarWidth,
                    threads,
                    selectedThreadId,
                    messages,
                    attachedFile,
                    onNewThread,
                    onRefreshThreads,
                    onSelectThread,
                    onSendMessage,
                    isSidebarCollapsed,
                    onSelectFile,
                    isAndroid
                )
            } else if (tab == NovaTab.Tools) {
                GroupedToolsScreen(tools)
            } else if (tab == NovaTab.Settings) {
                SettingsScreen(
                    text = systemInstructions,
                    onTextChange = {onSystemInstructionsChange(it)},
                    onSaveClick = { onSystemInstructionsSave() }
                )
            }
        }
    }
}

@Composable
fun GroupedToolsScreen(
    tools: List<ToolServer>,
    onToolClick: (ToolDescriptor) -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {
        tools.forEachIndexed { index, server ->
            if (server.tools.isEmpty()) return@forEachIndexed

            Text(
                text = server.name.replace("-", " ").capitalizeWords(),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
            )

            AdaptiveCardGrid(
                items = server.tools,
                minCellSize = 320.dp,
                itemContent = { tool ->
                    ToolCard(
                        tool = tool,
                        onClick = { onToolClick(tool) }
                    )
                }
            )

            Spacer(Modifier.height(16.dp))
        }
    }
}

fun String.capitalizeWords(): String {
    return split(" ").map { word ->
        if (word.isNotEmpty()) {
            word.replaceFirstChar { it.uppercase() }
        } else {
            ""
        }
    }.joinToString(" ")
}

@Composable
fun ChatContainer(
    modifier: Modifier = Modifier,
    sidebarWidth: Dp = 300.dp,
    threads: List<ThreadPreview>,
    selectedThreadId: String?,
    messages : List<ChatMessage>,
    attachedFile : FileAttachment?,
    onNewThread: () -> Unit,
    onRefreshThreads: () -> Unit,
    onSelectThread: (String) -> Unit,
    onSendMessage: (String) -> Unit,
    isSidebarCollapsed: Boolean,
    onSelectFile: () -> Unit,
    isAndroid : Boolean
) {
    if (!isAndroid) {
        // —— DESKTOP (unchanged): side-by-side layout
        Row(modifier) {
            if (!isSidebarCollapsed) {
                ThreadsPane(
                    width = sidebarWidth,
                    threads = threads,
                    selectedThreadId = selectedThreadId,
                    onNewThread = onNewThread,
                    onRefresh = onRefreshThreads,
                    onSelect = onSelectThread,
                )
                Divider(Modifier.fillMaxHeight().width(1.dp))
            }

            Box(Modifier.weight(1f)) {
                Column(Modifier.fillMaxSize()) {
                    Box(Modifier.weight(1f).fillMaxWidth()) { MessagesContainer(messages) }
                    ChatComposer(onSend = onSendMessage, attachedFile = attachedFile, onSelectFile = onSelectFile)
                }
            }
        }
    } else {
        // —— ANDROID: chat takes 100%; sidebar overlays from the left when open
        Box(modifier) {
            // Chat content (always full screen)
            Column(Modifier.fillMaxSize()) {
                Box(Modifier.weight(1f).fillMaxWidth()) { MessagesContainer(messages) }
                ChatComposer(onSend = onSendMessage, attachedFile = attachedFile, onSelectFile = onSelectFile)
            }

            // Scrim when sidebar is open (tap to close)
//            if (!isSidebarCollapsed) {
//                Box(
//                    Modifier
//                        .matchParentSize()
//                        .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.32f))
//                        .clickable(onClick = onToggleSidebar)
//                        .zIndex(1f)
//                )
//            }

            // Overlayed ThreadsPane with slide animation
            AnimatedVisibility(
                visible = !isSidebarCollapsed,
                enter = slideInHorizontally { fullWidth -> -fullWidth },
                exit  = slideOutHorizontally { fullWidth -> -fullWidth },
            ) {
                Surface(
                    tonalElevation = 6.dp,
                    shadowElevation = 6.dp,
                    shape = RoundedCornerShape(topEnd = 16.dp, bottomEnd = 16.dp),
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(sidebarWidth)
                        .align(Alignment.CenterStart)
                        .zIndex(2f)
                ) {
                    ThreadsPane(
                        width = sidebarWidth,              // inner uses its own width
                        threads = threads,
                        selectedThreadId = selectedThreadId,
                        onNewThread = onNewThread,
                        onRefresh = onRefreshThreads,
                        onSelect = onSelectThread,
                    )
                }
            }
        }
    }
}

@Composable
fun NovaTopTabs(current: NovaTab, onChange: (NovaTab) -> Unit) {
    val tabs = NovaTab.entries
    val selectedIndex = tabs.indexOf(current).coerceAtLeast(0)

    Column(Modifier.fillMaxWidth()) {
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            TabRow(
                selectedTabIndex = selectedIndex,
                modifier = Modifier.widthIn(max = 520.dp),
                containerColor = Color.Transparent,
            ) {
                tabs.forEachIndexed { index, tab ->
                    Tab(
                        selected = index == selectedIndex,
                        onClick = { onChange(tab) },
                        text = { Text(tab.name, fontSize = 14.sp) }
                    )
                }
            }
        }
        Spacer(Modifier.height(4.dp))
    }
}