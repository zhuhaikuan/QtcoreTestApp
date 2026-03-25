/*
 * Copyright (C) 2025 Lenovo
 * All Rights Reserved.
 * Lenovo Confidential Restricted.
 */
package com.lenovo.quantum.test

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.lenovo.quantum.test.accountmanager.AccountManager
import com.lenovo.quantum.sdk.getQuantumClient

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val filePicker = AndroidFilePicker(applicationContext)
        val filePicker2 = AndroidFilePicker2(applicationContext)
        appContext = applicationContext
        val viewModel = ChatbotViewModel(filePicker, filePicker2, applicationContext)
        AccountManager.initialize(this@MainActivity)
        enableEdgeToEdge()

        setContent {
            filePicker.RegisterForActivityResult()
            filePicker2.RegisterForActivityResult()
            App(viewModel)
        }
    }

    override fun onDestroy() {
        getQuantumClient().disconnectFromService(baseContext)
        super.onDestroy()
    }
}