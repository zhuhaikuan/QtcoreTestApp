package com.lenovo.quantum.test

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.TextFieldValue
import com.lenovo.quantum.sdk.logging.logD
import com.lenovo.quantum.sdk.logging.logE
import com.lenovo.quantum.test.components.IRecorderListener
import com.lenovo.quantum.test.client.AudioStream
import com.lenovo.quantum.test.components.RecorderState
import com.lenovo.quantum.test.components.rememberAudioRecorder
import com.lenovo.quantum.test.util.MicrophonePermissionStatus
import com.lenovo.quantum.test.util.Permissions
import com.lenovo.quantum.test.util.rememberMicrophonePermissionHandler
import kotlinx.coroutines.launch

@Composable
fun Microphone(
    onAddSample: (AudioStream) -> Unit,
    modifier: Modifier = Modifier,
    onTextChanged: (TextFieldValue) -> Unit
) {
    val permissionHandler = rememberMicrophonePermissionHandler(Permissions.AUDIO)
    val permissionStatus by permissionHandler.microphonePermissionStatus.collectAsState()

    val audioRecorder = rememberAudioRecorder()
    val recorderState by audioRecorder.recorderStateFlow.collectAsState()

    var totalAudioSizeRecorded: Long by remember { mutableLongStateOf(0L) }
    var userMessage by remember { mutableStateOf("Check microphone permission...") }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        permissionHandler.checkPermission()
    }

    var icon = Icons.Filled.Mic
    var tint = MaterialTheme.colorScheme.primary
    var contentDescription  = "Start Recording"

    IconButton(
        onClick = when (permissionStatus) {
            MicrophonePermissionStatus.GRANTED -> {
                if (recorderState == RecorderState.RECORDING) {
                    icon = Icons.Filled.Stop
                    tint = Color.Red
                    contentDescription = "Stop Recording"
                    userMessage = "Recording... Total size so far: $totalAudioSizeRecorded bytes"
                }
                else {
                    userMessage = if (recorderState == RecorderState.STOPPED) {
                        "Recording finished! Total size: $totalAudioSizeRecorded bytes"
                    } else if (recorderState == RecorderState.ERROR) {
                        "Error during recording."
                    } else {
                        "Ready to record."
                    }
                }
                {
                    coroutineScope.launch {
                        if (audioRecorder.isRecording()) {
                            audioRecorder.stopRecording()
                        } else {
                            totalAudioSizeRecorded = 0L
                            audioRecorder.setListener(object : IRecorderListener {
                                override fun onUpdateReceived(state: RecorderState?) {
                                    when (state) {
                                        RecorderState.RECORDING -> logD { "recording in progress" }
                                        RecorderState.IDLE -> logD { "recording in idle" }
                                        RecorderState.STOPPED -> logD { "recording done" }
                                        RecorderState.ERROR -> logE { "there was an internal error during recording" }
                                        null -> logE { "recorder state is null" }
                                    }
                                }

                                override fun onDataReceived(data: AudioStream) {
                                    logD { "onDataReceived" }
                                    onAddSample(data)
                                    totalAudioSizeRecorded += data.data.size
                                }
                            })
                            audioRecorder.startRecording()
                        }
                    }
                }
            }
            MicrophonePermissionStatus.DENIED -> {
                userMessage = "Mic permission denied. Please enable it in settings."
                { permissionHandler.requestPermission() }
            }
            MicrophonePermissionStatus.NOTDETERMINED -> {
                userMessage = "Mic permission needed to record audio."
                { permissionHandler.requestPermission() }
            }
            MicrophonePermissionStatus.UNAVAILABLE -> {
                userMessage = "Mic is not available on this device/platform."
                { logE { "Mic is not available on this device/platform." } }
            }
        },
        modifier = modifier
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = modifier.fillMaxSize(0.7f)
        )
    }
    onTextChanged(TextFieldValue(userMessage))
}