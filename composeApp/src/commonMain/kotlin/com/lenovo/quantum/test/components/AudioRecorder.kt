package com.lenovo.quantum.test.components

import androidx.compose.runtime.Composable
import kotlinx.coroutines.flow.StateFlow

enum class RecorderState {
    IDLE,
    RECORDING,
    STOPPED,
    ERROR
}

expect class AudioRecorder {
    fun startRecording()
    fun setListener(listener: IRecorderListener?)
    fun stopRecording()
    fun isRecording(): Boolean
    fun getRecordingState(): RecorderState
    val recorderStateFlow: StateFlow<RecorderState>
}

@Composable
expect fun rememberAudioRecorder(): AudioRecorder