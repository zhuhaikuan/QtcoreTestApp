/*
 * Copyright (C) 2025 Lenovo
 * All Rights Reserved.
 * Lenovo Confidential Restricted.
 */
package com.lenovo.quantum.test

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import com.lenovo.quantum.sdk.logging.logD
import com.lenovo.quantum.test.client.FileAttachment
import com.lenovo.quantum.sdk.logging.logE
import org.json.JSONObject
import java.io.BufferedReader
import java.io.ByteArrayInputStream
import java.nio.file.Files
import java.util.Base64
import javax.imageio.ImageIO
import javax.swing.JFileChooser

class DesktopFilePicker : FilePicker() {

    private fun decodeBase64ToImageBitmap(base64: String): ImageBitmap? {
        return try {
            val bytes = Base64.getDecoder().decode(base64)
            val bais = ByteArrayInputStream(bytes)
            val bufferedImage = ImageIO.read(bais) ?: return null
            bufferedImage.toComposeImageBitmap()
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun launch(): FileAttachment? {
        val fileChooser = JFileChooser()
        val result = fileChooser.showOpenDialog(null)
        if (result != JFileChooser.APPROVE_OPTION) return null

        val file = fileChooser.selectedFile
        var mimeType = Files.probeContentType(file.toPath())

        if (mimeType == "audio/mpeg") mimeType = "audio/mp3" // equivalent

        if (mimeType !in getValidMimeTypes()) {
            logE { "Invalid file type: $mimeType" }
            return null
        }

        val bytes = file.readBytes()

        val data = if (shouldConvertToBase64(mimeType)) {
            Base64.getEncoder().encodeToString(bytes)
        } else {
            bytes.toString(Charsets.UTF_8)
        }
        val jsonObject = if (mimeType == "text/plain") {
            try {
                val bufferedReader: BufferedReader = file?.bufferedReader() ?: throw NullPointerException()
                val jsonString = bufferedReader.readText()
                logD{"jsonString: $jsonString"}
                JSONObject(jsonString)
            } catch (e: Exception) {
                logE { "error parsing as JSON: $e" }
                null
            }
        } else {
            null
        }

        logD { "jsonObject attached: $jsonObject" }
        val imageBitmap = if (mimeType in MimeTypes.image) {
            decodeBase64ToImageBitmap(data)
        } else {
            null
        }

        return FileAttachment(
            uri = "file://${file.path}",
            fileName = file.name,
            mimeType = mimeType,
            data = bytes,
            imageBitmap = imageBitmap,
            jsonObject = jsonObject
        )
    }

}