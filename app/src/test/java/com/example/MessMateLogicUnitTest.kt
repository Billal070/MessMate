package com.example

import com.example.data.local.DeadlineWindow
import com.example.data.local.MealType
import com.example.data.local.RiceTransactionEntity
import com.example.data.local.RiceTransactionType
import com.example.data.local.StudyCompletionStatus
import com.example.data.local.StudySessionEntity
import com.example.data.local.StudySessionType
import com.example.data.repository.MealRiceRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.ZoneId
import java.time.ZonedDateTime

class MessMateLogicUnitTest {

    @Test
    fun mealCycle_dateMapping_matchesDinnerPlusNextDayBreakfastAndLunch() {
        val nov23 = LocalDate.of(2026, 11, 23)
        val nov24 = LocalDate.of(2026, 11, 24)

        // Cycle starting 23 Nov = Dinner 23 Nov + Breakfast 24 Nov + Lunch 24 Nov
        assertEquals(nov23, MealRiceRepository.cycleStartDateForMeal(nov23, MealType.DINNER))
        assertEquals(nov23, MealRiceRepository.cycleStartDateForMeal(nov24, MealType.BREAKFAST))
        assertEquals(nov23, MealRiceRepository.cycleStartDateForMeal(nov24, MealType.LUNCH))

        // Dinner of 24 Nov starts a new cycle (24 Nov)
        assertEquals(nov24, MealRiceRepository.cycleStartDateForMeal(nov24, MealType.DINNER))
    }

    @Test
    fun mealDeadlines_bangladeshTime_before1pm_grace1to2pm_after2pm() {
        val dhaka = ZoneId.of("Asia/Dhaka")
        val day = LocalDate.of(2026, 11, 23)

        fun evaluate(time: LocalTime): Pair<DeadlineWindow, LocalDate> {
            val zdt = ZonedDateTime.of(day, time, dhaka)
            return when {
                zdt.toLocalTime().isBefore(MealRiceRepository.DEADLINE_1PM) ->
                    DeadlineWindow.BEFORE_1PM to zdt.toLocalDate()
                zdt.toLocalTime().isBefore(MealRiceRepository.DEADLINE_2PM) ->
                    DeadlineWindow.GRACE_1PM_TO_2PM to zdt.toLocalDate()
                else ->
                    DeadlineWindow.AFTER_2PM to zdt.toLocalDate().plusDays(1)
            }
        }

        // 12:59 PM -> Before 1 PM -> today's cycle
        assertEquals(DeadlineWindow.BEFORE_1PM to day, evaluate(LocalTime.of(12, 59)))
        // 1:00 PM & 1:45 PM -> Grace period -> today's cycle with warning
        assertEquals(DeadlineWindow.GRACE_1PM_TO_2PM to day, evaluate(LocalTime.of(13, 0)))
        assertEquals(DeadlineWindow.GRACE_1PM_TO_2PM to day, evaluate(LocalTime.of(13, 45)))
        // 2:00 PM & 8:00 PM -> Locked today, applies to tomorrow's cycle
        assertEquals(DeadlineWindow.AFTER_2PM to day.plusDays(1), evaluate(LocalTime.of(14, 0)))
        assertEquals(DeadlineWindow.AFTER_2PM to day.plusDays(1), evaluate(LocalTime.of(20, 0)))
    }

    @Test
    fun riceStockFormula_supportsDepositsAdjustmentsDeductionsAndNegativeStock() {
        val txs = listOf(
            // Start with 1.00 KG consumption (stock = -1.00 KG)
            RiceTransactionEntity(dateIso = "2026-11-23", timestampEpochMs = 1L, type = RiceTransactionType.BREAKFAST_DEDUCTION, quantityKg = 0.20, relatedMealId = "M1"),
            RiceTransactionEntity(dateIso = "2026-11-23", timestampEpochMs = 2L, type = RiceTransactionType.LUNCH_DEDUCTION, quantityKg = 0.40, relatedMealId = "M2"),
            RiceTransactionEntity(dateIso = "2026-11-23", timestampEpochMs = 3L, type = RiceTransactionType.DINNER_DEDUCTION, quantityKg = 0.40, relatedMealId = "M3")
        )

        fun computeStock(list: List<RiceTransactionEntity>): Double {
            val added = list.filter { it.type == RiceTransactionType.RICE_ADDED }.sumOf { it.quantityKg }
            val adjustments = list.filter { it.type == RiceTransactionType.MANUAL_ADJUSTMENT }.sumOf { it.quantityKg }
            val consumed = list.filter { it.type.isDeduction }.sumOf { it.quantityKg }
            return added + adjustments - consumed
        }

        // Negative stock allowed: -1.00 KG
        assertEquals(-1.00, computeStock(txs), 0.0001)

        // Add 10 KG to -1 KG -> 9 KG
        val afterAdd = txs + RiceTransactionEntity(dateIso = "2026-11-23", timestampEpochMs = 4L, type = RiceTransactionType.RICE_ADDED, quantityKg = 10.0)
        assertEquals(9.00, computeStock(afterAdd), 0.0001)

        // Add 25 KG -> 34 KG
        val afterAdd25 = afterAdd + RiceTransactionEntity(dateIso = "2026-11-23", timestampEpochMs = 5L, type = RiceTransactionType.RICE_ADDED, quantityKg = 25.0)
        assertEquals(34.00, computeStock(afterAdd25), 0.0001)

        // Manual adjustment -2.5 KG -> 31.5 KG
        val afterAdj = afterAdd25 + RiceTransactionEntity(dateIso = "2026-11-23", timestampEpochMs = 6L, type = RiceTransactionType.MANUAL_ADJUSTMENT, quantityKg = -2.5)
        assertEquals(31.50, computeStock(afterAdj), 0.0001)
    }

    @Test
    fun semester1CsvAsset_containsExact46SubjectsForAll6Technologies() {
        val csvFile = File("src/main/assets/semester_1_subjects.csv")
        assertTrue("Expected bundled semester_1_subjects.csv asset", csvFile.exists())

        val lines = csvFile.readLines().filter { it.isNotBlank() }
        assertEquals("technology,semester,subject_name,subject_code", lines.first().removePrefix("\uFEFF").trim())

        val dataRows = lines.drop(1).map { it.split(",") }
        assertEquals(46, dataRows.size)

        val byTech = dataRows.groupBy { it[0].trim() }
        assertEquals(8, byTech["Mechanical Technology"]?.size)
        assertEquals(7, byTech["Computer Science and Technology"]?.size)
        assertEquals(8, byTech["Construction Technology"]?.size)
        assertEquals(8, byTech["Electrical Technology"]?.size)
        assertEquals(7, byTech["Electronics Technology"]?.size)
        assertEquals(8, byTech["Civil Technology"]?.size)

        assertTrue(
            byTech["Computer Science and Technology"]!!.any {
                it[2].trim() == "Computer Office Application" && it[3].trim() == "28511"
            }
        )
    }

    @Test
    fun studySessions_dailyMonthlyAndSubjectBreakdownTotalsAreAccurate() {
        val sessions = listOf(
            StudySessionEntity(
                subjectName = "Mathematics-I",
                subjectCode = "25911",
                dateIso = "2026-11-23",
                startTimeEpochMs = 1000L,
                endTimeEpochMs = 1501_000L,
                durationSeconds = 1500L,
                sessionType = StudySessionType.POMODORO,
                completionStatus = StudyCompletionStatus.COMPLETED
            ),
            StudySessionEntity(
                subjectName = "Physics-I",
                subjectCode = "25912",
                dateIso = "2026-11-22",
                startTimeEpochMs = 1000L,
                endTimeEpochMs = 1801_000L,
                durationSeconds = 1800L,
                sessionType = StudySessionType.STOPWATCH,
                completionStatus = StudyCompletionStatus.MANUALLY_STOPPED
            )
        )

        val nov = YearMonth.of(2026, 11)
        val monthSessions = sessions.filter { YearMonth.from(LocalDate.parse(it.dateIso)) == nov }
        assertEquals(3300L, monthSessions.sumOf { it.durationSeconds })
        assertEquals(1500L, sessions.filter { it.dateIso == "2026-11-23" }.sumOf { it.durationSeconds })
    }
}
