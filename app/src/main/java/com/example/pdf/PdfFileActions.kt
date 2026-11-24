package com.example.pdf

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException

/**
 * Reliable, verified file operations for exporting, opening, and sharing generated PDF reports.
 */
object PdfFileActions {

    const val PDF_MIME_TYPE = "application/pdf"

    fun ensurePdfExtension(fileName: String): String {
        val trimmed = fileName.trim().ifEmpty { "MessMate_Report.pdf" }
        return if (trimmed.lowercase().endsWith(".pdf")) trimmed else "$trimmed.pdf"
    }

    /**
     * Copies a validated PDF file to a SAF destination URI using write-truncate mode ("wt")
     * so existing files are never corrupted with leftover trailing bytes, flushes completely,
     * and verifies exact byte count written.
     */
    suspend fun copyPdfToUri(
        context: Context,
        sourceFile: File,
        destinationUri: Uri
    ): Result<Long> = withContext(Dispatchers.IO) {
        runCatching {
            val validation = PdfStructureValidator.validate(sourceFile)
            if (!validation.isValid) {
                throw IOException(validation.errorMessage ?: "Generated PDF failed validation")
            }

            val expectedSize = sourceFile.length()
            val resolver = context.contentResolver
            val outputStream = runCatching { resolver.openOutputStream(destinationUri, "wt") }.getOrNull()
                ?: resolver.openOutputStream(destinationUri)
                ?: throw IOException("Unable to open destination output stream")

            val bytesCopied = outputStream.use { out ->
                sourceFile.inputStream().buffered().use { input ->
                    val copied = input.copyTo(out, bufferSize = 8192)
                    out.flush()
                    copied
                }
            }

            if (bytesCopied <= 0L || bytesCopied != expectedSize) {
                throw IOException("Incomplete PDF copy: wrote $bytesCopied of $expectedSize bytes")
            }
            bytesCopied
        }
    }

    fun getShareableUri(context: Context, file: File): Uri =
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)

    fun buildOpenPdfIntent(context: Context, file: File): Intent {
        val validation = PdfStructureValidator.validate(file)
        if (!validation.isValid) {
            throw IOException(validation.errorMessage ?: "Invalid PDF file")
        }
        val uri = getShareableUri(context, file)
        return Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, PDF_MIME_TYPE)
            clipData = ClipData.newUri(context.contentResolver, file.name, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
        }
    }

    fun buildSharePdfIntent(context: Context, file: File): Intent {
        val validation = PdfStructureValidator.validate(file)
        if (!validation.isValid) {
            throw IOException(validation.errorMessage ?: "Invalid PDF file")
        }
        val uri = getShareableUri(context, file)
        return Intent(Intent.ACTION_SEND).apply {
            type = PDF_MIME_TYPE
            putExtra(Intent.EXTRA_STREAM, uri)
            clipData = ClipData.newUri(context.contentResolver, file.name, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }
}
