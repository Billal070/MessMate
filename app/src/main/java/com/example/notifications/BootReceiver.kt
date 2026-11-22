package com.example.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.MessMateApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        if (action != Intent.ACTION_BOOT_COMPLETED &&
            action != Intent.ACTION_TIMEZONE_CHANGED &&
            action != Intent.ACTION_TIME_CHANGED
        ) {
            return
        }

        val app = context.applicationContext as? MessMateApplication ?: return
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                app.container.mealRiceRepository.ensureDefaultsAndReconcile()
                val settings = app.container.profileSettingsRepository.getAppSettingsOnce()
                val reminders = app.container.studyRepository.getAllStudyRemindersOnce()
                app.container.reminderScheduler.syncAllReminders(
                    mealReminderEnabled = settings.mealReminderEnabled,
                    studyReminders = reminders
                )
            } finally {
                pendingResult.finish()
            }
        }
    }
}
