package com.example

import android.app.Application
import android.content.Context
import com.example.data.local.MessMateDatabase
import com.example.data.local.PreferencesStore
import com.example.data.repository.CsvSubjectImporter
import com.example.data.repository.MealRiceRepository
import com.example.data.repository.ProfileSettingsRepository
import com.example.data.repository.StudyRepository
import com.example.notifications.NotificationChannels
import com.example.notifications.ReminderScheduler
import com.example.pdf.MonthlyPdfGenerator
import com.example.util.BangladeshTimeProvider
import com.example.util.TimeProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class AppContainer(
    context: Context,
    val timeProvider: TimeProvider = BangladeshTimeProvider(),
    databaseOverride: MessMateDatabase? = null
) {
    private val appContext = context.applicationContext ?: context

    // Lazily initialize heavy disk/database/PDF/alarm dependencies off the Application.onCreate critical path
    val database: MessMateDatabase by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        databaseOverride ?: MessMateDatabase.getInstance(appContext)
    }
    val preferencesStore: PreferencesStore by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        PreferencesStore(appContext)
    }
    val csvSubjectImporter: CsvSubjectImporter by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        CsvSubjectImporter(appContext, database.subjectDao())
    }
    val mealRiceRepository: MealRiceRepository by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        MealRiceRepository(database, timeProvider, preferencesStore)
    }
    val studyRepository: StudyRepository by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        StudyRepository(database, timeProvider)
    }
    val profileSettingsRepository: ProfileSettingsRepository by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        ProfileSettingsRepository(appContext, database, preferencesStore, timeProvider)
    }
    val reminderScheduler: ReminderScheduler by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        ReminderScheduler(appContext, timeProvider)
    }
    val pdfGenerator: MonthlyPdfGenerator by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        MonthlyPdfGenerator()
    }
}

class MessMateApplication : Application() {
    lateinit var container: AppContainer
        private set

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)

        // Defer notification channel creation, CSV seed check, default study settings, and alarm sync
        // to Dispatchers.IO so cold start does zero blocking work on the main thread.
        // Meal reconciliation is triggered once by MessMateViewModel on IO to avoid duplicate contention.
        appScope.launch {
            runCatching {
                NotificationChannels.createChannels(this@MessMateApplication)
                container.csvSubjectImporter.importInitialSubjectsIfNeeded()
                container.studyRepository.ensureDefaultStudySettings()

                val settings = container.profileSettingsRepository.getAppSettingsOnce()
                val reminders = container.studyRepository.getAllStudyRemindersOnce()
                container.reminderScheduler.syncAllReminders(
                    mealReminderEnabled = settings.mealReminderEnabled,
                    studyReminders = reminders
                )
            }.onFailure { error ->
                android.util.Log.e("MessMateApplication", "Startup initialization failed", error)
            }
        }
    }
}
