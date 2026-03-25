package com.lenovo.quantum.test

import android.content.ContentProvider
import android.content.ContentValues
import android.content.UriMatcher
import android.database.Cursor
import android.net.Uri
import android.os.ParcelFileDescriptor
import androidx.core.net.toUri
import com.lenovo.quantum.sdk.logging.logD
import java.io.File
import java.io.FileNotFoundException

class PdfDocumentProvider: ContentProvider() {

    companion object {
        const val AUTHORITY = "com.lenovo.quantum.test.pdfdocumentprovider"
        const val DOCUMENTS_DIR_NAME = "pdf_documents"
        const val PDF = 1
        val CONTENT_URI: Uri = "content://$AUTHORITY/$DOCUMENTS_DIR_NAME".toUri()
        private val uriMatcher = UriMatcher(UriMatcher.NO_MATCH).apply {
            addURI(AUTHORITY, "$DOCUMENTS_DIR_NAME/*", PDF)
        }
    }

    override fun delete(
        uri: Uri,
        selection: String?,
        selectionArgs: Array<out String?>?
    ): Int = 0

    override fun getType(uri: Uri): String? = "application/pdf"

    override fun insert(
        uri: Uri,
        values: ContentValues?
    ): Uri? = null

    override fun onCreate(): Boolean = true

    override fun query(
        uri: Uri,
        projection: Array<out String?>?,
        selection: String?,
        selectionArgs: Array<out String?>?,
        sortOrder: String?
    ): Cursor? {
        // Not used for file access, so just return null
        return null
    }

    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<out String?>?
    ): Int {
        // Not used for file access, so just return 0
        return 0
    }

    override fun openFile(uri: Uri, mode: String): ParcelFileDescriptor? {
        logD { "openFile (E)" }
        if (uriMatcher.match(uri) != PDF) throw FileNotFoundException("Unsupported URI: $uri")

        val fileName = uri.lastPathSegment ?: throw FileNotFoundException("Missing file name")
        val docPath = File(context?.filesDir, DOCUMENTS_DIR_NAME)
        val docFile = File(docPath, fileName)

        if (!docFile.exists()) {
            throw FileNotFoundException("File not found: $fileName")
        }

        if (docFile.length() == 0L) {
            throw FileNotFoundException("File with size 0: $fileName")
        }

        if (docFile.exists() && docFile.length() > 0) {
            return ParcelFileDescriptor.open(
                docFile,
                ParcelFileDescriptor.MODE_READ_ONLY
            )
        }
        logD { "openFile (X)" }
        return null
    }
}
