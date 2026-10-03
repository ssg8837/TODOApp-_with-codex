package com.example.todoapplication.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import com.example.todoapplication.R

object NotificationChannels {
    const val TODO_REMINDERS = "todo_reminders"

    fun create(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(
                TODO_REMINDERS,
                context.getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply {
                description = context.getString(R.string.notification_channel_description)
            },
        )
    }
}
