package com.example.ui.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.AppContainer
import com.example.data.local.AppLanguage
import com.example.data.local.AppSettingsEntity
import com.example.data.local.MealCycleEntity
import com.example.data.local.MealRecordEntity
import com.example.data.local.MealStatus
import com.example.data.local.MealType
import com.example.data.local.ProfileEntity
import com.example.data.local.RiceTransactionEntity
import com.example.data.local.StudyReminderEntity
import com.example.data.local.StudySessionEntity
import com.example.data.local.StudySettingsEntity
import com.example.data.local.ThemeMode
import com.example.data.local.UserPreferences
import com.example.data.repository.DeadlineDecision
import com.example.data.repository.MealToggleResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

sealed class RecentActivityItem(val timestampEpochMs: Long) {
    data class Rice(val tx: RiceTransactionEntity) : RecentActivityItem(tx.timestampEpochMs)
    data class Study(val session: StudySessionEntity) : RecentActivityItem(session.endTimeEpochMs)
    data class Meal(val record: MealRecordEntity) : RecentActivityItem(record.updatedAtEpochMs)
}

class MessMateViewModel(private val container: AppContainer) : ViewModel() {

    val timeProvider = container.timeProvider
    val mealRiceRepo = container.mealRiceRepository
    val studyRepo = container.studyRepository
    val profileRepo = container.profileSettingsRepository
    val reminderScheduler = container.reminderScheduler

    val preferencesState: StateFlow<UserPreferences?> = profileRepo.preferences
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val preferences: StateFlow<UserPreferences> = preferencesState
        .map { it ?: UserPreferences() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UserPreferences())

    val profile: StateFlow<ProfileEntity?> = profileRepo.profile
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val appSettings: StateFlow<AppSettingsEntity> = profileRepo.appSettings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppSettingsEntity())

    val studySettings: StateFlow<StudySettingsEntity> = studyRepo.studySettings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), StudySettingsEntity())

    val studyReminders: StateFlow<List<StudyReminderEntity>> = studyRepo.studyReminders
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val currentRiceStockKg: StateFlow<Double> = mealRiceRepo.currentRiceStockKg
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val allCycles: StateFlow<List<MealCycleEntity>> = mealRiceRepo.allCycles
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val todayDate: LocalDate
        get() = timeProvider.today()

    val todayMeals: StateFlow<List<MealRecordEntity>> = mealRiceRepo.observeMealsForDate(timeProvider.today())
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val tomorrowMeals: StateFlow<List<MealRecordEntity>> = mealRiceRepo.observeMealsForDate(timeProvider.today().plusDays(1))
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val todayStudySeconds: StateFlow<Long> = studyRepo.observeTodayStudySeconds(timeProvider.today())
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    // Lightweight 1-row query for HomeScreen "Last focus" card — avoids loading the entire study_sessions table on startup
    val latestStudySession: StateFlow<StudySessionEntity?> = studyRepo.observeRecentSessions(1)
        .map { it.firstOrNull() }
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val recentActivity: StateFlow<List<RecentActivityItem>> = combine(
        mealRiceRepo.observeRecentRice(6),
        studyRepo.observeRecentSessions(6),
        mealRiceRepo.observeRecentMeals(6)
    ) { riceList, studyList, mealList ->
        val items = mutableListOf<RecentActivityItem>()
        riceList.filter { it.relatedMealId == null }.forEach { items.add(RecentActivityItem.Rice(it)) }
        studyList.forEach { items.add(RecentActivityItem.Study(it)) }
        mealList.filter { it.status == MealStatus.COUNTED || it.status == MealStatus.SKIPPED }
            .forEach { items.add(RecentActivityItem.Meal(it)) }
        items.sortedByDescending { it.timestampEpochMs }.take(6)
    }
        .flowOn(Dispatchers.Default)
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _lastMealToggleResult = MutableStateFlow<MealToggleResult?>(null)
    val lastMealToggleResult: StateFlow<MealToggleResult?> = _lastMealToggleResult.asStateFlow()

    init {
        reconcileNow()
    }

    fun reconcileNow() {
        viewModelScope.launch(Dispatchers.IO) {
            mealRiceRepo.ensureDefaultsAndReconcile()
        }
    }

    fun currentDeadlineDecision(): DeadlineDecision = mealRiceRepo.evaluateDeadlineWindow()

    fun toggleMealOnOff(newIsOn: Boolean) {
        viewModelScope.launch {
            val result = mealRiceRepo.toggleMealOnOff(newIsOn)
            _lastMealToggleResult.value = result
        }
    }

    fun dismissMealToggleNotice() {
        _lastMealToggleResult.value = null
    }

    fun skipMeal(date: LocalDate, mealType: MealType) {
        viewModelScope.launch {
            mealRiceRepo.skipMeal(date, mealType)
        }
    }

    fun restoreMeal(date: LocalDate, mealType: MealType) {
        viewModelScope.launch {
            mealRiceRepo.restoreMeal(date, mealType)
        }
    }

    fun setCycleStatusForDate(cycleStartDate: LocalDate, isOn: Boolean) {
        viewModelScope.launch {
            mealRiceRepo.setCycleStatusForDate(cycleStartDate, isOn)
        }
    }

    suspend fun addRiceDeposit(quantityKg: Double, date: LocalDate, note: String): Result<Long> =
        mealRiceRepo.addRiceDeposit(quantityKg, date, note)

    suspend fun addManualAdjustment(quantityKg: Double, date: LocalDate, note: String): Result<Long> =
        mealRiceRepo.addManualAdjustment(quantityKg, date, note)

    suspend fun updateRiceTransaction(tx: RiceTransactionEntity): Result<Unit> =
        mealRiceRepo.updateRiceTransaction(tx)

    fun deleteRiceTransaction(tx: RiceTransactionEntity) {
        viewModelScope.launch {
            mealRiceRepo.deleteRiceTransaction(tx)
        }
    }

    suspend fun completeInitialSetup(
        name: String,
        photoPath: String?,
        messName: String,
        institutionName: String,
        department: String,
        semester: String
    ): Result<Unit> {
        val result = profileRepo.completeInitialSetup(
            name = name,
            photoPath = photoPath,
            messName = messName,
            institutionName = institutionName,
            department = department,
            semester = semester
        )
        if (result.isSuccess) {
            mealRiceRepo.ensureDefaultsAndReconcile()
            val settings = profileRepo.getAppSettingsOnce()
            reminderScheduler.scheduleMealReminder(settings.mealReminderEnabled)
        }
        return result
    }

    suspend fun updateProfile(
        name: String,
        photoPath: String?,
        messName: String,
        institutionName: String,
        department: String,
        semester: String
    ): Result<Unit> = profileRepo.updateProfile(
        name = name,
        photoPath = photoPath,
        messName = messName,
        institutionName = institutionName,
        department = department,
        semester = semester
    )

    suspend fun copyPickedPhoto(uri: Uri): String? = profileRepo.copyPickedPhotoToInternalStorage(uri)

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { profileRepo.setThemeMode(mode) }
    }

    fun setLanguage(language: AppLanguage) {
        viewModelScope.launch { profileRepo.setLanguage(language) }
    }

    fun markNotificationPermissionPrompted() {
        viewModelScope.launch { profileRepo.setNotificationPermissionPrompted() }
    }

    fun setMealReminderEnabled(enabled: Boolean) {
        viewModelScope.launch {
            profileRepo.setMealReminderEnabled(enabled)
            reminderScheduler.scheduleMealReminder(enabled)
        }
    }

    suspend fun updateRiceSettings(breakfastKg: Double, lunchKg: Double, dinnerKg: Double): Result<Unit> =
        profileRepo.updateRiceConsumptionSettings(breakfastKg, lunchKg, dinnerKg)

    suspend fun updateStudySettings(
        dailyGoalMinutes: Int,
        pomodoroFocusMinutes: Int,
        shortBreakMinutes: Int,
        longBreakMinutes: Int
    ): Result<Unit> = studyRepo.updateStudySettings(
        dailyGoalMinutes = dailyGoalMinutes,
        pomodoroFocusMinutes = pomodoroFocusMinutes,
        shortBreakMinutes = shortBreakMinutes,
        longBreakMinutes = longBreakMinutes
    )

    fun addStudyReminder(hour: Int, minute: Int, label: String) {
        viewModelScope.launch {
            val id = studyRepo.addStudyReminder(hour, minute, label)
            reminderScheduler.scheduleStudyReminder(
                StudyReminderEntity(id = id, hour = hour, minute = minute, label = label.trim(), isEnabled = true)
            )
        }
    }

    fun updateStudyReminder(reminder: StudyReminderEntity) {
        viewModelScope.launch {
            studyRepo.updateStudyReminder(reminder)
            reminderScheduler.scheduleStudyReminder(reminder)
        }
    }

    fun deleteStudyReminder(reminder: StudyReminderEntity) {
        viewModelScope.launch {
            studyRepo.deleteStudyReminder(reminder)
            reminderScheduler.cancelStudyReminder(reminder.id)
        }
    }

    fun disableAllStudyReminders() {
        viewModelScope.launch {
            val existing = studyRepo.getAllStudyRemindersOnce()
            studyRepo.disableAllStudyReminders()
            existing.forEach { reminderScheduler.cancelStudyReminder(it.id) }
        }
    }

    class Factory(private val container: AppContainer) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return MessMateViewModel(container) as T
        }
    }
}
