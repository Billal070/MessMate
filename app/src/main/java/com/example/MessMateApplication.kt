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
    val database: MessMateDatabase = MessMateDatabase.getInstance(context)
) {
    val preferencesStore = PreferencesStore(context)
    val csvSubjectImporter = CsvSubjectImporter(context, database.subjectDao())
    val mealRiceRepository = MealRiceRepository(database, timeProvider)
    val studyRepository = StudyRepository(database, timeProvider)
    val profileSettingsRepository = ProfileSettingsRepository(context, database, preferencesStore, timeProvider)
    val reminderScheduler = ReminderScheduler(context, timeProvider)
    val pdfGenerator = MonthlyPdfGenerator()
}

class MessMateApplication : Application() {
    lateinit var container: AppContainer
        private set

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        NotificationChannels.createChannels(this)

        appScope.launch {
            runCatching {
                container.csvSubjectImporter.importInitialSubjectsIfNeeded()
                container.studyRepository.ensureDefaultStudySettings()
                container.mealRiceRepository.ensureDefaultsAndReconcile()

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
