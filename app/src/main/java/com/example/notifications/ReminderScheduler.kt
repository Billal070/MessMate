package com.example.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.example.data.local.StudyReminderEntity
import com.example.util.TimeProvider
import java.time.LocalTime

class ReminderScheduler(
    private val context: Context,
    private val timeProvider: TimeProvider
) {
    private val alarmManager: AlarmManager?
        get() = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager

    fun scheduleMealReminder(enabled: Boolean) {
        val manager = alarmManager ?: return
        val pendingIntent = buildMealPendingIntent()
        if (!enabled) {
            manager.cancel(pendingIntent)
            return
        }
        val triggerMs = nextTriggerEpochMs(hour = MEAL_REMINDER_HOUR_BST, minute = MEAL_REMINDER_MINUTE_BST)
        runCatching {
            manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMs, pendingIntent)
        }
    }

    fun scheduleStudyReminder(reminder: StudyReminderEntity) {
        val manager = alarmManager ?: return
        val pendingIntent = buildStudyPendingIntent(reminder.id, reminder.label)
        if (!reminder.isEnabled) {
            manager.cancel(pendingIntent)
            return
        }
        val triggerMs = nextTriggerEpochMs(reminder.hour, reminder.minute)
        runCatching {
            manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMs, pendingIntent)
        }
    }

    fun cancelStudyReminder(reminderId: Long) {
        val manager = alarmManager ?: return
        val pendingIntent = buildStudyPendingIntent(reminderId, "")
        manager.cancel(pendingIntent)
    }

    fun syncAllReminders(mealReminderEnabled: Boolean, studyReminders: List<StudyReminderEntity>) {
        scheduleMealReminder(mealReminderEnabled)
        for (reminder in studyReminders) {
            scheduleStudyReminder(reminder)
        }
    }

    fun nextTriggerEpochMs(hour: Int, minute: Int): Long {
        val now = timeProvider.now()
        val targetTime = LocalTime.of(hour.coerceIn(0, 23), minute.coerceIn(0, 59))
        var target = timeProvider.atBangladeshDateTime(now.toLocalDate(), targetTime)
        if (!target.isAfter(now)) {
            target = target.plusDays(1)
        }
        return target.toInstant().toEpochMilli()
    }

    private fun buildMealPendingIntent(): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = ACTION_MEAL_REMINDER
        }
        return PendingIntent.getBroadcast(
            context,
            REQ_CODE_MEAL,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun buildStudyPendingIntent(reminderId: Long, label: String): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = ACTION_STUDY_REMINDER
            putExtra(EXTRA_REMINDER_ID, reminderId)
            putExtra(EXTRA_REMINDER_LABEL, label)
        }
        val requestCode = REQ_CODE_STUDY_BASE + (reminderId.toInt() and 0x0FFF)
        return PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    companion object {
        const val ACTION_MEAL_REMINDER = "com.example.messmate.ACTION_MEAL_REMINDER"
        const val ACTION_STUDY_REMINDER = "com.example.messmate.ACTION_STUDY_REMINDER"
        const val EXTRA_REMINDER_ID = "extra_reminder_id"
        const val EXTRA_REMINDER_LABEL = "extra_reminder_label"

        const val MEAL_REMINDER_HOUR_BST = 12
        const val MEAL_REMINDER_MINUTE_BST = 0

        private const val REQ_CODE_MEAL = 1001
        private const val REQ_CODE_STUDY_BASE = 2000
    }
}
