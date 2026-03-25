package com.lenovo.quantum.test.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import com.lenovo.quantum.sdk.logging.logD
import com.lenovo.quantum.sdk.logging.logE
import com.lenovo.quantum.test.client.AudioStream
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioSystem
import kotlinx.coroutines.isActive
import kotlinx.io.Buffer
import kotlinx.io.readByteArray
import javax.sound.sampled.DataLine
import javax.sound.sampled.LineUnavailableException
import javax.sound.sampled.TargetDataLine
import kotlin.coroutines.cancellation.CancellationException
import kotlin.math.min

actual class AudioRecorder {
    private var targetDataLine: TargetDataLine? = null
    private var recordingJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO)

    private val _recorderState = MutableStateFlow(RecorderState.IDLE)
    actual val recorderStateFlow: StateFlow<RecorderState> = _recorderState

    private var audioBuffer = Buffer()

    private var mListener: IRecorderListener? = null
    actual fun setListener(listener: IRecorderListener?) {
        mListener = listener
    }

    // AudioRecord Configuration: riff-24khz-16bit-mono-pcm
    private val sampleRate = 16000
    private val channels = 1
    private val bytesPerSample = 2
    private val audioFormat = AudioFormat(AudioFormat.Encoding.PCM_SIGNED, sampleRate.toFloat(), bytesPerSample*8, channels, 2, sampleRate.toFloat(), false)
    private val recordedAudioMime = "audio/l16;rate=16000"
    private var mRecordDuration: Float = 0.5f
    private val bufferSizeToSend = ((sampleRate * bytesPerSample * channels) * mRecordDuration).toInt()
    actual fun startRecording() {
        if (_recorderState.value == RecorderState.RECORDING) {
            logE { "Already recording." }
            return
        }

        try {
            val info = DataLine.Info(TargetDataLine::class.java, audioFormat)
            if (!AudioSystem.isLineSupported(info)) {
                logE { "Audio line not supported for format: $audioFormat" }
                _recorderState.value = RecorderState.ERROR
                return
            }

            targetDataLine = AudioSystem.getLine(info) as TargetDataLine
            targetDataLine?.open(audioFormat)
            targetDataLine?.start()

            _recorderState.value = RecorderState.RECORDING
            sendUpdate(_recorderState.value)
            logD { "Desktop recording started (raw data)." }

            recordingJob = scope.launch {
                val buffer = ByteArray(targetDataLine?.bufferSize ?: (2048 / 5)) // Read in chunks
                var totalBytesRead = 0
                logD { "buffer size is ${buffer.size}"}
                try {
                    while (isActive) {
                        if (targetDataLine?.isOpen == true) {
                            val bytesRead = targetDataLine?.read(buffer, 0, buffer.size) ?: 0
                            if (bytesRead > 0) {
                                totalBytesRead += bytesRead
                                audioBuffer.write(buffer)
                                while (totalBytesRead >= bufferSizeToSend) {
                                    val bufferCut = audioBuffer.readByteArray(bufferSizeToSend)
                                    sendData(recordedAudioMime, bufferCut)
                                    totalBytesRead -= bufferSizeToSend
                                }
                            } else {
                                logD {
                                    "states are isActive $isActive, targetLine " +
                                            "${targetDataLine?.isOpen}, isRunning ${targetDataLine?.isRunning}"
                                }
                                delay(100)
                            }
                        }
                    }
                    while (totalBytesRead >= bufferSizeToSend) {
                        val bufferCut = audioBuffer.readByteArray(bufferSizeToSend)
                        totalBytesRead -= bufferSizeToSend
                        sendData(recordedAudioMime, bufferCut, totalBytesRead == 0)
                    }
                    if (totalBytesRead > 0) {
                        val bufferCut = audioBuffer.readByteArray()
                        sendData(recordedAudioMime, bufferCut, eos = true)
                    }
                } catch (e: Exception) {
                    if (e !is CancellationException) {
                        logE { "Error during desktop raw audio capture: $e" }
                        withContext(Dispatchers.Main) {
                            _recorderState.value = RecorderState.ERROR
                        }
                    }
                } finally {
                    stopAndReleaseDataLineInternal()
                }
            }

        } catch (e: LineUnavailableException) {
            logE { "Line unavailable: ${e.message}" }
            _recorderState.value = RecorderState.ERROR
            stopAndReleaseDataLineInternal()
        } catch (e: Exception) {
            logE { "Failed to start desktop recording: ${e.message}" }
            _recorderState.value = RecorderState.ERROR
            stopAndReleaseDataLineInternal()
        }
    }

    actual fun stopRecording() {
        if (_recorderState.value != RecorderState.RECORDING) {
            if (_recorderState.value == RecorderState.IDLE || _recorderState.value == RecorderState.STOPPED ) {
                logE { "Recording not active or already stopped." }
            } else if (_recorderState.value == RecorderState.ERROR) {
                logE { "Recording was in an error state." }
            }
//            return audioBuffer?.toByteArray().also { // Return any partial data if error occurred mid-way
//                cleanupAfterStop()
//            }
        }

        recordingJob?.cancel()

        logD {"Desktop recording stopped" }
//        return recordedData
    }

    private fun sendUpdate(state: RecorderState?) {
        mListener?.onUpdateReceived(state)
    }

    private fun sendData(mimeType: String, samples: ByteArray, eos: Boolean = false) {
        mListener?.onDataReceived(AudioStream(mimeType = mimeType, data = samples, eos = eos))
    }


    private fun stopAndReleaseDataLineInternal() {
        _recorderState.value = RecorderState.STOPPED
        sendUpdate(_recorderState.value)
        targetDataLine?.stop()
        targetDataLine?.close()
        targetDataLine = null
    }

    actual fun isRecording(): Boolean {
        return _recorderState.value == RecorderState.RECORDING
    }

    actual fun getRecordingState(): RecorderState {
        return _recorderState.value
    }
}

@Composable
actual fun rememberAudioRecorder(): AudioRecorder {
    val audioRecorder = remember { AudioRecorder() }

    DisposableEffect(Unit) {
        onDispose {
            if (audioRecorder.isRecording()) {
                audioRecorder.stopRecording()
            }
        }
    }
    return audioRecorder
}