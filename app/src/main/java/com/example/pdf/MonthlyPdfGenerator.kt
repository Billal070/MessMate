package com.example.pdf

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.example.R
import com.example.data.local.ProfileEntity
import com.example.data.repository.MonthlyMealSummary
import com.example.data.repository.MonthlyRiceSummary
import com.example.data.repository.StudyStatistics
import com.example.util.Formatters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.time.YearMonth
import java.util.Locale

data class MonthlyReportData(
    val yearMonth: YearMonth,
    val profile: ProfileEntity,
    val mealSummary: MonthlyMealSummary,
    val riceSummary: MonthlyRiceSummary,
    val studyStats: StudyStatistics
)

class MonthlyPdfGenerator {

    suspend fun generateMonthlyReport(
        localizedContext: Context,
        report: MonthlyReportData,
        locale: Locale
    ): File = withContext(Dispatchers.IO) {
        // Store in persistent app filesDir/reports/ (declared in file_paths.xml as saved_reports)
        // so the OS cache cleaner never evicts a report between generation and SAF export/open.
        val reportsDir = File(localizedContext.filesDir, "reports").apply { mkdirs() }
        val fileName = PdfFileActions.ensurePdfExtension("MessMate_Report_${report.yearMonth}.pdf")
        val outFile = File(reportsDir, fileName)
        val tempFile = File(reportsDir, "$fileName.tmp")

        val reportModel = buildReportModel(localizedContext, report, locale)

        try {
            if (tempFile.exists()) tempFile.delete()

            // Primary renderer: Android Skia PdfDocument (supports full Unicode / Bangla glyph embedding)
            val androidPdfOk = runCatching {
                renderWithAndroidPdfDocument(reportModel, tempFile)
                PdfStructureValidator.validate(tempFile).isValid
            }.getOrDefault(false)

            // Verified fallback: pure-Kotlin standards-compliant PDF 1.4 writer if Skia fails or outputs invalid PDF
            if (!androidPdfOk) {
                if (tempFile.exists()) tempFile.delete()
                FallbackPdf14Writer.write(reportModel, tempFile)
            }

            val finalValidation = PdfStructureValidator.validate(tempFile)
            if (!finalValidation.isValid) {
                tempFile.delete()
                throw IOException(finalValidation.errorMessage ?: "Generated PDF failed integrity check")
            }

            // Atomic commit: never expose a partially written or corrupted PDF at outFile
            if (outFile.exists()) outFile.delete()
            if (!tempFile.renameTo(outFile)) {
                tempFile.copyTo(outFile, overwrite = true)
                tempFile.delete()
            }

            val committedValidation = PdfStructureValidator.validate(outFile)
            if (!committedValidation.isValid) {
                outFile.delete()
                throw IOException(committedValidation.errorMessage ?: "Saved PDF failed integrity check")
            }

            outFile
        } catch (t: Throwable) {
            if (tempFile.exists()) tempFile.delete()
            throw t
        }
    }

    private fun buildReportModel(
        localizedContext: Context,
        report: MonthlyReportData,
        locale: Locale
    ): PdfReportModel {
        val profileLines = buildList {
            add("${localizedContext.getString(R.string.pdf_student_name)}: ${report.profile.name}")
            if (report.profile.messName.isNotBlank()) {
                add("${localizedContext.getString(R.string.pdf_mess_name)}: ${report.profile.messName}")
            }
            if (report.profile.institutionName.isNotBlank()) {
                add("${localizedContext.getString(R.string.pdf_institution)}: ${report.profile.institutionName}")
            }
            val deptSem = listOf(report.profile.department, report.profile.semester)
                .filter { it.isNotBlank() }
                .joinToString(" • ")
            if (deptSem.isNotBlank()) {
                add(deptSem)
            }
        }

        val sections = listOf(
            PdfReportSection(
                title = localizedContext.getString(R.string.pdf_meal_summary),
                rows = listOf(
                    PdfKeyValueRow(
                        localizedContext.getString(R.string.records_counted_meals),
                        report.mealSummary.countedMeals.toString()
                    ),
                    PdfKeyValueRow(
                        "  ${localizedContext.getString(R.string.meal_breakfast)} / ${localizedContext.getString(R.string.meal_lunch)} / ${localizedContext.getString(R.string.meal_dinner)}",
                        "${report.mealSummary.breakfastCounted} / ${report.mealSummary.lunchCounted} / ${report.mealSummary.dinnerCounted}"
                    ),
                    PdfKeyValueRow(
                        localizedContext.getString(R.string.records_skipped_meals),
                        report.mealSummary.skippedMeals.toString()
                    ),
                    PdfKeyValueRow(
                        localizedContext.getString(R.string.records_off_days),
                        report.mealSummary.offDays.toString()
                    )
                )
            ),
            PdfReportSection(
                title = localizedContext.getString(R.string.pdf_rice_summary),
                rows = listOf(
                    PdfKeyValueRow(
                        localizedContext.getString(R.string.pdf_rice_contribution),
                        Formatters.formatKg(report.riceSummary.totalAddedKg)
                    ),
                    PdfKeyValueRow(
                        localizedContext.getString(R.string.pdf_rice_consumption),
                        Formatters.formatKg(report.riceSummary.totalConsumedKg)
                    ),
                    PdfKeyValueRow(
                        localizedContext.getString(R.string.pdf_rice_adjustments),
                        Formatters.formatSignedKg(report.riceSummary.totalAdjustmentsKg)
                    ),
                    PdfKeyValueRow(
                        localizedContext.getString(R.string.pdf_rice_remaining),
                        Formatters.formatKg(report.riceSummary.remainingStockKg)
                    )
                )
            ),
            PdfReportSection(
                title = localizedContext.getString(R.string.pdf_study_summary),
                rows = listOf(
                    PdfKeyValueRow(
                        localizedContext.getString(R.string.records_total_study),
                        Formatters.formatDurationHoursMinutes(report.studyStats.monthlySeconds)
                    ),
                    PdfKeyValueRow(
                        localizedContext.getString(R.string.study_stats_top_subject),
                        report.studyStats.mostStudiedSubject ?: localizedContext.getString(R.string.label_none)
                    ),
                    PdfKeyValueRow(
                        localizedContext.getString(R.string.study_stats_streak),
                        localizedContext.getString(R.string.study_stats_streak_days, report.studyStats.currentStreakDays)
                    )
                )
            ),
            PdfReportSection(
                title = localizedContext.getString(R.string.pdf_subject_report),
                rows = report.studyStats.subjectBreakdown.map { item ->
                    val subjectTitle = if (item.subjectCode.isNotBlank()) {
                        "${item.subjectName} (${item.subjectCode})"
                    } else {
                        item.subjectName
                    }
                    PdfKeyValueRow(subjectTitle, Formatters.formatDurationSeconds(item.totalSeconds))
                },
                emptyMessage = localizedContext.getString(R.string.study_history_empty)
            )
        )

        return PdfReportModel(
            headerTitle = localizedContext.getString(R.string.pdf_header_title),
            monthSubtitle = "${localizedContext.getString(R.string.pdf_month)}: ${Formatters.formatMonthYear(report.yearMonth, locale)}",
            profileLines = profileLines,
            sections = sections,
            footerLeftText = localizedContext.getString(R.string.pdf_generated_offline),
            pageIndicatorFormat = { page, total ->
                localizedContext.getString(R.string.records_page_indicator, page, total)
            }
        )
    }

    private fun renderWithAndroidPdfDocument(model: PdfReportModel, targetFile: File) {
        val pageWidth = PdfLayoutPlanner.PAGE_WIDTH
        val pageHeight = PdfLayoutPlanner.PAGE_HEIGHT
        val margin = PdfLayoutPlanner.MARGIN
        val contentWidth = pageWidth - margin * 2f

        val pages = PdfLayoutPlanner.planPages(model)
        val totalPages = pages.size

        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#251811")
            textSize = 20f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        val subtitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#6E584D")
            textSize = 11f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
        }
        val sectionPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#D9380E")
            textSize = 13f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#40322B")
            textSize = 11f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
        }
        val valuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#1E140E")
            textSize = 11f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        val dividerPaint = Paint().apply {
            color = Color.parseColor("#EBDAD0")
            strokeWidth = 1f
        }
        val headerBarPaint = Paint().apply {
            color = Color.parseColor("#F83D11")
        }
        val boxBgPaint = Paint().apply {
            color = Color.parseColor("#FFF5EF")
        }
        val boxBorderPaint = Paint().apply {
            color = Color.parseColor("#F6D6C7")
            style = Paint.Style.STROKE
            strokeWidth = 1f
        }

        val document = PdfDocument()
        var activePage: PdfDocument.Page? = null
        try {
            for (plan in pages) {
                val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, plan.pageNumber).create()
                val page = document.startPage(pageInfo)
                activePage = page
                val canvas: Canvas = page.canvas

                // Top warm brand bar
                canvas.drawRect(0f, 0f, pageWidth.toFloat(), 10f, headerBarPaint)

                var y: Float
                if (plan.isFirstPage) {
                    y = 44f
                    canvas.drawText(ellipsizeText(model.headerTitle, titlePaint, contentWidth), margin, y, titlePaint)
                    y += 20f
                    canvas.drawText(ellipsizeText(model.monthSubtitle, subtitlePaint, contentWidth), margin, y, subtitlePaint)
                    y += 14f
                    canvas.drawLine(margin, y, pageWidth - margin, y, dividerPaint)
                    y += 16f

                    if (model.profileLines.isNotEmpty()) {
                        val boxHeight = model.profileLines.size * 18f + 16f
                        canvas.drawRoundRect(margin, y, pageWidth - margin, y + boxHeight, 8f, 8f, boxBgPaint)
                        canvas.drawRoundRect(margin, y, pageWidth - margin, y + boxHeight, 8f, 8f, boxBorderPaint)
                        var py = y + 20f
                        for (line in model.profileLines) {
                            canvas.drawText(
                                ellipsizeText(line, valuePaint, contentWidth - 24f),
                                margin + 12f,
                                py,
                                valuePaint
                            )
                            py += 18f
                        }
                        y += boxHeight + 22f
                    } else {
                        y += 10f
                    }
                } else {
                    canvas.drawText(
                        ellipsizeText("${model.headerTitle} — ${model.monthSubtitle}", subtitlePaint, contentWidth),
                        margin,
                        38f,
                        subtitlePaint
                    )
                    canvas.drawLine(margin, 48f, pageWidth - margin, 48f, dividerPaint)
                    y = 74f
                }

                for (block in plan.blocks) {
                    when (block) {
                        is PdfBlock.SectionHeading -> {
                            canvas.drawText(ellipsizeText(block.title, sectionPaint, contentWidth), margin, y, sectionPaint)
                            y += 8f
                            canvas.drawLine(margin, y, pageWidth - margin, y, dividerPaint)
                            y += 18f
                        }
                        is PdfBlock.KeyValue -> {
                            val maxValWidth = contentWidth * 0.42f
                            val valText = ellipsizeText(block.value, valuePaint, maxValWidth)
                            val valWidth = valuePaint.measureText(valText)
                            val maxLabelWidth = (contentWidth - valWidth - 28f).coerceAtLeast(contentWidth * 0.45f)
                            val labelText = ellipsizeText(block.label, labelPaint, maxLabelWidth)
                            canvas.drawText(labelText, margin + 8f, y, labelPaint)
                            canvas.drawText(valText, pageWidth - margin - valWidth - 8f, y, valuePaint)
                            y += 19f
                        }
                        is PdfBlock.EmptyNote -> {
                            canvas.drawText(
                                ellipsizeText(block.message, subtitlePaint, contentWidth - 16f),
                                margin + 8f,
                                y,
                                subtitlePaint
                            )
                            y += 19f
                        }
                        PdfBlock.Spacer -> {
                            y += 12f
                        }
                    }
                }

                // Footer on every page
                canvas.drawLine(margin, pageHeight - 36f, pageWidth - margin, pageHeight - 36f, dividerPaint)
                val pageLabel = model.pageIndicatorFormat(plan.pageNumber, totalPages)
                val pageLabelWidth = subtitlePaint.measureText(pageLabel)
                canvas.drawText(
                    ellipsizeText(model.footerLeftText, subtitlePaint, contentWidth - pageLabelWidth - 16f),
                    margin,
                    pageHeight - 20f,
                    subtitlePaint
                )
                canvas.drawText(
                    pageLabel,
                    pageWidth - margin - pageLabelWidth,
                    pageHeight - 20f,
                    subtitlePaint
                )

                document.finishPage(page)
                activePage = null
            }

            FileOutputStream(targetFile).use { fos ->
                document.writeTo(fos)
                fos.flush()
                runCatching { fos.fd.sync() }
            }
        } finally {
            activePage?.let { page -> runCatching { document.finishPage(page) } }
            runCatching { document.close() }
        }
    }

    private fun ellipsizeText(text: String, paint: Paint, maxWidth: Float): String {
        val clean = text.replace('\n', ' ').replace('\r', ' ').trim()
        if (clean.isEmpty() || maxWidth <= 0f || paint.measureText(clean) <= maxWidth) {
            return clean
        }
        val ellipsis = "..."
        val ellipsisWidth = paint.measureText(ellipsis)
        if (ellipsisWidth >= maxWidth) return ellipsis
        val count = paint.breakText(clean, true, maxWidth - ellipsisWidth, null)
        return if (count <= 0) ellipsis else clean.substring(0, count).trimEnd() + ellipsis
    }
}
