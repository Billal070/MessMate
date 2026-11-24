package com.example

import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.MessMateDatabase
import com.example.data.local.ProfileEntity
import com.example.data.local.StudyCompletionStatus
import com.example.data.local.StudySessionEntity
import com.example.data.local.StudySessionType
import com.example.data.repository.MonthlyMealSummary
import com.example.data.repository.MonthlyRiceSummary
import com.example.data.repository.StudyRepository
import com.example.data.repository.StudyStatistics
import com.example.data.repository.SubjectStudySummary
import com.example.pdf.MonthlyPdfGenerator
import com.example.pdf.MonthlyReportData
import com.example.pdf.PdfFileActions
import com.example.pdf.PdfStructureValidator
import com.example.ui.components.MotionSpec
import com.example.util.TimeProvider
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.Locale

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.UPSIDE_DOWN_CAKE])
class PdfAndPerformanceVerificationTest {

    private lateinit var context: android.content.Context
    private lateinit var db: MessMateDatabase
    private val zone = ZoneId.of("Asia/Dhaka")

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, MessMateDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() {
        db.close()
    }

    private fun fakeTimeProvider(dateTime: ZonedDateTime): TimeProvider = object : TimeProvider {
        override val zoneId: ZoneId = zone
        override fun now(): ZonedDateTime = dateTime
        override fun nowEpochMs(): Long = dateTime.toInstant().toEpochMilli()
        override fun today(): LocalDate = dateTime.toLocalDate()
        override fun currentYearMonth(): YearMonth = YearMonth.from(dateTime)
        override fun fromEpochMs(epochMs: Long): ZonedDateTime = Instant.ofEpochMilli(epochMs).atZone(zone)
    }

    @Test
    fun singlePageMonthlyPdf_isValidPersistentFile_andPassesStructureValidation() = runBlocking {
        val generator = MonthlyPdfGenerator()
        val report = MonthlyReportData(
            yearMonth = YearMonth.of(2026, 3),
            profile = ProfileEntity(
                name = "Tahmid Hasan",
                messName = "Green Valley Student Mess",
                institutionName = "Dhaka Polytechnic Institute",
                department = "Computer Science and Technology (CST)",
                semester = "4th Semester"
            ),
            mealSummary = MonthlyMealSummary(
                countedMeals = 72,
                skippedMeals = 6,
                offMeals = 9,
                offDays = 3,
                breakfastCounted = 24,
                lunchCounted = 24,
                dinnerCounted = 24
            ),
            riceSummary = MonthlyRiceSummary(
                totalAddedKg = 15.0,
                totalConsumedKg = 11.25,
                totalAdjustmentsKg = 0.5,
                remainingStockKg = 4.25
            ),
            studyStats = StudyStatistics(
                todaySeconds = 5400L,
                weeklySeconds = 36000L,
                monthlySeconds = 108000L,
                currentStreakDays = 7,
                mostStudiedSubject = "Data Structures & Algorithms",
                subjectBreakdown = listOf(
                    SubjectStudySummary("Data Structures & Algorithms", "66641", 43200L, 12),
                    SubjectStudySummary("Object Oriented Programming", "66642", 36000L, 10),
                    SubjectStudySummary("Digital Electronics", "66842", 28800L, 8)
                )
            )
        )

        val pdfFile = generator.generateMonthlyReport(context, report, Locale.ENGLISH)

        assertTrue("PDF file must exist", pdfFile.exists())
        assertTrue("PDF should be saved in persistent filesDir/reports", pdfFile.absolutePath.contains("/reports/"))
        assertTrue("PDF must end with .pdf", pdfFile.name.endsWith(".pdf"))

        val validation = PdfStructureValidator.validate(pdfFile)
        assertTrue("Generated PDF failed validation: ${validation.errorMessage}", validation.isValid)
        assertEquals("Single-page report should have 1 page", 1, validation.pageCount)
        assertTrue("PDF size should be > 400 bytes", validation.sizeBytes > 400L)

        val rawBytes = pdfFile.readBytes()
        val header = String(rawBytes, 0, 5, Charsets.ISO_8859_1)
        val tail = String(rawBytes, (rawBytes.size - 64).coerceAtLeast(0), minOf(64, rawBytes.size), Charsets.ISO_8859_1)
        assertEquals("%PDF-", header)
        assertTrue("Tail must contain %%EOF", tail.contains("%%EOF"))
    }

    @Test
    fun multiPageMonthlyPdf_paginatesCleanly_andPassesStructureValidation() = runBlocking {
        val generator = MonthlyPdfGenerator()
        val manySubjects = (1..40).map { idx ->
            SubjectStudySummary(
                subjectName = "Very Long Polytechnic Engineering Course Title Number $idx",
                subjectCode = "66${100 + idx}",
                totalSeconds = (idx * 900L),
                sessionCount = idx
            )
        }
        val report = MonthlyReportData(
            yearMonth = YearMonth.of(2026, 4),
            profile = ProfileEntity(
                name = "Rafiqul Islam",
                messName = "Shapla Mess",
                institutionName = "Chattogram Polytechnic Institute",
                department = "Electrical Technology (ET)",
                semester = "6th Semester"
            ),
            mealSummary = MonthlyMealSummary(81, 3, 6, 2, 27, 27, 27),
            riceSummary = MonthlyRiceSummary(20.0, 14.5, -0.25, 5.25),
            studyStats = StudyStatistics(
                todaySeconds = 3600L,
                weeklySeconds = 21600L,
                monthlySeconds = 180000L,
                currentStreakDays = 12,
                mostStudiedSubject = manySubjects.last().subjectName,
                subjectBreakdown = manySubjects
            )
        )

        val pdfFile = generator.generateMonthlyReport(context, report, Locale.ENGLISH)
        val validation = PdfStructureValidator.validate(pdfFile)

        assertTrue("Multi-page PDF failed validation: ${validation.errorMessage}", validation.isValid)
        assertTrue("40 subjects must paginate across at least 2 pages, got ${validation.pageCount}", validation.pageCount >= 2)

        // When Ghostscript is available on the host, parse & render all pages to nullpage to verify strict PDF compliance
        val gsBinary = File("/usr/bin/gs")
        if (gsBinary.exists() && gsBinary.canExecute()) {
            val proc = ProcessBuilder(
                gsBinary.absolutePath,
                "-dBATCH",
                "-dNOPAUSE",
                "-dSAFER",
                "-sDEVICE=nullpage",
                pdfFile.absolutePath
            ).redirectErrorStream(true).start()
            val gsOutput = proc.inputStream.bufferedReader().readText()
            val exitCode = proc.waitFor()
            assertEquals("Ghostscript failed to parse generated PDF:\n$gsOutput", 0, exitCode)
            assertTrue("Ghostscript should report rendering Page 2:\n$gsOutput", gsOutput.contains("Page 2"))
        }
    }

    @Test
    fun pdfStructureValidator_rejectsCorruptAndTruncatedFiles() {
        val tempDir = File(context.cacheDir, "pdf_validator_test").apply { mkdirs() }

        val emptyFile = File(tempDir, "empty.pdf").apply { writeBytes(ByteArray(0)) }
        assertFalse(PdfStructureValidator.validate(emptyFile).isValid)

        val plainText = File(tempDir, "not_pdf.pdf").apply { writeText("Hello, this is not a PDF file at all!".repeat(10)) }
        assertFalse(PdfStructureValidator.validate(plainText).isValid)

        val truncatedPdf = File(tempDir, "truncated.pdf").apply {
            writeText("%PDF-1.4\n1 0 obj\n<< /Type /Catalog >>\nendobj\n" + " ".repeat(200))
        }
        assertFalse(PdfStructureValidator.validate(truncatedPdf).isValid)
    }

    @Test
    fun pdfFileActions_copyAndIntents_enforceValidityMimeTypeAndClipData() = runBlocking {
        val generator = MonthlyPdfGenerator()
        val report = MonthlyReportData(
            yearMonth = YearMonth.of(2026, 5),
            profile = ProfileEntity(name = "Nadia Akter"),
            mealSummary = MonthlyMealSummary(30, 0, 0, 0, 10, 10, 10),
            riceSummary = MonthlyRiceSummary(10.0, 5.0, 0.0, 5.0),
            studyStats = StudyStatistics()
        )
        val sourcePdf = generator.generateMonthlyReport(context, report, Locale.ENGLISH)

        // Verify SAF copy writes exact byte-for-byte valid PDF
        val destFile = File(context.cacheDir, "exported_copy.pdf").apply {
            // Pre-populate with longer garbage data to verify truncation overwrites cleanly!
            writeBytes(ByteArray(sourcePdf.length().toInt() + 1024) { 0x58 })
        }
        val copyResult = PdfFileActions.copyPdfToUri(context, sourcePdf, Uri.fromFile(destFile))
        assertTrue("Copy should succeed: ${copyResult.exceptionOrNull()}", copyResult.isSuccess)
        assertEquals(sourcePdf.length(), copyResult.getOrNull())
        assertArrayEquals("Exported bytes must match source PDF exactly", sourcePdf.readBytes(), destFile.readBytes())
        assertTrue("Exported PDF must pass structure validation", PdfStructureValidator.validate(destFile).isValid)

        // Verify Open PDF intent
        val openIntent = PdfFileActions.buildOpenPdfIntent(context, sourcePdf)
        assertEquals(Intent.ACTION_VIEW, openIntent.action)
        assertEquals("application/pdf", openIntent.type)
        assertNotNull("Open intent must grant FileProvider URI", openIntent.data)
        assertNotNull("Open intent must attach ClipData for Android 10+ URI grant", openIntent.clipData)
        assertTrue((openIntent.flags and Intent.FLAG_GRANT_READ_URI_PERMISSION) != 0)

        // Verify Share PDF intent
        val shareIntent = PdfFileActions.buildSharePdfIntent(context, sourcePdf)
        assertEquals(Intent.ACTION_SEND, shareIntent.action)
        assertEquals("application/pdf", shareIntent.type)
        assertNotNull("Share intent must attach ClipData for Android 10+ URI grant", shareIntent.clipData)
        assertTrue((shareIntent.flags and Intent.FLAG_GRANT_READ_URI_PERMISSION) != 0)

        // Verify extension helper
        assertEquals("Report.pdf", PdfFileActions.ensurePdfExtension("Report"))
        assertEquals("Report.pdf", PdfFileActions.ensurePdfExtension("Report.pdf"))
    }

    @Test
    fun optimizedStudyStatistics_matchesExactTodayWeeklyMonthlyAndStreak() {
        val today = LocalDate.of(2026, 3, 18) // Wednesday (week started Saturday 2026-03-14)
        val repo = StudyRepository(db, fakeTimeProvider(ZonedDateTime.of(2026, 3, 18, 12, 0, 0, 0, zone)))

        val sessions = listOf(
            StudySessionEntity(subjectName = "Math", subjectCode = "65911", dateIso = "2026-03-18", startTimeEpochMs = 0, endTimeEpochMs = 1800_000, durationSeconds = 1800, sessionType = StudySessionType.POMODORO, completionStatus = StudyCompletionStatus.COMPLETED),
            StudySessionEntity(subjectName = "DSA", subjectCode = "66641", dateIso = "2026-03-17", startTimeEpochMs = 0, endTimeEpochMs = 3600_000, durationSeconds = 3600, sessionType = StudySessionType.STOPWATCH, completionStatus = StudyCompletionStatus.MANUALLY_STOPPED),
            StudySessionEntity(subjectName = "DSA", subjectCode = "66641", dateIso = "2026-03-16", startTimeEpochMs = 0, endTimeEpochMs = 1200_000, durationSeconds = 1200, sessionType = StudySessionType.POMODORO, completionStatus = StudyCompletionStatus.COMPLETED),
            StudySessionEntity(subjectName = "Physics", subjectCode = "65912", dateIso = "2026-03-02", startTimeEpochMs = 0, endTimeEpochMs = 900_000, durationSeconds = 900, sessionType = StudySessionType.POMODORO, completionStatus = StudyCompletionStatus.COMPLETED),
            StudySessionEntity(subjectName = "Old Month", subjectCode = "101", dateIso = "2026-02-28", startTimeEpochMs = 0, endTimeEpochMs = 2400_000, durationSeconds = 2400, sessionType = StudySessionType.POMODORO, completionStatus = StudyCompletionStatus.COMPLETED)
        )

        val stats = repo.computeStatistics(sessions, today, YearMonth.of(2026, 3))
        assertEquals(1800L, stats.todaySeconds)
        assertEquals(1800L + 3600L + 1200L, stats.weeklySeconds)
        assertEquals(1800L + 3600L + 1200L + 900L, stats.monthlySeconds)
        assertEquals(3, stats.currentStreakDays)
        assertEquals("DSA", stats.mostStudiedSubject)
        assertEquals(3, stats.subjectBreakdown.size)
        assertEquals(4800L, stats.subjectBreakdown.first().totalSeconds)
        assertEquals(2, stats.subjectBreakdown.first().sessionCount)
    }

    @Test
    fun motionSpec_respectsReducedMotionPreference() {
        assertEquals(0, MotionSpec.duration(MotionSpec.DURATION_FAST, reducedMotion = true))
        assertEquals(0, MotionSpec.duration(MotionSpec.DURATION_STANDARD, reducedMotion = true))
        assertEquals(0, MotionSpec.duration(MotionSpec.DURATION_EMPHASIS, reducedMotion = true))

        assertTrue(MotionSpec.duration(MotionSpec.DURATION_FAST, reducedMotion = false) in 150..250)
        assertTrue(MotionSpec.duration(MotionSpec.DURATION_STANDARD, reducedMotion = false) in 150..250)
        assertTrue(MotionSpec.duration(MotionSpec.DURATION_EMPHASIS, reducedMotion = false) in 150..250)
    }
}
