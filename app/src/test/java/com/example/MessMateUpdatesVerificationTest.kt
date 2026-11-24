package com.example

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppLanguage
import com.example.data.local.MealCycleEntity
import com.example.data.local.MealRecordEntity
import com.example.data.local.MealStatus
import com.example.data.local.MealType
import com.example.data.local.MessMateDatabase
import com.example.data.local.PreferencesStore
import com.example.data.local.ProfileEntity
import com.example.data.local.RiceTransactionEntity
import com.example.data.local.RiceTransactionType
import com.example.data.repository.MealRiceRepository
import com.example.data.repository.ProfileSettingsRepository
import com.example.ui.screens.WHATSAPP_DEVELOPER_LINK
import com.example.ui.screens.WHATSAPP_DEVELOPER_PHONE
import com.example.ui.screens.buildWhatsAppContactIntent
import com.example.ui.viewmodel.CalendarRecordsViewModel
import com.example.util.LocaleHelper
import com.example.util.TimeProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
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

private class TestDhakaTimeProvider(
    date: LocalDate = LocalDate.of(2026, 11, 23),
    time: LocalTime = LocalTime.of(10, 0)
) : TimeProvider {
    override val zoneId: ZoneId = ZoneId.of("Asia/Dhaka")
    private var current: ZonedDateTime = ZonedDateTime.of(LocalDateTime.of(date, time), zoneId)
    override fun now(): ZonedDateTime = current
    fun set(date: LocalDate, time: LocalTime) {
        current = ZonedDateTime.of(LocalDateTime.of(date, time), zoneId)
    }
}

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class MessMateUpdatesVerificationTest {

    private lateinit var context: Context
    private lateinit var db: MessMateDatabase
    private lateinit var prefs: PreferencesStore
    private lateinit var fakeTime: TestDhakaTimeProvider

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        db = MessMateDatabase.createInMemory(context)
        prefs = PreferencesStore(context)
        runBlocking { prefs.clearForTesting() }
        fakeTime = TestDhakaTimeProvider()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun officialLogoResources_andOriginalSvgAsset_existAndLoadCleanly() {
        // Original SVG preserved verbatim in assets
        val svgSource = context.assets.open("messmate_official_logo.svg").bufferedReader().use { it.readText() }
        assertTrue(svgSource.contains("<svg"))
        assertTrue(svgSource.contains("#F83D11"))

        // Android drawables & launcher mipmaps resolve cleanly
        assertNotNull(ContextCompat.getDrawable(context, R.drawable.ic_messmate_logo))
        assertNotNull(ContextCompat.getDrawable(context, R.drawable.ic_launcher_foreground))
        assertNotNull(ContextCompat.getDrawable(context, R.drawable.ic_launcher_background))
        assertNotNull(ContextCompat.getDrawable(context, R.drawable.ic_splash_logo))
        assertNotNull(ContextCompat.getDrawable(context, R.drawable.splash_background))
        assertNotNull(ContextCompat.getDrawable(context, R.mipmap.ic_launcher))
        assertNotNull(ContextCompat.getDrawable(context, R.mipmap.ic_launcher_round))
    }

    @Test
    fun freshInstall_hasZeroDataBeforeSetup_andStartsTrackingFromSetupDateOnly() = runTest {
        val setupDate = LocalDate.of(2026, 11, 23)
        fakeTime.set(setupDate, LocalTime.of(10, 0))

        val mealRiceRepo = MealRiceRepository(db, fakeTime, prefs)
        val profileRepo = ProfileSettingsRepository(context, db, prefs, fakeTime)

        // 1. Before setup completes: reconciliation must create ZERO meals and ZERO rice transactions
        mealRiceRepo.ensureDefaultsAndReconcile()
        assertTrue(mealRiceRepo.observeMealsForDate(setupDate).first().isEmpty())
        assertTrue(mealRiceRepo.observeMealsForDate(setupDate.minusDays(1)).first().isEmpty())
        assertTrue(mealRiceRepo.allRiceTransactions.first().isEmpty())
        assertEquals(0.0, mealRiceRepo.getCurrentRiceStockOnce(), 0.0001)

        // 2. Complete initial setup on setupDate -> persists trackingStartDateIso
        val setupResult = profileRepo.completeInitialSetup(
            name = "Test Student",
            photoPath = null,
            messName = "Green Mess",
            institutionName = "DPI",
            department = "Computer Science and Technology",
            semester = "1st Semester"
        )
        assertTrue(setupResult.isSuccess)
        assertEquals(setupDate.toString(), prefs.getTrackingStartDateIsoOnce())

        // 3. Reconcile after setup -> tracks from setupDate only, ZERO fabricated meals/deductions before setupDate
        mealRiceRepo.ensureDefaultsAndReconcile()
        val yesterdayMeals = mealRiceRepo.observeMealsForDate(setupDate.minusDays(1)).first()
        assertTrue("No fabricated meals before tracking start date", yesterdayMeals.isEmpty())

        val todayMeals = mealRiceRepo.observeMealsForDate(setupDate).first()
        assertEquals(3, todayMeals.size)
        // At 10:00 AM, only Breakfast (08:30) is counted -> 0.20 KG deducted (NOT yesterday's dinner!)
        assertEquals(MealStatus.COUNTED, todayMeals.first { it.mealType == MealType.BREAKFAST }.status)
        assertEquals(MealStatus.PENDING, todayMeals.first { it.mealType == MealType.LUNCH }.status)
        assertEquals(MealStatus.PENDING, todayMeals.first { it.mealType == MealType.DINNER }.status)
        assertEquals(-0.20, mealRiceRepo.getCurrentRiceStockOnce(), 0.0001)

        // 4. Opening a historical date before setupDate must NOT fabricate activity
        val historicalDate = setupDate.minusDays(5)
        mealRiceRepo.ensureDateRecordsExist(historicalDate)
        assertTrue(mealRiceRepo.observeMealsForDate(historicalDate).first().isEmpty())
    }

    @Test
    fun existingUserUpgrade_preservesAllHistoricalRecordsWithoutReset() = runTest {
        val today = LocalDate.of(2026, 11, 23)
        val earlierDate = LocalDate.of(2026, 11, 10)
        fakeTime.set(today, LocalTime.of(10, 0))

        // Seed existing user data as it would exist before the update (no trackingStartDateIso in DataStore yet)
        db.profileDao().upsert(
            ProfileEntity(
                id = 1,
                name = "Existing User",
                setupCompleted = true,
                createdDateIso = earlierDate.toString()
            )
        )
        prefs.setSetupCompleted(true)
        db.mealCycleDao().upsert(
            MealCycleEntity(
                id = MealRiceRepository.cycleId(earlierDate),
                startDateIso = earlierDate.toString(),
                isOn = true,
                updatedAtEpochMs = 1000L
            )
        )
        db.mealRecordDao().upsert(
            MealRecordEntity(
                id = MealRiceRepository.mealId(earlierDate, MealType.DINNER),
                cycleId = MealRiceRepository.cycleId(earlierDate),
                dateIso = earlierDate.toString(),
                mealType = MealType.DINNER,
                status = MealStatus.COUNTED,
                isManuallyOverridden = false,
                updatedAtEpochMs = 1000L
            )
        )
        db.riceTransactionDao().insert(
            RiceTransactionEntity(
                dateIso = earlierDate.toString(),
                timestampEpochMs = 1000L,
                type = RiceTransactionType.RICE_ADDED,
                quantityKg = 20.0,
                relatedMealId = null,
                note = "Existing deposit"
            )
        )

        val mealRiceRepo = MealRiceRepository(db, fakeTime, prefs)
        mealRiceRepo.ensureDefaultsAndReconcile()

        // Historical data remains completely intact
        val preservedMeals = mealRiceRepo.observeMealsForDate(earlierDate).first()
        assertTrue(preservedMeals.any { it.mealType == MealType.DINNER && it.status == MealStatus.COUNTED })
        val preservedRice = mealRiceRepo.observeRiceByDate(earlierDate).first()
        assertTrue(preservedRice.any { it.type == RiceTransactionType.RICE_ADDED && it.quantityKg == 20.0 })
        assertEquals(earlierDate.toString(), prefs.getTrackingStartDateIsoOnce())
    }

    @Test
    fun calendarNavigation_allowsPastMonths_returnsToCurrentMonth_andBlocksFutureMonths() = runTest {
        val today = LocalDate.of(2026, 11, 23)
        val currentMonth = YearMonth.of(2026, 11)
        fakeTime.set(today, LocalTime.of(10, 0))

        val container = AppContainer(context, fakeTime, db)
        val viewModel = CalendarRecordsViewModel(container)

        // Starts at current Asia/Dhaka month; future navigation is blocked
        assertEquals(currentMonth, viewModel.selectedMonth.value)
        assertFalse(viewModel.canNavigateNextMonth())

        viewModel.nextMonth()
        assertEquals("nextMonth must not move past current month", currentMonth, viewModel.selectedMonth.value)

        viewModel.selectMonth(currentMonth.plusMonths(3))
        assertEquals("selectMonth must clamp future months", currentMonth, viewModel.selectedMonth.value)

        // Previous months remain accessible
        viewModel.previousMonth()
        assertEquals(YearMonth.of(2026, 10), viewModel.selectedMonth.value)
        assertTrue(viewModel.canNavigateNextMonth())

        viewModel.previousMonth()
        assertEquals(YearMonth.of(2026, 9), viewModel.selectedMonth.value)

        // Can navigate forward back to current month, then blocked again
        viewModel.nextMonth()
        assertEquals(YearMonth.of(2026, 10), viewModel.selectedMonth.value)
        viewModel.nextMonth()
        assertEquals(currentMonth, viewModel.selectedMonth.value)
        assertFalse(viewModel.canNavigateNextMonth())
        viewModel.nextMonth()
        assertEquals(currentMonth, viewModel.selectedMonth.value)

        // Future dates must not create activity records
        val futureDate = today.plusDays(4)
        container.mealRiceRepository.ensureDateRecordsExist(futureDate)
        assertTrue(container.mealRiceRepository.observeMealsForDate(futureDate).first().isEmpty())
    }

    @Test
    fun whatsAppContact_usesExactNumberUrlAndActionViewIntent() {
        assertEquals("01944890493", WHATSAPP_DEVELOPER_PHONE)
        assertEquals("https://wa.me/8801944890493", WHATSAPP_DEVELOPER_LINK)

        val intent = buildWhatsAppContactIntent()
        assertEquals(Intent.ACTION_VIEW, intent.action)
        assertEquals("https://wa.me/8801944890493", intent.data?.toString())
        assertTrue(intent.hasCategory(Intent.CATEGORY_BROWSABLE))

        val enContext = LocaleHelper.createLocalizedContext(context, AppLanguage.EN)
        assertEquals("Contact on WhatsApp", enContext.getString(R.string.about_contact_whatsapp))
    }
}
