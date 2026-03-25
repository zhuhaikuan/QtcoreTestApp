package com.lenovo.quantum.test.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.lenovo.quantum.sdk.logging.logD
import com.lenovo.quantum.sdk.logging.logE
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.TargetDataLine

actual open class MicrophonePermissionHandler {
    private val _microphonePermissionStatus = MutableStateFlow(checkInitialDesktopPermission())
    actual val microphonePermissionStatus: StateFlow<MicrophonePermissionStatus> = _microphonePermissionStatus

    private fun checkInitialDesktopPermission(): MicrophonePermissionStatus {
        // On desktop, we often assume permission if a microphone line can be obtained.
        // OS-level privacy settings might block this silently.
        // A more robust check might try to open a line briefly.
        return try {
            val mixerInfo = AudioSystem.getMixerInfo()
            var micFound = false
            for (info in mixerInfo) {
                val mixer = AudioSystem.getMixer(info)
                val targetLineInfo = mixer.targetLineInfo
                for (lineInfo in targetLineInfo) {
                    if (lineInfo.lineClass == TargetDataLine::class.java) {
                        // Try to get the line to see if it's accessible
                        AudioSystem.getLine(lineInfo)?.use {
                            // If we can get here, it's likely available.
                            // Note: 'use' will close the line automatically.
                            logD { "There is a potentially available audio line!" }
                        }
                        micFound = true
                        // Found a potential microphone line
                        break
                    }
                }
                if (micFound) break
            }
            if (micFound) MicrophonePermissionStatus.GRANTED else MicrophonePermissionStatus.UNAVAILABLE
        } catch (e: Exception) {
            logE { "Desktop microphone check error: ${e.message}" }
            MicrophonePermissionStatus.UNAVAILABLE
        }
    }

    actual fun checkPermission(): MicrophonePermissionStatus {
        _microphonePermissionStatus.value = checkInitialDesktopPermission()
        return _microphonePermissionStatus.value
    }

    actual fun requestPermission() {
        logD { "Microphone access is typically managed by OS settings if restricted. Attempting to re-check status." }
        checkPermission() // Re-evaluate and update the flow
        if (_microphonePermissionStatus.value == MicrophonePermissionStatus.GRANTED) {
            logD { "Microphone appears to be available." }
        } else {
            logE { "Microphone still not available/granted. User may need to check " +
                    "OS Privacy Settings for Microphone." }
        }
    }

}

@Composable
actual fun rememberMicrophonePermissionHandler(permission: Permissions): MicrophonePermissionHandler {
    return remember { MicrophonePermissionHandler() }
}
