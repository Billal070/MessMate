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
        val reportsDir = File(localizedContext.cacheDir, "reports").apply { mkdirs() }
        val fileName = "MessMate_Report_${report.yearMonth}.pdf"
        val outFile = File(reportsDir, fileName)

        val document = PdfDocument()
        val pageWidth = 595 // A4 @ 72dpi
        val pageHeight = 842
        val margin = 40f
        var pageNumber = 1
        var page = document.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create())
        var canvas: Canvas = page.canvas

        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#101827")
            textSize = 20f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        val subtitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#475569")
            textSize = 11f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
        }
        val sectionPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#1E3A8A")
            textSize = 13f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#334155")
            textSize = 11f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
        }
        val valuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#0F172A")
            textSize = 11f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        val dividerPaint = Paint().apply {
            color = Color.parseColor("#CBD5E1")
            strokeWidth = 1f
        }
        val headerBarPaint = Paint().apply {
            color = Color.parseColor("#101827")
        }
        val boxBgPaint = Paint().apply {
            color = Color.parseColor("#F1F5F9")
        }

        var y = 0f

        // Top accent bar
        canvas.drawRect(0f, 0f, pageWidth.toFloat(), 12f, headerBarPaint)
        y = 48f

        // Header
        canvas.drawText(localizedContext.getString(R.string.pdf_header_title), margin, y, titlePaint)
        y += 20f
        canvas.drawText(
            "${localizedContext.getString(R.string.pdf_month)}: ${Formatters.formatMonthYear(report.yearMonth, locale)}",
            margin,
            y,
            subtitlePaint
        )
        y += 18f
        canvas.drawLine(margin, y, pageWidth - margin, y, dividerPaint)
        y += 20f

        // Profile info box
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
        val boxHeight = profileLines.size * 18f + 16f
        canvas.drawRoundRect(margin, y, pageWidth - margin, y + boxHeight, 8f, 8f, boxBgPaint)
        var py = y + 20f
        for (line in profileLines) {
            canvas.drawText(line, margin + 12f, py, valuePaint)
            py += 18f
        }
        y += boxHeight + 24f

        fun ensureSpace(needed: Float) {
            if (y + needed > pageHeight - 50f) {
                document.finishPage(page)
                pageNumber++
                page = document.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create())
                canvas = page.canvas
                y = 48f
            }
        }

        fun drawKeyValueRow(label: String, value: String) {
            ensureSpace(20f)
            canvas.drawText(label, margin + 8f, y, labelPaint)
            canvas.drawText(value, pageWidth - margin - valuePaint.measureText(value) - 8f, y, valuePaint)
            y += 18f
        }

        // Meal Summary
        ensureSpace(120f)
        canvas.drawText(localizedContext.getString(R.string.pdf_meal_summary), margin, y, sectionPaint)
        y += 8f
        canvas.drawLine(margin, y, pageWidth - margin, y, dividerPaint)
        y += 18f
        drawKeyValueRow(localizedContext.getString(R.string.records_counted_meals), report.mealSummary.countedMeals.toString())
        drawKeyValueRow(
            "  ${localizedContext.getString(R.string.meal_breakfast)} / ${localizedContext.getString(R.string.meal_lunch)} / ${localizedContext.getString(R.string.meal_dinner)}",
            "${report.mealSummary.breakfastCounted} / ${report.mealSummary.lunchCounted} / ${report.mealSummary.dinnerCounted}"
        )
        drawKeyValueRow(localizedContext.getString(R.string.records_skipped_meals), report.mealSummary.skippedMeals.toString())
        drawKeyValueRow(localizedContext.getString(R.string.records_off_days), report.mealSummary.offDays.toString())
        y += 14f

        // Rice Summary
        ensureSpace(120f)
        canvas.drawText(localizedContext.getString(R.string.pdf_rice_summary), margin, y, sectionPaint)
        y += 8f
        canvas.drawLine(margin, y, pageWidth - margin, y, dividerPaint)
        y += 18f
        drawKeyValueRow(localizedContext.getString(R.string.pdf_rice_contribution), Formatters.formatKg(report.riceSummary.totalAddedKg))
        drawKeyValueRow(localizedContext.getString(R.string.pdf_rice_consumption), Formatters.formatKg(report.riceSummary.totalConsumedKg))
        drawKeyValueRow(localizedContext.getString(R.string.pdf_rice_adjustments), Formatters.formatSignedKg(report.riceSummary.totalAdjustmentsKg))
        drawKeyValueRow(localizedContext.getString(R.string.pdf_rice_remaining), Formatters.formatKg(report.riceSummary.remainingStockKg))
        y += 14f

        // Study Summary
        ensureSpace(110f)
        canvas.drawText(localizedContext.getString(R.string.pdf_study_summary), margin, y, sectionPaint)
        y += 8f
        canvas.drawLine(margin, y, pageWidth - margin, y, dividerPaint)
        y += 18f
        drawKeyValueRow(
            localizedContext.getString(R.string.records_total_study),
            Formatters.formatDurationHoursMinutes(report.studyStats.monthlySeconds)
        )
        drawKeyValueRow(
            localizedContext.getString(R.string.study_stats_top_subject),
            report.studyStats.mostStudiedSubject ?: localizedContext.getString(R.string.label_none)
        )
        drawKeyValueRow(
            localizedContext.getString(R.string.study_stats_streak),
            localizedContext.getString(R.string.study_stats_streak_days, report.studyStats.currentStreakDays)
        )
        y += 14f

        // Subject-wise Study Report
        ensureSpace(60f)
        canvas.drawText(localizedContext.getString(R.string.pdf_subject_report), margin, y, sectionPaint)
        y += 8f
        canvas.drawLine(margin, y, pageWidth - margin, y, dividerPaint)
        y += 18f

        if (report.studyStats.subjectBreakdown.isEmpty()) {
            canvas.drawText(localizedContext.getString(R.string.study_history_empty), margin + 8f, y, subtitlePaint)
            y += 18f
        } else {
            for (item in report.studyStats.subjectBreakdown) {
                val subjectTitle = if (item.subjectCode.isNotBlank()) {
                    "${item.subjectName} (${item.subjectCode})"
                } else {
                    item.subjectName
                }
                drawKeyValueRow(subjectTitle, Formatters.formatDurationSeconds(item.totalSeconds))
            }
        }

        // Footer on last page
        canvas.drawLine(margin, pageHeight - 36f, pageWidth - margin, pageHeight - 36f, dividerPaint)
        canvas.drawText(
            localizedContext.getString(R.string.pdf_generated_offline),
            margin,
            pageHeight - 20f,
            subtitlePaint
        )

        document.finishPage(page)
        FileOutputStream(outFile).use { fos ->
            document.writeTo(fos)
        }
        document.close()

        outFile
    }
}
