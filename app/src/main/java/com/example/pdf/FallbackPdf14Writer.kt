package com.example.pdf

import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.nio.charset.StandardCharsets

internal data class PdfKeyValueRow(
    val label: String,
    val value: String
)

internal data class PdfReportSection(
    val title: String,
    val rows: List<PdfKeyValueRow>,
    val emptyMessage: String? = null
)

internal data class PdfReportModel(
    val headerTitle: String,
    val monthSubtitle: String,
    val profileLines: List<String>,
    val sections: List<PdfReportSection>,
    val footerLeftText: String,
    val pageIndicatorFormat: (page: Int, total: Int) -> String
)

internal sealed class PdfBlock {
    data class SectionHeading(val title: String) : PdfBlock()
    data class KeyValue(val label: String, val value: String) : PdfBlock()
    data class EmptyNote(val message: String) : PdfBlock()
    data object Spacer : PdfBlock()
}

internal data class PdfPagePlan(
    val pageNumber: Int,
    val isFirstPage: Boolean,
    val blocks: List<PdfBlock>
)

internal object PdfLayoutPlanner {
    const val PAGE_WIDTH = 595
    const val PAGE_HEIGHT = 842
    const val MARGIN = 40f
    const val FOOTER_SAFE_Y = 792f

    fun planPages(model: PdfReportModel): List<PdfPagePlan> {
        val allBlocks = buildList {
            model.sections.forEachIndexed { index, section ->
                add(PdfBlock.SectionHeading(section.title))
                if (section.rows.isEmpty() && !section.emptyMessage.isNullOrBlank()) {
                    add(PdfBlock.EmptyNote(section.emptyMessage))
                } else {
                    section.rows.forEach { row ->
                        add(PdfBlock.KeyValue(row.label, row.value))
                    }
                }
                if (index < model.sections.lastIndex) {
                    add(PdfBlock.Spacer)
                }
            }
        }

        val pages = mutableListOf<PdfPagePlan>()
        var currentBlocks = mutableListOf<PdfBlock>()
        var pageNumber = 1
        var isFirstPage = true

        val profileBoxHeight = if (model.profileLines.isEmpty()) 0f else (model.profileLines.size * 18f + 20f)
        var y = 112f + if (profileBoxHeight > 0f) profileBoxHeight + 22f else 12f

        fun blockHeight(block: PdfBlock): Float = when (block) {
            is PdfBlock.SectionHeading -> 34f
            is PdfBlock.KeyValue -> 20f
            is PdfBlock.EmptyNote -> 20f
            PdfBlock.Spacer -> 14f
        }

        var idx = 0
        while (idx < allBlocks.size) {
            val block = allBlocks[idx]
            val needed = if (block is PdfBlock.SectionHeading) {
                // Keep heading together with at least its first content row
                val nextHeight = allBlocks.getOrNull(idx + 1)?.let { blockHeight(it) } ?: 0f
                blockHeight(block) + nextHeight
            } else {
                blockHeight(block)
            }

            if (y + needed > FOOTER_SAFE_Y && currentBlocks.isNotEmpty()) {
                pages.add(PdfPagePlan(pageNumber, isFirstPage, currentBlocks))
                pageNumber++
                isFirstPage = false
                currentBlocks = mutableListOf()
                y = 76f
                if (block is PdfBlock.Spacer) {
                    idx++
                    continue
                }
            }

            currentBlocks.add(block)
            y += blockHeight(block)
            idx++
        }

        if (currentBlocks.isNotEmpty() || pages.isEmpty()) {
            pages.add(PdfPagePlan(pageNumber, isFirstPage, currentBlocks))
        }

        return pages
    }
}

/**
 * Standards-compliant pure-Kotlin PDF 1.4 writer (A4 portrait, multi-page, valid xref/trailer/%%EOF).
 * Used as a verified fallback when Android's Skia PdfDocument is unavailable or fails validation
 * (e.g., on buggy OEM drivers or JVM/Robolectric test environments).
 */
internal object FallbackPdf14Writer {

    fun write(model: PdfReportModel, targetFile: File) {
        val pages = PdfLayoutPlanner.planPages(model)
        val totalPages = pages.size

        // Object numbering:
        // 1 = Catalog, 2 = Pages, 3 = Helvetica (Regular), 4 = Helvetica-Bold
        // For each page i (0..totalPages-1):
        //   pageObj = 5 + i * 2
        //   contentObj = 6 + i * 2
        val objects = mutableListOf<Pair<Int, ByteArray>>()
        val pageObjectIds = (0 until totalPages).map { 5 + it * 2 }

        objects.add(1 to "<< /Type /Catalog /Pages 2 0 R >>".toPdfBytes())
        val kidsRef = pageObjectIds.joinToString(" ") { "$it 0 R" }
        objects.add(2 to "<< /Type /Pages /Kids [ $kidsRef ] /Count $totalPages >>".toPdfBytes())
        objects.add(3 to "<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica /Encoding /WinAnsiEncoding >>".toPdfBytes())
        objects.add(4 to "<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica-Bold /Encoding /WinAnsiEncoding >>".toPdfBytes())

        pages.forEachIndexed { index, pagePlan ->
            val pageObjId = 5 + index * 2
            val contentObjId = 6 + index * 2
            val streamBytes = buildPageStream(model, pagePlan, totalPages)

            val pageDict = buildString {
                append("<< /Type /Page /Parent 2 0 R ")
                append("/MediaBox [0 0 ${PdfLayoutPlanner.PAGE_WIDTH} ${PdfLayoutPlanner.PAGE_HEIGHT}] ")
                append("/Resources << /Font << /F1 3 0 R /F2 4 0 R >> >> ")
                append("/Contents $contentObjId 0 R >>")
            }
            objects.add(pageObjId to pageDict.toPdfBytes())

            val streamObj = ByteArrayOutputStream().apply {
                write("<< /Length ${streamBytes.size} >>\nstream\n".toPdfBytes())
                write(streamBytes)
                write("\nendstream".toPdfBytes())
            }.toByteArray()
            objects.add(contentObjId to streamObj)
        }

        val out = ByteArrayOutputStream()
        out.write("%PDF-1.4\n%\u00E2\u00E3\u00CF\u00D3\n".toByteArray(StandardCharsets.ISO_8859_1))

        val offsets = IntArray(objects.size + 1)
        for ((objId, body) in objects) {
            offsets[objId] = out.size()
            out.write("$objId 0 obj\n".toPdfBytes())
            out.write(body)
            out.write("\nendobj\n".toPdfBytes())
        }

        val xrefOffset = out.size()
        val totalObjs = objects.size + 1
        out.write("xref\n0 $totalObjs\n".toPdfBytes())
        out.write("0000000000 65535 f \n".toPdfBytes())
        for (objId in 1..objects.size) {
            val offsetStr = offsets[objId].toString().padStart(10, '0')
            out.write("$offsetStr 00000 n \n".toPdfBytes())
        }
        out.write(
            buildString {
                append("trailer\n")
                append("<< /Size $totalObjs /Root 1 0 R >>\n")
                append("startxref\n")
                append("$xrefOffset\n")
                append("%%EOF\n")
            }.toPdfBytes()
        )

        FileOutputStream(targetFile).use { fos ->
            fos.write(out.toByteArray())
            fos.flush()
            runCatching { fos.fd.sync() }
        }
    }

    private fun buildPageStream(
        model: PdfReportModel,
        plan: PdfPagePlan,
        totalPages: Int
    ): ByteArray {
        val pageWidth = PdfLayoutPlanner.PAGE_WIDTH.toFloat()
        val pageHeight = PdfLayoutPlanner.PAGE_HEIGHT.toFloat()
        val margin = PdfLayoutPlanner.MARGIN
        val sb = StringBuilder()

        // Helper converting top-down y to PDF bottom-up y
        fun pdfY(topDownY: Float): Float = pageHeight - topDownY

        fun drawRect(x: Float, topY: Float, w: Float, h: Float, r: Float, g: Float, b: Float) {
            val bottomY = pageHeight - topY - h
            sb.append(String.format(java.util.Locale.US, "%.3f %.3f %.3f rg\n", r, g, b))
            sb.append(String.format(java.util.Locale.US, "%.2f %.2f %.2f %.2f re f\n", x, bottomY, w, h))
        }

        fun drawLine(x1: Float, topY1: Float, x2: Float, topY2: Float, r: Float, g: Float, b: Float, width: Float = 0.8f) {
            sb.append(String.format(java.util.Locale.US, "%.3f %.3f %.3f RG %.2f w\n", r, g, b, width))
            sb.append(String.format(java.util.Locale.US, "%.2f %.2f m %.2f %.2f l S\n", x1, pdfY(topY1), x2, pdfY(topY2)))
        }

        fun drawText(
            text: String,
            x: Float,
            topBaselineY: Float,
            fontKey: String,
            fontSize: Float,
            r: Float,
            g: Float,
            b: Float
        ) {
            val safe = sanitizeAndEscapePdfText(text)
            sb.append("BT\n")
            sb.append("/$fontKey ${String.format(java.util.Locale.US, "%.1f", fontSize)} Tf\n")
            sb.append(String.format(java.util.Locale.US, "%.3f %.3f %.3f rg\n", r, g, b))
            sb.append(String.format(java.util.Locale.US, "1 0 0 1 %.2f %.2f Tm\n", x, pdfY(topBaselineY)))
            sb.append("($safe) Tj\n")
            sb.append("ET\n")
        }

        // Top brand accent bar (#F83D11)
        drawRect(0f, 0f, pageWidth, 10f, 0.973f, 0.239f, 0.067f)

        var y: Float
        if (plan.isFirstPage) {
            drawText(truncateForPdf(model.headerTitle, 52), margin, 44f, "F2", 19f, 0.145f, 0.094f, 0.067f)
            drawText(truncateForPdf(model.monthSubtitle, 70), margin, 64f, "F1", 11f, 0.431f, 0.345f, 0.302f)
            drawLine(margin, 76f, pageWidth - margin, 76f, 0.922f, 0.855f, 0.816f, 1.0f)
            y = 94f

            if (model.profileLines.isNotEmpty()) {
                val boxHeight = model.profileLines.size * 18f + 16f
                drawRect(margin, y, pageWidth - margin * 2f, boxHeight, 0.996f, 0.961f, 0.937f)
                var py = y + 20f
                for (line in model.profileLines) {
                    drawText(truncateForPdf(line, 76), margin + 12f, py, "F2", 10.5f, 0.145f, 0.094f, 0.067f)
                    py += 18f
                }
                y += boxHeight + 22f
            } else {
                y += 12f
            }
        } else {
            drawText(truncateForPdf("${model.headerTitle} — ${model.monthSubtitle}", 76), margin, 38f, "F2", 11f, 0.431f, 0.345f, 0.302f)
            drawLine(margin, 48f, pageWidth - margin, 48f, 0.922f, 0.855f, 0.816f, 0.8f)
            y = 74f
        }

        for (block in plan.blocks) {
            when (block) {
                is PdfBlock.SectionHeading -> {
                    drawText(truncateForPdf(block.title, 62), margin, y, "F2", 12.5f, 0.851f, 0.220f, 0.055f)
                    y += 8f
                    drawLine(margin, y, pageWidth - margin, y, 0.945f, 0.847f, 0.796f, 0.9f)
                    y += 18f
                }
                is PdfBlock.KeyValue -> {
                    val valueStr = truncateForPdf(block.value, 28)
                    val labelStr = truncateForPdf(block.label, 52)
                    val estimatedValueWidth = valueStr.length * 5.8f
                    val valueX = (pageWidth - margin - 8f - estimatedValueWidth).coerceAtLeast(pageWidth * 0.56f)
                    drawText(labelStr, margin + 8f, y, "F1", 10.5f, 0.251f, 0.196f, 0.169f)
                    drawText(valueStr, valueX, y, "F2", 10.5f, 0.118f, 0.078f, 0.055f)
                    y += 19f
                }
                is PdfBlock.EmptyNote -> {
                    drawText(truncateForPdf(block.message, 74), margin + 8f, y, "F1", 10f, 0.471f, 0.400f, 0.365f)
                    y += 19f
                }
                PdfBlock.Spacer -> {
                    y += 12f
                }
            }
        }

        // Consistent footer on every page
        drawLine(margin, pageHeight - 36f, pageWidth - margin, pageHeight - 36f, 0.922f, 0.855f, 0.816f, 0.8f)
        drawText(truncateForPdf(model.footerLeftText, 55), margin, pageHeight - 20f, "F1", 9.5f, 0.471f, 0.400f, 0.365f)
        val pageLabel = model.pageIndicatorFormat(plan.pageNumber, totalPages)
        val estPageLabelWidth = pageLabel.length * 5.2f
        drawText(
            truncateForPdf(pageLabel, 24),
            (pageWidth - margin - estPageLabelWidth).coerceAtLeast(pageWidth * 0.72f),
            pageHeight - 20f,
            "F1",
            9.5f,
            0.471f,
            0.400f,
            0.365f
        )

        return sb.toString().toPdfBytes()
    }

    private fun truncateForPdf(text: String, maxChars: Int): String {
        val clean = text.replace('\n', ' ').replace('\r', ' ').trim()
        return if (clean.length <= maxChars) clean else clean.take(maxChars - 3).trimEnd() + "..."
    }

    private fun sanitizeAndEscapePdfText(input: String): String {
        val sb = StringBuilder(input.length)
        for (ch in input) {
            val mapped = when {
                ch.code in 0x20..0x7E -> ch
                ch == '\u2022' -> '-'
                ch == '\u2014' || ch == '\u2013' -> '-'
                else -> '?'
            }
            when (mapped) {
                '\\' -> sb.append("\\\\")
                '(' -> sb.append("\\(")
                ')' -> sb.append("\\)")
                else -> sb.append(mapped)
            }
        }
        return sb.toString()
    }

    private fun String.toPdfBytes(): ByteArray = toByteArray(StandardCharsets.ISO_8859_1)
}
