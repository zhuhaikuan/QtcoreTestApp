/*
 * Copyright (C) 2025 Lenovo
 * All Rights Reserved.
 * Lenovo Confidential Restricted.
 */
package com.lenovo.quantum.test.accountmanager

import com.lenovo.quantum.sdk.logging.logD
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

actual object AccountManager {

    private const val TAG = "testapp.AccountManager"

    actual var isInitialized = false
        private set

    actual val isLoggedIn : Boolean
        get() = false

    private val _state = MutableStateFlow(UserData())
    actual val state = _state.asStateFlow()

    actual val data : UserData
        get() =  _state.value

    actual fun initialize(context: Any?) {
        logD(TAG) { "Not implemented yet" }
    }

    actual fun onAccountClick() {
        logD(TAG) { "Not implemented yet" }
    }
}