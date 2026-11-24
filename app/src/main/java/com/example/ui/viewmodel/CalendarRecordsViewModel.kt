package com.example.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.AppContainer
import com.example.data.local.MealCycleEntity
import com.example.data.local.MealRecordEntity
import com.example.data.local.MealStatus
import com.example.data.local.ProfileEntity
import com.example.data.local.RiceTransactionEntity
import com.example.data.local.RiceTransactionType
import com.example.data.local.StudySessionEntity
import com.example.data.repository.MonthlyMealSummary
import com.example.data.repository.MonthlyRiceSummary
import com.example.data.repository.StudyStatistics
import com.example.pdf.MonthlyReportData
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import java.time.LocalDate
import java.time.YearMonth
import java.util.Locale

data class CalendarDayMarkers(
    val date: LocalDate,
    val hasMealActivity: Boolean = false,
    val hasStudyActivity: Boolean = false,
    val hasRiceActivity: Boolean = false,
    val isMealOffDay: Boolean = false
)

data class DateDetailState(
    val date: LocalDate,
    val meals: List<MealRecordEntity> = emptyList(),
    val dinnerCycle: MealCycleEntity? = null,
    val morningCycle: MealCycleEntity? = null,
    val riceTransactions: List<RiceTransactionEntity> = emptyList(),
    val studySessions: List<StudySessionEntity> = emptyList()
)

data class PdfGenerationState(
    val isGenerating: Boolean = false,
    val generatedFile: File? = null,
    val generatedForMonth: YearMonth? = null,
    val error: String? = null
)

@OptIn(ExperimentalCoroutinesApi::class)
class CalendarRecordsViewModel(private val container: AppContainer) : ViewModel() {

    private val mealRiceRepo = container.mealRiceRepository
    private val studyRepo = container.studyRepository
    private val profileRepo = container.profileSettingsRepository
    private val pdfGenerator = container.pdfGenerator
    val timeProvider = container.timeProvider

    private val _selectedMonth = MutableStateFlow(timeProvider.currentYearMonth())
    val selectedMonth: StateFlow<YearMonth> = _selectedMonth.asStateFlow()

    val monthMeals: StateFlow<List<MealRecordEntity>> = _selectedMonth
        .flatMapLatest { mealRiceRepo.observeMealsInMonth(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val monthRiceTransactions: StateFlow<List<RiceTransactionEntity>> = _selectedMonth
        .flatMapLatest { mealRiceRepo.observeRiceInMonth(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val monthStudySessions: StateFlow<List<StudySessionEntity>> = _selectedMonth
        .flatMapLatest { studyRepo.observeSessionsInMonth(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val monthMealSummary: StateFlow<MonthlyMealSummary> = monthMeals.combine(_selectedMonth) { records, _ ->
        val counted = records.count { it.status == MealStatus.COUNTED }
        val skipped = records.count { it.status == MealStatus.SKIPPED }
        val offMeals = records.count { it.status == MealStatus.OFF }
        val offDays = records.groupBy { it.dateIso }.count { (_, dayRecords) ->
            dayRecords.isNotEmpty() && dayRecords.all { it.status == MealStatus.OFF }
        }
        MonthlyMealSummary(
            countedMeals = counted,
            skippedMeals = skipped,
            offMeals = offMeals,
            offDays = offDays,
            breakfastCounted = records.count { it.mealType.name == "BREAKFAST" && it.status == MealStatus.COUNTED },
            lunchCounted = records.count { it.mealType.name == "LUNCH" && it.status == MealStatus.COUNTED },
            dinnerCounted = records.count { it.mealType.name == "DINNER" && it.status == MealStatus.COUNTED }
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        MonthlyMealSummary(0, 0, 0, 0, 0, 0, 0)
    )

    val monthRiceSummary: StateFlow<MonthlyRiceSummary> = combine(
        monthRiceTransactions,
        mealRiceRepo.currentRiceStockKg
    ) { txs, currentStock ->
        MonthlyRiceSummary(
            totalAddedKg = txs.filter { it.type == RiceTransactionType.RICE_ADDED }.sumOf { it.quantityKg },
            totalConsumedKg = txs.filter { it.type.isDeduction }.sumOf { it.quantityKg },
            totalAdjustmentsKg = txs.filter { it.type == RiceTransactionType.MANUAL_ADJUSTMENT }.sumOf { it.quantityKg },
            remainingStockKg = currentStock
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        MonthlyRiceSummary(0.0, 0.0, 0.0, 0.0)
    )

    val monthStudyStats: StateFlow<StudyStatistics> = _selectedMonth
        .flatMapLatest { studyRepo.observeStatistics(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), StudyStatistics())

    val calendarMarkers: StateFlow<Map<LocalDate, CalendarDayMarkers>> = combine(
        _selectedMonth,
        monthMeals,
        monthRiceTransactions,
        monthStudySessions
    ) { month, meals, rices, studies ->
        val mealsByDate = meals.groupBy { it.dateIso }
        val riceByDate = rices.groupBy { it.dateIso }
        val studyByDate = studies.groupBy { it.dateIso }

        val today = timeProvider.today()
        val map = mutableMapOf<LocalDate, CalendarDayMarkers>()
        for (day in 1..month.lengthOfMonth()) {
            val date = month.atDay(day)
            if (date.isAfter(today)) {
                map[date] = CalendarDayMarkers(date = date)
                continue
            }
            val iso = date.toString()
            val dayMeals = mealsByDate[iso].orEmpty()
            val dayRices = riceByDate[iso].orEmpty()
            val dayStudies = studyByDate[iso].orEmpty()

            val isOffDay = dayMeals.isNotEmpty() && dayMeals.all { it.status == MealStatus.OFF }
            val hasMealActivity = dayMeals.any { it.status == MealStatus.COUNTED || it.status == MealStatus.SKIPPED }
            val hasRiceActivity = dayRices.isNotEmpty()
            val hasStudyActivity = dayStudies.isNotEmpty()

            map[date] = CalendarDayMarkers(
                date = date,
                hasMealActivity = hasMealActivity,
                hasStudyActivity = hasStudyActivity,
                hasRiceActivity = hasRiceActivity,
                isMealOffDay = isOffDay
            )
        }
        map
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    private val _pdfState = MutableStateFlow(PdfGenerationState())
    val pdfState: StateFlow<PdfGenerationState> = _pdfState.asStateFlow()

    fun canNavigateNextMonth(month: YearMonth = _selectedMonth.value): Boolean =
        month.isBefore(timeProvider.currentYearMonth())

    fun previousMonth() {
        _selectedMonth.value = _selectedMonth.value.minusMonths(1)
        _pdfState.value = PdfGenerationState()
    }

    fun nextMonth() {
        val currentMonth = timeProvider.currentYearMonth()
        val candidate = _selectedMonth.value.plusMonths(1)
        if (!candidate.isAfter(currentMonth)) {
            _selectedMonth.value = candidate
            _pdfState.value = PdfGenerationState()
        }
    }

    fun selectMonth(yearMonth: YearMonth) {
        val currentMonth = timeProvider.currentYearMonth()
        _selectedMonth.value = if (yearMonth.isAfter(currentMonth)) currentMonth else yearMonth
        _pdfState.value = PdfGenerationState()
    }

    fun ensureDateLoaded(date: LocalDate) {
        if (date.isAfter(timeProvider.today())) return
        viewModelScope.launch {
            mealRiceRepo.ensureDateRecordsExist(date)
        }
    }

    fun observeDateDetail(date: LocalDate): Flow<DateDetailState> {
        val prevDay = date.minusDays(1)
        return combine(
            mealRiceRepo.observeMealsForDate(date),
            mealRiceRepo.observeCyclesInRange(prevDay, date),
            mealRiceRepo.observeRiceByDate(date),
            studyRepo.observeSessionsForDate(date)
        ) { meals, cycles, rices, studies ->
            DateDetailState(
                date = date,
                meals = meals,
                morningCycle = cycles.firstOrNull { it.startDateIso == prevDay.toString() },
                dinnerCycle = cycles.firstOrNull { it.startDateIso == date.toString() },
                riceTransactions = rices,
                studySessions = studies
            )
        }
    }

    fun generateMonthlyPdf(localizedContext: Context, locale: Locale, onReady: (File) -> Unit = {}) {
        val month = _selectedMonth.value
        viewModelScope.launch {
            _pdfState.value = PdfGenerationState(isGenerating = true)
            try {
                val profile = profileRepo.getProfileOnce() ?: ProfileEntity(name = "Student")
                val mealSummary = mealRiceRepo.getMonthlyMealSummary(month)
                val riceSummary = mealRiceRepo.getMonthlyRiceSummary(month)
                val studyStats = studyRepo.getMonthlyStudySummary(month)
                val file = pdfGenerator.generateMonthlyReport(
                    localizedContext = localizedContext,
                    report = MonthlyReportData(
                        yearMonth = month,
                        profile = profile,
                        mealSummary = mealSummary,
                        riceSummary = riceSummary,
                        studyStats = studyStats
                    ),
                    locale = locale
                )
                _pdfState.value = PdfGenerationState(
                    isGenerating = false,
                    generatedFile = file,
                    generatedForMonth = month
                )
                onReady(file)
            } catch (e: Exception) {
                _pdfState.value = PdfGenerationState(
                    isGenerating = false,
                    error = e.message ?: "PDF generation failed"
                )
            }
        }
    }

    class Factory(private val container: AppContainer) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return CalendarRecordsViewModel(container) as T
        }
    }
}
