/*
 * Copyright (C) 2025 Lenovo
 * All Rights Reserved.
 * Lenovo Confidential Restricted.
 */
package com.lenovo.quantum.test

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.lenovo.quantum.test.accountmanager.AccountManager
//import com.lenovo.quantum.test.mcp.McpTestActivity
import com.lenovo.quantum.sdk.getQuantumClient
import com.lenovo.quantum.test.shell.components.MainShell
import com.lenovo.quantum.test.shell.viewmodels.ShellViewModel

class NovaActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val filePicker = AndroidFilePicker(applicationContext)
        appContext = applicationContext
        AccountManager.initialize(this@NovaActivity)
        enableEdgeToEdge()

        setContent {
            filePicker.RegisterForActivityResult()
            MainShell(ShellViewModel(applicationContext, filePicker).also { it.toggleSidebar() }, isAndroid = true)
        }
    }

    override fun onDestroy() {
        getQuantumClient().disconnectFromService(baseContext)
        super.onDestroy()
    }
}

/**
 * Wrapper around App that adds a floating action button to launch MCP test screen
 */
//@Composable
//fun AppWithMcpButton(chatbotViewModel: ChatbotViewModel) {
//    val context = LocalContext.current
//
//    Box(modifier = Modifier.fillMaxSize()) {
//        // Main app content
//        App(chatbotViewModel)
//
//        // Floating action button to launch MCP test
//        FloatingActionButton(
//            onClick = {
//                val intent = Intent(context, McpTestActivity::class.java)
//                context.startActivity(intent)
//            },
//            modifier = Modifier
//                .align(Alignment.BottomEnd)
//                .padding(16.dp)
//        ) {
//            Icon(Icons.Default.Settings, contentDescription = "MCP Test")
//        }
//    }
//}