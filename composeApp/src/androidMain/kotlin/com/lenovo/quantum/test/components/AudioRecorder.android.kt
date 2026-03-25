package com.lenovo.quantum.test.components

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.lenovo.quantum.sdk.logging.logD
import com.lenovo.quantum.sdk.logging.logE
import com.lenovo.quantum.test.client.AudioStream
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.io.Buffer
import kotlinx.io.readByteArray
import kotlin.math.min

actual class AudioRecorder(private val context: Context) {
    private var audioRecord: AudioRecord? = null
    private var recordingJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO)

    private val _recorderState = MutableStateFlow(RecorderState.IDLE)
    actual val recorderStateFlow: StateFlow<RecorderState> = _recorderState

    private var audioBuffer: Buffer? = null

    private var mListener: IRecorderListener? = null
    actual fun setListener(listener: IRecorderListener?) {
        mListener = listener
    }

    // Note: currently, the Speech SDK support 16 kHz sample rate, 16 bit samples, mono (single-channel) only.
    // AudioRecord Configuration: riff-16khz-16bit-mono-pcm
    private val recordedAudioMime = "audio/l16;rate=24000"
    private val sampleRate = 16000
    private val channelConfig = AudioFormat.CHANNEL_IN_MONO
    private val audioFormat = AudioFormat.ENCODING_PCM_16BIT
    private var bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)
    private val channels = 1
    private val bytesPerSample = 2
    private var mRecordDuration: Float = 0.1f

    // Permissions should be checked before calling
    @SuppressLint("MissingPermission")
    actual fun startRecording() {
        if (!hasRecordAudioPermission()) {
            _recorderState.value = RecorderState.ERROR
            logE { "ERROR: RECORD_AUDIO permission not granted." }
            return
        }

        if (_recorderState.value == RecorderState.RECORDING) {
            logE { "Already recording." }
            return
        }

        if (bufferSize == AudioRecord.ERROR_BAD_VALUE || bufferSize == AudioRecord.ERROR) {
            logE { "Error: Invalid AudioRecord buffer size." }
            _recorderState.value = RecorderState.ERROR
            return
        }

        //TODO check if it's the case to use audioSource MediaRecorder.AudioSource.VOICE_RECOGNITION
        audioRecord = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            sampleRate,
            channelConfig,
            audioFormat,
            bufferSize
        )

        if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
            logE { "Error: AudioRecord initialization failed." }
            audioRecord?.release()
            audioRecord = null
            _recorderState.value = RecorderState.ERROR
            return
        }

        audioBuffer = Buffer()

        val bufferSizeToSend = ((sampleRate * bytesPerSample * channels) * mRecordDuration).toInt()

        audioRecord?.startRecording()
        _recorderState.value = RecorderState.RECORDING
        logD { "Recording started (raw data). Buffer size: $bufferSize" }
        sendUpdate(_recorderState.value)

        var totalBytesRead = 0
        recordingJob = scope.launch {
            val data = ByteArray(bufferSize)
            while (isActive && audioRecord?.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                val bytesRead = audioRecord?.read(data, 0, bufferSize) ?: 0
                if (bytesRead > 0) {
                    totalBytesRead += bytesRead
                    audioBuffer?.write(data)
                    if (totalBytesRead >= bufferSizeToSend) {
                        val bufferCut = audioBuffer!!.readByteArray(
                            min(bufferSizeToSend.toLong(),audioBuffer!!.size).toInt())
                        sendData(recordedAudioMime, bufferCut)
                        totalBytesRead -= bufferSizeToSend
                        audioBuffer!!.clear()
                    }
                }
                else if (bytesRead < 0) { // Error
                    logE { "Error reading audio data: $bytesRead" }
                    withContext(Dispatchers.Main) {
                        _recorderState.value = RecorderState.ERROR
                    }
                    break
                }
            }
            while (totalBytesRead >= bufferSizeToSend) {
                val bufferCut = audioBuffer!!.readByteArray(bufferSizeToSend)
                totalBytesRead -= bufferSizeToSend
                sendData(recordedAudioMime, bufferCut, totalBytesRead == 0)
            }
            if (totalBytesRead > 0) {
                val bufferCut = audioBuffer!!.readByteArray()
                sendData(recordedAudioMime, bufferCut, eos = true)
            }
        }
    }

    private fun sendUpdate(state: RecorderState?) {
        mListener?.onUpdateReceived(state)
    }

    private fun sendData(mimeType: String, samples: ByteArray, eos: Boolean = false) {
        mListener?.onDataReceived(AudioStream(mimeType = mimeType, data = samples, eos = eos))
    }

    actual fun stopRecording() {
        if (_recorderState.value == RecorderState.RECORDING) {
            // Stop the reading coroutine
            recordingJob?.cancel()
            try {
                audioRecord?.stop()
            } catch (e: IllegalStateException) {
                logE { "AudioRecord stop failed: ${e.message}" }
                if (_recorderState.value != RecorderState.ERROR) {
                    _recorderState.value = RecorderState.ERROR
                }
            } finally {
                audioRecord?.release()
                audioRecord = null
            }

            // Set state to Stopped only if it wasn't an Error that stopped it
            if (_recorderState.value == RecorderState.RECORDING) {
                _recorderState.value = RecorderState.STOPPED
            }

            logD { "Recording stopped." }
            sendUpdate(_recorderState.value)
        }
        else {
            logE { "Not recording or recording already stopped." }
        }
    }

    actual fun isRecording(): Boolean {
        return _recorderState.value == RecorderState.RECORDING
    }

    actual fun getRecordingState(): RecorderState {
        return _recorderState.value
    }

    private fun hasRecordAudioPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }
}

@Composable
actual fun rememberAudioRecorder(): AudioRecorder {
    val context = LocalContext.current
    val audioRecorder = remember { AudioRecorder(context) }

    DisposableEffect(Unit) {
        onDispose {
            if (audioRecorder.isRecording()) {
                audioRecorder.stopRecording()
            }
        }
    }
    return audioRecorder
}