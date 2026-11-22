package com.example.notifications

import android.Manifest
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.MessMateApplication
import com.example.R
import com.example.util.LocaleHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        val pendingResult = goAsync()
        val app = context.applicationContext as? MessMateApplication
        val language = LocaleHelper.getSavedLanguageSync(context)
        val localized = LocaleHelper.createLocalizedContext(context, language)

        CoroutineScope(Dispatchers.IO).launch {
            try {
                NotificationChannels.createChannels(localized)
                // Always reconcile meals safely when triggered
                app?.container?.mealRiceRepository?.ensureDefaultsAndReconcile()

                when (action) {
                    ReminderScheduler.ACTION_MEAL_REMINDER -> {
                        val settings = app?.container?.profileSettingsRepository?.getAppSettingsOnce()
                        if (settings?.mealReminderEnabled != false) {
                            showNotification(
                                context = localized,
                                channelId = NotificationChannels.CHANNEL_MEAL,
                                notificationId = 1001,
                                title = localized.getString(R.string.notif_meal_title),
                                body = localized.getString(R.string.notif_meal_body)
                            )
                            app?.container?.reminderScheduler?.scheduleMealReminder(true)
                        }
                    }

                    ReminderScheduler.ACTION_STUDY_REMINDER -> {
                        val reminderId = intent.getLongExtra(ReminderScheduler.EXTRA_REMINDER_ID, -1L)
                        val label = intent.getStringExtra(ReminderScheduler.EXTRA_REMINDER_LABEL).orEmpty()
                        val reminder = app?.container?.studyRepository?.getAllStudyRemindersOnce()
                            ?.firstOrNull { it.id == reminderId }

                        if (reminder == null || reminder.isEnabled) {
                            val body = label.ifBlank { localized.getString(R.string.notif_study_body_default) }
                            showNotification(
                                context = localized,
                                channelId = NotificationChannels.CHANNEL_STUDY,
                                notificationId = 2000 + (reminderId.toInt().coerceAtLeast(1) and 0x0FFF),
                                title = localized.getString(R.string.notif_study_title),
                                body = body
                            )
                            if (reminder != null) {
                                app.container.reminderScheduler.scheduleStudyReminder(reminder)
                            }
                        }
                    }
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun showNotification(
        context: Context,
        channelId: String,
        notificationId: Int,
        title: String,
        body: String
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) return
        }

        val launchIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentIntent = PendingIntent.getActivity(
            context,
            notificationId,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(contentIntent)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context).notify(notificationId, notification)
    }
}
