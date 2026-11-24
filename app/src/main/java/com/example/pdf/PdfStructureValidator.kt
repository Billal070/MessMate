package com.example.pdf

import java.io.File
import java.io.RandomAccessFile
import java.nio.charset.StandardCharsets

data class PdfValidationResult(
    val isValid: Boolean,
    val pageCount: Int = 0,
    val sizeBytes: Long = 0L,
    val errorMessage: String? = null
)

/**
 * Validates that a generated PDF file conforms to core PDF binary structure requirements:
 * - Non-empty regular file
 * - Valid %PDF-1.x / %PDF-2.x magic header
 * - Cross-reference table/stream (xref / /Type /XRef) + startxref
 * - Properly terminated with %%EOF near the end of the file
 * - At least one /Type /Page object
 */
object PdfStructureValidator {

    private const val MIN_VALID_PDF_BYTES = 128L
    private const val TAIL_SCAN_BYTES = 2048
    private val PAGE_OBJECT_REGEX = Regex("""/Type\s*/Page(?![a-zA-Z])""")
    private val PDF_HEADER_REGEX = Regex("""^%PDF-[12]\.\d""")

    fun validate(file: File): PdfValidationResult {
        if (!file.exists() || !file.isFile) {
            return PdfValidationResult(isValid = false, errorMessage = "PDF file does not exist")
        }

        val length = file.length()
        if (length < MIN_VALID_PDF_BYTES) {
            return PdfValidationResult(
                isValid = false,
                sizeBytes = length,
                errorMessage = "PDF file is empty or truncated ($length bytes)"
            )
        }

        return runCatching {
            // Read header bytes
            val headerBytes = ByteArray(16)
            RandomAccessFile(file, "r").use { raf ->
                val readHeader = raf.read(headerBytes)
                val headerText = String(headerBytes, 0, readHeader.coerceAtLeast(0), StandardCharsets.ISO_8859_1)
                if (!PDF_HEADER_REGEX.containsMatchIn(headerText)) {
                    return PdfValidationResult(
                        isValid = false,
                        sizeBytes = length,
                        errorMessage = "Missing valid %PDF- header"
                    )
                }

                // Read tail bytes for %%EOF and startxref
                val tailSize = minOf(length, TAIL_SCAN_BYTES.toLong()).toInt()
                val tailBytes = ByteArray(tailSize)
                raf.seek(length - tailSize)
                raf.readFully(tailBytes)
                val tailText = String(tailBytes, StandardCharsets.ISO_8859_1)

                val eofIndex = tailText.lastIndexOf("%%EOF")
                if (eofIndex < 0) {
                    return PdfValidationResult(
                        isValid = false,
                        sizeBytes = length,
                        errorMessage = "Missing %%EOF terminator"
                    )
                }
                val startXrefIndex = tailText.lastIndexOf("startxref")
                if (startXrefIndex < 0 || startXrefIndex > eofIndex) {
                    return PdfValidationResult(
                        isValid = false,
                        sizeBytes = length,
                        errorMessage = "Missing valid startxref pointer before %%EOF"
                    )
                }
            }

            // Inspect structure tokens across the PDF payload (monthly reports are compact, < 5MB)
            val contentText = String(file.readBytes(), StandardCharsets.ISO_8859_1)
            val hasXref = contentText.contains("\nxref") ||
                contentText.contains("\rxref") ||
                contentText.contains("/Type /XRef") ||
                contentText.contains("/Type/XRef")
            if (!hasXref) {
                return PdfValidationResult(
                    isValid = false,
                    sizeBytes = length,
                    errorMessage = "Missing PDF cross-reference table or stream"
                )
            }

            val pageCount = PAGE_OBJECT_REGEX.findAll(contentText).count()
            if (pageCount < 1) {
                return PdfValidationResult(
                    isValid = false,
                    sizeBytes = length,
                    errorMessage = "PDF contains no /Type /Page objects"
                )
            }

            PdfValidationResult(
                isValid = true,
                pageCount = pageCount,
                sizeBytes = length
            )
        }.getOrElse { error ->
            PdfValidationResult(
                isValid = false,
                sizeBytes = length,
                errorMessage = error.message ?: "PDF structure validation error"
            )
        }
    }
}
