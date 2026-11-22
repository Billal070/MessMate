package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppLanguage
import com.example.data.local.DeadlineWindow
import com.example.data.local.MealStatus
import com.example.data.local.MealType
import com.example.data.local.MessMateDatabase
import com.example.data.local.PreferencesStore
import com.example.data.local.ProfileEntity
import com.example.data.local.RiceTransactionType
import com.example.data.local.StudyCompletionStatus
import com.example.data.local.StudySessionType
import com.example.data.repository.CsvSubjectImporter
import com.example.data.repository.MealRiceRepository
import com.example.data.repository.ProfileSettingsRepository
import com.example.data.repository.StudyRepository
import com.example.pdf.MonthlyPdfGenerator
import com.example.pdf.MonthlyReportData
import com.example.util.LocaleHelper
import com.example.util.TimeProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.YearMonth
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.Locale

private class FakeBangladeshTimeProvider(
    initialDate: LocalDate = LocalDate.of(2026, 11, 23),
    initialTime: LocalTime = LocalTime.of(10, 0)
) : TimeProvider {
    override val zoneId: ZoneId = ZoneId.of("Asia/Dhaka")
    private var current: ZonedDateTime = ZonedDateTime.of(LocalDateTime.of(initialDate, initialTime), zoneId)

    override fun now(): ZonedDateTime = current

    fun setDateTime(date: LocalDate, time: LocalTime) {
        current = ZonedDateTime.of(LocalDateTime.of(date, time), zoneId)
    }
}

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class MessMateAcceptanceTest {

    private lateinit var context: Context
    private lateinit var db: MessMateDatabase
    private lateinit var fakeTime: FakeBangladeshTimeProvider
    private lateinit var mealRiceRepo: MealRiceRepository
    private lateinit var studyRepo: StudyRepository
    private lateinit var profileRepo: ProfileSettingsRepository
    private lateinit var csvImporter: CsvSubjectImporter

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        db = MessMateDatabase.createInMemory(context)
        fakeTime = FakeBangladeshTimeProvider()
        val prefs = PreferencesStore(context)
        mealRiceRepo = MealRiceRepository(db, fakeTime)
        studyRepo = StudyRepository(db, fakeTime)
        profileRepo = ProfileSettingsRepository(context, db, prefs, fakeTime)
        csvImporter = CsvSubjectImporter(context, db.subjectDao())
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun mealCycle_initialOn_autoCountsAfterScheduledTimes_andNeverDoubleCounts() = runTest {
        val day1 = LocalDate.of(2026, 11, 23)
        fakeTime.setDateTime(day1, LocalTime.of(21, 0)) // 9:00 PM BST
        mealRiceRepo.ensureDefaultsAndReconcile()

        val day1Meals = mealRiceRepo.observeMealsForDate(day1).first()
        assertEquals(3, day1Meals.size)
        assertTrue(day1Meals.all { it.status == MealStatus.COUNTED })

        // Reconcile multiple times — must never duplicate meals or deductions
        mealRiceRepo.ensureDefaultsAndReconcile()
        mealRiceRepo.ensureDefaultsAndReconcile()

        val txs = mealRiceRepo.allRiceTransactions.first()
        val day1Txs = txs.filter { it.dateIso == day1.toString() }
        assertEquals(3, day1Txs.size)
        assertEquals(1.00, day1Txs.sumOf { it.quantityKg }, 0.0001)
    }

    @Test
    fun mealOnOff_deadlines_before1pm_grace1to2pm_andAfter2pmLockedCycle() = runTest {
        val day = LocalDate.of(2026, 11, 23)

        // Before 1:00 PM -> applies to today's Dinner cycle
        fakeTime.setDateTime(day, LocalTime.of(12, 30))
        val res1 = mealRiceRepo.toggleMealOnOff(false)
        assertEquals(DeadlineWindow.BEFORE_1PM, res1.window)
        assertEquals(day, res1.effectiveCycleStartDate)
        assertFalse(mealRiceRepo.getCycleForStartDateOnce(day)!!.isOn)

        // Between 1:00 PM and 2:00 PM -> grace warning, still applies to today's Dinner cycle
        fakeTime.setDateTime(day, LocalTime.of(13, 30))
        val res2 = mealRiceRepo.toggleMealOnOff(true)
        assertEquals(DeadlineWindow.GRACE_1PM_TO_2PM, res2.window)
        assertEquals(day, res2.effectiveCycleStartDate)
        assertTrue(mealRiceRepo.getCycleForStartDateOnce(day)!!.isOn)

        // After 2:00 PM -> locked today's cycle stays ON, change applies to tomorrow's cycle
        fakeTime.setDateTime(day, LocalTime.of(14, 15))
        val res3 = mealRiceRepo.toggleMealOnOff(false)
        assertEquals(DeadlineWindow.AFTER_2PM, res3.window)
        assertEquals(day.plusDays(1), res3.effectiveCycleStartDate)
        assertTrue(mealRiceRepo.getCycleForStartDateOnce(day)!!.isOn)
        assertFalse(mealRiceRepo.getCycleForStartDateOnce(day.plusDays(1))!!.isOn)
    }

    @Test
    fun mealOff_persistsAcrossMultipleOfflineDays_withZeroDeductions() = runTest {
        val day1 = LocalDate.of(2026, 11, 23)
        fakeTime.setDateTime(day1, LocalTime.of(10, 0))
        mealRiceRepo.ensureDefaultsAndReconcile()
        mealRiceRepo.toggleMealOnOff(false)

        // Reopen app 4 days later at 9:00 PM
        val day5 = day1.plusDays(4)
        fakeTime.setDateTime(day5, LocalTime.of(21, 0))
        mealRiceRepo.ensureDefaultsAndReconcile()

        // Cycle day1..day5 must remain OFF (never auto-flipped back to ON)
        for (offset in 0L..4L) {
            val cycle = mealRiceRepo.getCycleForStartDateOnce(day1.plusDays(offset))
            assertNotNull(cycle)
            assertFalse("Cycle ${day1.plusDays(offset)} should remain OFF", cycle!!.isOn)
        }

        // Day 3 meals should all be OFF with zero deductions
        val day3 = day1.plusDays(2)
        val day3Meals = mealRiceRepo.observeMealsForDate(day3).first()
        assertTrue(day3Meals.isNotEmpty() && day3Meals.all { it.status == MealStatus.OFF })
        val day3Rice = mealRiceRepo.observeRiceByDate(day3).first()
        assertTrue(day3Rice.isEmpty())
    }

    @Test
    fun riceStock_skipRestore_customConsumption_negativeStock_andDeletion() = runTest {
        val day = LocalDate.of(2026, 11, 23)
        fakeTime.setDateTime(day, LocalTime.of(21, 0))
        mealRiceRepo.ensureDefaultsAndReconcile()

        // Initial 3 meals counted: 0.20 + 0.40 + 0.40 = 1.00 KG consumed -> stock = -1.00 KG
        assertEquals(-1.00, mealRiceRepo.getCurrentRiceStockOnce(), 0.0001)

        // Adding 10 KG to -1 KG -> Expected stock = 9 KG
        mealRiceRepo.addRiceDeposit(10.0, day, "Deposit 1")
        assertEquals(9.00, mealRiceRepo.getCurrentRiceStockOnce(), 0.0001)

        // Adding 25 KG -> Expected stock = 34 KG
        mealRiceRepo.addRiceDeposit(25.0, day, "Deposit 2")
        assertEquals(34.00, mealRiceRepo.getCurrentRiceStockOnce(), 0.0001)

        // Skip Lunch -> reverses 0.40 KG deduction -> stock = 34.40 KG
        mealRiceRepo.skipMeal(day, MealType.LUNCH)
        assertEquals(34.40, mealRiceRepo.getCurrentRiceStockOnce(), 0.0001)

        // Update Lunch rice setting to 0.50 KG and restore Lunch -> deducts configured 0.50 KG -> stock = 33.90 KG
        profileRepo.updateRiceConsumptionSettings(0.20, 0.50, 0.40)
        mealRiceRepo.restoreMeal(day, MealType.LUNCH)
        assertEquals(33.90, mealRiceRepo.getCurrentRiceStockOnce(), 0.0001)

        // Delete the 25 KG deposit -> stock = 8.90 KG
        val deposit25 = mealRiceRepo.allRiceTransactions.first()
            .first { it.type == RiceTransactionType.RICE_ADDED && it.quantityKg == 25.0 }
        mealRiceRepo.deleteRiceTransaction(deposit25)
        assertEquals(8.90, mealRiceRepo.getCurrentRiceStockOnce(), 0.0001)
    }

    @Test
    fun studyCsvImport_noDuplicates_andStudyStatsAccuracy() = runTest {
        csvImporter.importInitialSubjectsIfNeeded()
        val imported = studyRepo.allSubjects.first()
        assertEquals(46, imported.size)

        val cstSubjects = studyRepo.observeSubjectsByFilter("1st Semester", "Computer Science and Technology").first()
        assertEquals(7, cstSubjects.size)
        assertTrue(cstSubjects.any { it.subjectName == "Computer Office Application" && it.subjectCode == "28511" })

        val mechSubjects = studyRepo.observeSubjectsByFilter("1st Semester", "Mechanical Technology").first()
        assertEquals(8, mechSubjects.size)
        assertTrue(mechSubjects.any { it.subjectName == "Machine Shop Practice-I" && it.subjectCode == "27012" })

        val civilSubjects = studyRepo.observeSubjectsByFilter("1st Semester", "Civil Technology").first()
        assertEquals(8, civilSubjects.size)
        assertTrue(civilSubjects.any { it.subjectName == "Chemistry" && it.subjectCode == "25913" })

        // Running import again must not duplicate subjects
        csvImporter.importInitialSubjectsIfNeeded()
        val secondCount = studyRepo.allSubjects.first().size
        assertEquals(46, secondCount)

        // Save Pomodoro and Stopwatch sessions
        val day = LocalDate.of(2026, 11, 23)
        fakeTime.setDateTime(day, LocalTime.of(18, 0))
        val mathSub = studyRepo.allSubjects.first().first { it.subjectName.contains("Mathematics") }
        val nowMs = fakeTime.nowEpochMs()

        studyRepo.saveStudySession(
            subject = mathSub,
            fallbackSubjectName = "General",
            startTimeEpochMs = nowMs - 1500_000L,
            endTimeEpochMs = nowMs,
            durationSeconds = 1500L, // 25 min Pomodoro
            sessionType = StudySessionType.POMODORO,
            completionStatus = StudyCompletionStatus.COMPLETED
        )
        studyRepo.saveStudySession(
            subject = mathSub,
            fallbackSubjectName = "General",
            startTimeEpochMs = nowMs - 600_000L,
            endTimeEpochMs = nowMs,
            durationSeconds = 600L, // 10 min Stopwatch
            sessionType = StudySessionType.STOPWATCH,
            completionStatus = StudyCompletionStatus.MANUALLY_STOPPED
        )

        val stats = studyRepo.getMonthlyStudySummary(YearMonth.of(2026, 11))
        assertEquals(2100L, stats.todaySeconds)
        assertEquals(2100L, stats.monthlySeconds)
        assertEquals(mathSub.subjectName, stats.mostStudiedSubject)
        assertEquals(1, stats.currentStreakDays)
    }

    @Test
    fun offlinePdfGeneration_andBilingualResourcesWork() = runTest {
        val month = YearMonth.of(2026, 11)
        val generator = MonthlyPdfGenerator()
        val mealSummary = mealRiceRepo.getMonthlyMealSummary(month)
        val riceSummary = mealRiceRepo.getMonthlyRiceSummary(month)
        val studyStats = studyRepo.getMonthlyStudySummary(month)

        val pdfFile = generator.generateMonthlyReport(
            localizedContext = context,
            report = MonthlyReportData(
                yearMonth = month,
                profile = ProfileEntity(name = "Billal Hossain", messName = "Green Mess", institutionName = "DPI"),
                mealSummary = mealSummary,
                riceSummary = riceSummary,
                studyStats = studyStats
            ),
            locale = Locale.ENGLISH
        )
        assertTrue(pdfFile.exists())
        assertTrue(pdfFile.length() > 100L)

        // Verify English & Bangla localization
        val enContext = LocaleHelper.createLocalizedContext(context, AppLanguage.EN)
        val bnContext = LocaleHelper.createLocalizedContext(context, AppLanguage.BN)
        assertEquals("MessMate", enContext.getString(R.string.app_name))
        assertEquals("MessMate", bnContext.getString(R.string.app_name))
        assertEquals("Home", enContext.getString(R.string.nav_home))
        assertEquals("হোম", bnContext.getString(R.string.nav_home))
    }
}
