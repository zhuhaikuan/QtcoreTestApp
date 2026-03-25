package com.lenovo.quantum.test.shell.components

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.lenovo.quantum.test.shell.models.NovaTab
import com.lenovo.quantum.test.shell.viewmodels.ShellViewModel

@Composable
fun MainShell(viewModel: ShellViewModel, isAndroid : Boolean) {

    val model by viewModel.model.collectAsState()

    NovaTheme {
        Scaffold(contentWindowInsets = WindowInsets.safeDrawing) { innerPadding ->
            NovaScreen(
                threads = model.chatScreenModel.previewThreads,
                selectedThreadId = model.chatScreenModel.selectedThreadId,
                tab = model.currentScreen,
                messages = model.chatScreenModel.messages,
                tools = model.toolsScreenModel.toolServers,
                attachedFile = model.chatScreenModel.attachment,
                onTabChange = { viewModel.switchScreen(it) },
                onNewThread = { viewModel.setSelectedThread(null) },
                onRefreshThreads = { /* TODO */ },
                onSelectThread = { viewModel.setSelectedThread(it) },
                onSendMessage = { viewModel.sendMessage(it) },
                isSidebarCollapsed = model.chatScreenModel.isSidebarCollapsed,
                onToggleSidebar = { viewModel.toggleSidebar() },
                onSelectFile = { viewModel.selectFile() },
                modifier = Modifier.padding(innerPadding),
                isAndroid = isAndroid,
                systemInstructions = model.settingsScreenModel.systemInstructions,
                onSystemInstructionsChange = { viewModel.onSystemInstructionsType(it) },
                onSystemInstructionsSave = { viewModel.onSystemInstructionsSave() }
            )
        }
    }
}