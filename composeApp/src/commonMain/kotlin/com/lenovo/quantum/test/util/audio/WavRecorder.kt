/*
 * Copyright (C) 2025 Lenovo
 * All Rights Reserved.
 * Lenovo Confidential Restricted.
 */
package com.lenovo.quantum.test.util.audio

import com.lenovo.quantum.sdk.logging.logE

// Configuration
const val WAV_SAMPLE_RATE = 24000
const val WAV_CHANNELS = 1 // Mono
const val WAV_BITS_PER_SAMPLE = 16

fun createWavFileData(rawPcmData: ByteArray): ByteArray {
    val totalAudioLen = rawPcmData.size
    val totalDataLen = totalAudioLen + 36 // Size of the RIFF chunk (everything after "RIFF" and its size field)

    val byteRate = (WAV_SAMPLE_RATE * WAV_CHANNELS * WAV_BITS_PER_SAMPLE / 8)
    val blockAlign = (WAV_CHANNELS * WAV_BITS_PER_SAMPLE / 8)

    val headerSize = 44
    val wavFileBytes = ByteArray(headerSize + rawPcmData.size)

    // Helper to write Int in Little Endian
    fun putIntLE(value: Int, dest: ByteArray, offset: Int) {
        dest[offset] = (value and 0xFF).toByte()
        dest[offset + 1] = ((value shr 8) and 0xFF).toByte()
        dest[offset + 2] = ((value shr 16) and 0xFF).toByte()
        dest[offset + 3] = ((value shr 24) and 0xFF).toByte()
    }

    // Helper to write Short in Little Endian
    fun putShortLE(value: Short, dest: ByteArray, offset: Int) {
        dest[offset] = (value.toInt() and 0xFF).toByte()
        dest[offset + 1] = ((value.toInt() shr 8) and 0xFF).toByte()
    }

    // RIFF Header
    "RIFF".encodeToByteArray().copyInto(wavFileBytes, 0)
    putIntLE(totalDataLen, wavFileBytes, 4) // ChunkSize
    "WAVE".encodeToByteArray().copyInto(wavFileBytes, 8)

    // FMT Subchunk
    "fmt ".encodeToByteArray().copyInto(wavFileBytes, 12)
    putIntLE(16, wavFileBytes, 16) // Subchunk1Size for PCM
    putShortLE(1.toShort(), wavFileBytes, 20) // AudioFormat (1 for PCM)
    putShortLE(WAV_CHANNELS.toShort(), wavFileBytes, 22) // NumChannels
    putIntLE(WAV_SAMPLE_RATE, wavFileBytes, 24) // SampleRate
    putIntLE(byteRate, wavFileBytes, 28) // ByteRate
    putShortLE(blockAlign.toShort(), wavFileBytes, 32) // BlockAlign
    putShortLE(WAV_BITS_PER_SAMPLE.toShort(), wavFileBytes, 34) // BitsPerSample

    // DATA Subchunk
    "data".encodeToByteArray().copyInto(wavFileBytes, 36)
    putIntLE(totalAudioLen, wavFileBytes, 40) // Subchunk2Size (size of rawPcmData)

    // Copy the raw PCM data
    rawPcmData.copyInto(wavFileBytes, headerSize)

    return wavFileBytes
}

suspend fun saveRawAudioToWavFile(
    rawPcmData: ByteArray,
    outputFilePath: String
): Boolean {
    if (rawPcmData.isEmpty()) {
        logE { "The Raw PCM data is empty" }
        return false
    }
    val wavFileData = createWavFileData(rawPcmData)

    return try {
        writeBytesToPlatformFile(outputFilePath, wavFileData)
    } catch (e: Exception) {
        logE { "Error writing WAV file to $outputFilePath: ${e.message}" }
        false
    }
}