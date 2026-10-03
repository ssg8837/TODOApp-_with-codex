package com.example.todoapplication.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import com.example.todoapplication.R

/** TODO Reminder Notification이 사용하는 Android notification channel을 생성한다. */
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
