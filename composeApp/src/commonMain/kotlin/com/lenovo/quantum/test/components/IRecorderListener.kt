package com.lenovo.quantum.test.components

import com.lenovo.quantum.test.client.AudioStream

interface IRecorderListener {
    fun onUpdateReceived(state: RecorderState?)

    fun onDataReceived(data: AudioStream)
}