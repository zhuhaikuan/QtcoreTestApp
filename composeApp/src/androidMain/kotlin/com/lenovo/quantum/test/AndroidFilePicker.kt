/*
 * Copyright (C) 2025 Lenovo
 * All Rights Reserved.
 * Lenovo Confidential Restricted.
 */
package com.lenovo.quantum.test

import android.content.Context
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

class AndroidFilePicker(
    private val context: Context
) : FilePicker() {

    private var launcher : ManagedActivityResultLauncher<Array<String>, Uri?>? = null
    private var resultDeferred: CompletableDeferred<FileAttachment?>? = null

    override suspend fun launch() : FileAttachment? {
        logD { "launch (E)" }
        resultDeferred = CompletableDeferred()
        launcher?.launch(getValidMimeTypes().toTypedArray())
        val fileAttachment = resultDeferred?.await()
        logD { "launch (X)" }
        return fileAttachment
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
            val contentUri =
                Uri.withAppendedPath(
                    PdfDocumentProvider.CONTENT_URI,
                    pdfFile.name
                )
            logD { "copyPdfAndGenerateUri (X), contentUri = $contentUri" }
            return contentUri
        }
        logD { "copyPdfAndGenerateUri (X), returning null" }
        return null
    }

    private fun onFilePicked(uri: Uri?) {
        logD { "onFilePicked (E)" }
        if (uri != null) {
            val inputStream = context.contentResolver.openInputStream(uri)
            val mimeType = context.contentResolver.getType(uri) ?: "unknown"

            if (mimeType !in getValidMimeTypes()) {
                logE { "Invalid file type: $mimeType" }
                resultDeferred?.complete(null)
                logD { "onFilePicked (X)" }
                return
            }
            logD { "mimeType: $mimeType" }
            //if (mimeType == "application/pdf") {
            logD { "wrn009: onFilePicked, uri = ${uri.toString()}" }
            val contentUri = copyPdfAndGenerateUri(uri)

            //resultDeferred?.complete(
            //FileAttachment(
            //uri = contentUri.toString(),
            //fileName = getRealName(uri),
            //mimeType = mimeType
            //)
            //)
            //logD { "onFilePicked (X)" }
            //return
            //}

            val bytes = inputStream?.readBytes()
            if (bytes == null) {
                logE { "Error reading file" }
                resultDeferred?.complete(null)
                logD { "onFilePicked (X)" }
                return
            }

            val data = if (shouldConvertToBase64(mimeType)) {
                Base64.encodeToString(bytes, Base64.NO_WRAP)
            } else {
                bytes.toString(Charsets.UTF_8)
            }

            val jsonObject = if (mimeType == "text/plain") {
                try {
                    val tempFile = fileFromContentUri(context, uri)
                    val bufferedReader: BufferedReader = tempFile?.bufferedReader() ?: throw NullPointerException()
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
                base64ToBitmap(data)
            } else {
                null
            }

            data?.let{
                resultDeferred?.complete(
                    FileAttachment(
                        //uri = uri.toString(),
                        uri = contentUri.toString(),
                        fileName = getRealName(uri),
                        mimeType = mimeType,
                        data = bytes,
                        imageBitmap = imageBitmap,
                        jsonObject = jsonObject
                    )
                )
            }
        }
        logD { "onFilePicked (X)" }
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
            contract = ActivityResultContracts.OpenDocument(),
            onResult = { onFilePicked(it) }
        )
    }

}