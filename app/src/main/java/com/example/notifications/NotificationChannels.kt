package com.example.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import com.example.R

object NotificationChannels {
    const val CHANNEL_MEAL = "messmate_meal_reminder"
    const val CHANNEL_STUDY = "messmate_study_reminder"

    fun createChannels(context: Context) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return

        val mealChannel = NotificationChannel(
            CHANNEL_MEAL,
            context.getString(R.string.notif_channel_meal_name),
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = context.getString(R.string.notif_channel_meal_desc)
        }

        val studyChannel = NotificationChannel(
            CHANNEL_STUDY,
            context.getString(R.string.notif_channel_study_name),
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = context.getString(R.string.notif_channel_study_desc)
        }

        manager.createNotificationChannel(mealChannel)
        manager.createNotificationChannel(studyChannel)
    }
}
