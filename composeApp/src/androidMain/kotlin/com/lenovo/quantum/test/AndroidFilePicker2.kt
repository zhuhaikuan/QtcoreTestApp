/*
 * Copyright (C) 2025 Lenovo
 * All Rights Reserved.
 * Lenovo Confidential Restricted.
 */
package com.lenovo.quantum.test

import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.FileUtils
import android.provider.MediaStore
import android.util.Base64
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.lenovo.quantum.sdk.logging.logD
import kotlinx.coroutines.CompletableDeferred
import com.lenovo.quantum.test.client.FileAttachment
import com.lenovo.quantum.sdk.logging.logE
import org.json.JSONObject
import java.io.BufferedReader
import java.io.File
import java.io.FileOutputStream

class AndroidFilePicker2(
    private val context: Context
) : FilePicker() {

    private var launcher : ManagedActivityResultLauncher<Array<String>, List<Uri>>? = null
    private var resultDeferred: CompletableDeferred<List<FileAttachment>?>? = null

    override suspend fun launchForList() : List<FileAttachment>? {
        logD { "launch (E)" }
        resultDeferred = CompletableDeferred()
        launcher?.launch(getValidMimeTypes().toTypedArray())
        val fileAttachments = resultDeferred?.await()
        logD { "launch (X), selected ${fileAttachments?.size ?: 0} files" }
        return fileAttachments
    }

    private fun fileFromContentUri(context: Context, contentUri: Uri): File? {
        try {
            val tempFile = File(context.cacheDir, "temp_file.txt")
            tempFile.createNewFile()
            val oStream = FileOutputStream(tempFile)
            val inputStream = context.contentResolver.openInputStream(contentUri)
            inputStream?.let {
                FileUtils.copy(inputStream, oStream)
            }
            oStream.flush()
            return tempFile
        } catch (e: Exception) {
            logE { "error copying to temp_file.txt" }
            return null
        }
    }

    private fun copyPdfAndGenerateUri(pdfUri: Uri): Uri? {
        logD { "copyPdfAndGenerateUri (E)" }
        val internalStorageDir: File = context.filesDir
        val contentResolver = context.contentResolver
        contentResolver.openInputStream(pdfUri)?.use { input ->
            // Create a new file in your app's internal storage
            val pdfDir = File(internalStorageDir, PdfDocumentProvider.DOCUMENTS_DIR_NAME)
            if (!pdfDir.exists()) {
                pdfDir.mkdirs()
            }
            logD { "pdfDir = ${pdfDir.name}" }
            val pdfFile = File(pdfDir, getRealName(pdfUri))
            logD { "pdfFile = ${pdfFile.name}" }
            FileOutputStream(pdfFile).use { output ->
                input.copyTo(output)
            }
            // Now generate the Content URI
            logD { "generate content URI" }
            val contentUri = Uri.withAppendedPath(PdfDocumentProvider.CONTENT_URI, pdfFile.name)
            logD { "copyPdfAndGenerateUri (X), contentUri = $contentUri" }
            return contentUri
        }
        System.currentTimeMillis()
        logD { "copyPdfAndGenerateUri (X), returning null" }
        return null
    }

    private fun onFilesPicked(uris: List<Uri>?) {
        val startTime = System.currentTimeMillis()
        logE { "###### onFilesPicked (E), selected ${uris?.size ?: 0} files" }

        if (uris.isNullOrEmpty()) {
            resultDeferred?.complete(emptyList())
            logD { "onFilesPicked (X) - no files selected" }
            return
        }

        val fileAttachments = mutableListOf<FileAttachment>()

        for (uri in uris) {
            try {
                val mimeType = context.contentResolver.getType(uri) ?: "unknown"

                if (mimeType !in getValidMimeTypes()) {
                    logE { "Invalid file type: $mimeType for file $uri" }
                    continue // Skip invalid files but continue processing others
                }

                logD { "Processing file: ${getRealName(uri)}, mimeType: $mimeType" }

                // Handle PDF files
                val contentUri = copyPdfAndGenerateUri(uri)

                val jsonObject = if (mimeType == "text/plain") {
                    try {
                        val tempFile = fileFromContentUri(context, uri)
                        val bufferedReader: BufferedReader = tempFile?.bufferedReader() ?: throw NullPointerException()
                        val jsonString = bufferedReader.readText()
                        logD { "jsonString for ${getRealName(uri)}: $jsonString" }
                        JSONObject(jsonString)
                    } catch (e: Exception) {
                        logE { "error parsing as JSON for ${getRealName(uri)}: $e" }
                        null
                    }
                } else {
                    null
                }

                logD { "jsonObject attached for ${getRealName(uri)}: ${jsonObject != null}" }

                val fileAttachment = FileAttachment(
                    uri = contentUri?.toString() ?: uri.toString(),
                    fileName = getRealName(uri),
                    mimeType = mimeType,
                    data = null,
                    imageBitmap = null,
                    jsonObject = null
                )

                fileAttachments.add(fileAttachment)
                logD { "Successfully processed file: ${getRealName(uri)}" }

            } catch (e: Exception) {
                logE { "Error processing file ${getRealName(uri)}: ${e.message}" }
                // Continue with next file even if one fails
            }
        }

        resultDeferred?.complete(fileAttachments)
        logE { "###### onFilesPicked (X) - successfully processed ${fileAttachments.size} files, cost time = ${System.currentTimeMillis() - startTime}" }
    }

    private fun getRealName(uri: Uri): String {
        var fileName = uri.lastPathSegment ?: "unknown"
        val projection = arrayOf(
            MediaStore.Files.FileColumns.TITLE,
            MediaStore.Files.FileColumns.DISPLAY_NAME,
        )
        context.contentResolver.query(
            uri,
            projection,
            null,
            null
        )?.use { cursor ->
            // Should have only one result
            cursor.moveToNext()

            val titleColumn = cursor.getColumnIndexOrThrow(
                MediaStore.Files.FileColumns.TITLE
            )
            val nameColumn = cursor.getColumnIndexOrThrow(
                MediaStore.Files.FileColumns.DISPLAY_NAME
            )
            cursor.getString(titleColumn)?.let { title ->
                fileName = title
            }
            cursor.getString(nameColumn)?.let { name ->
                fileName = name
            }
        }
        return fileName
    }

    private fun base64ToBitmap(base64: String) : ImageBitmap? {
        return try {
            val bytes = Base64.decode(base64, Base64.NO_WRAP)
            val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            bitmap?.asImageBitmap()
        } catch (e: Exception) {
            logE { "Error converting base64 to bitmap: ${e.message}" }
            null
        }
    }

    @Composable
    fun RegisterForActivityResult() {
        launcher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.OpenMultipleDocuments(),
            onResult = { uris -> onFilesPicked(uris) }
        )
    }
}