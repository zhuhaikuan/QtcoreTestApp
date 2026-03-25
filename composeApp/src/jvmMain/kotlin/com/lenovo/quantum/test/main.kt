/*
 * Copyright (C) 2025 Lenovo
 * All Rights Reserved.
 * Lenovo Confidential Restricted.
 */
package com.lenovo.quantum.test

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.lenovo.quantum.test.App
import com.lenovo.quantum.test.ChatbotViewModel
import com.lenovo.quantum.test.DesktopFilePicker
import com.lenovo.quantum.test.accountmanager.AccountManager
import com.lenovo.quantum.test.shell.components.MainShell
import com.lenovo.quantum.test.shell.viewmodels.ShellViewModel

data class AppArguments(
    val testApp: Boolean = false,
)

fun main(args: Array<String>) = application {
    val arguments = parseArguments(args)
    Window(
        onCloseRequest = ::exitApplication,
        title = if (arguments.testApp) "Quantum Test App" else "Nova",
//        undecorated = true, // Hide the decor view
//        transparent = true // Make the window transparent
    ) {
        AccountManager.initialize()
        if (arguments.testApp) {
            val viewModel = ChatbotViewModel(DesktopFilePicker(), null /*not used in jvm yet*/)
            App(ChatbotViewModel(DesktopFilePicker(), null /*not used in jvm yet*/))
        } else {
            MainShell(ShellViewModel(null, DesktopFilePicker()), false)
        }
    }
}

// Simple argument parser
fun parseArguments(args: Array<String>): AppArguments {
    var testApp = false

    args.forEach { arg ->
        when {
            arg == "--testapp" || arg == "-t" -> testApp = true
            arg.startsWith("--config=") -> {
                val configPath = arg.substringAfter("=")
                // Handle config path
            }
        }
    }

    return AppArguments(testApp)
}