package com.lenovo.quantum.test.util

import androidx.compose.runtime.Composable
import kotlinx.coroutines.flow.StateFlow

enum class MicrophonePermissionStatus {
    GRANTED,
    DENIED,
    NOTDETERMINED,
    UNAVAILABLE
}

enum class Permissions {
    AUDIO
}

expect open class MicrophonePermissionHandler {
    val microphonePermissionStatus: StateFlow<MicrophonePermissionStatus>

    fun checkPermission(): MicrophonePermissionStatus

    fun requestPermission()
}

@Composable
expect fun rememberMicrophonePermissionHandler(permission: Permissions): MicrophonePermissionHandler