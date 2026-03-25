/*
 * Copyright (C) 2025 Lenovo
 * All Rights Reserved.
 * Lenovo Confidential Restricted.
 */
package com.lenovo.quantum.test.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import chaintech.videoplayer.host.MediaPlayerHost
import chaintech.videoplayer.model.AudioFile
import chaintech.videoplayer.model.AudioPlayerConfig
import chaintech.videoplayer.model.VideoPlayerConfig
import chaintech.videoplayer.ui.audio.AudioPlayerComposable
import chaintech.videoplayer.ui.video.VideoPlayerComposable
import coil3.compose.AsyncImage
import com.lenovo.quantum.test.MediaItem
import com.lenovo.quantum.test.MediaType

/**
 * Loads media (image, audio and video) based on its URI.
 * The media can be located on the device or on the web.
 * Works for mobile and desktop.
 */
@Composable
fun MediaSurface(
    mediaItem: MediaItem,
    showFileName: Boolean = true
) {
    Column(
        modifier = Modifier
            .padding(16.dp)
            .border(width = 1.dp, Color(0xFF333333), shape = RoundedCornerShape(8.dp))
            .padding(8.dp)
    ) {
        when (mediaItem.type) {
            MediaType.IMAGE -> {
                if (showFileName) { Text(text = mediaItem.fileName) }
                val model : Any? = when {
                    mediaItem.binary?.isNotEmpty() == true -> mediaItem.binary
                    mediaItem.uri.isNotBlank() -> mediaItem.uri
                    else -> null
                }
                AsyncImage(
                    modifier = Modifier
                        .height(200.dp),
                    model = model,
                    contentDescription = null
                )
            }
            MediaType.AUDIO -> {
                val audioFilesArray = listOf(
                    AudioFile(
                        audioUrl = mediaItem.uri,
                        audioTitle = "",
                        thumbnailUrl = ""
                    )
                )
                val playerHost = remember {
                    MediaPlayerHost(
                        mediaUrl = audioFilesArray[0].audioUrl,
                        isLooping = false
                    )
                }
                playerHost.pause()

                if (showFileName) { Text(text = mediaItem.fileName) }
                AudioPlayerComposable(
                    modifier = Modifier
                        .width(400.dp)
                        .height(100.dp),
                    audios = audioFilesArray,
                    playerHost = playerHost,
                    audioPlayerConfig = AudioPlayerConfig(
                        controlsBottomPadding = 0.dp
                    )
                )
            }
            MediaType.VIDEO -> {
                val playerHost = remember {
                    MediaPlayerHost(mediaUrl = mediaItem.uri)
                }
                playerHost.pause()

                if (showFileName) { Text(text = mediaItem.fileName) }
                VideoPlayerComposable(
                    modifier = Modifier
                        .width(400.dp)
                        .height(300.dp),
                    playerHost = playerHost,
                    playerConfig = VideoPlayerConfig(
                        isZoomEnabled = false,
                        isAutoHideControlEnabled = false,
                        isSpeedControlEnabled = false,
                        isFullScreenEnabled = false,
                        isScreenLockEnabled = false,
                        isScreenResizeEnabled = false
                    )
                )
            }
            else -> {
                Text(text = mediaItem.fileName.ifBlank { "Unknown media type" })
            }
        }
    }
}