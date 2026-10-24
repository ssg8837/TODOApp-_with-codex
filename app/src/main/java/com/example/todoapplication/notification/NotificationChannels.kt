package com.example.todoapplication.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import com.example.todoapplication.R

/** TODO Reminder Notification이 사용하는 Android notification channel을 생성한다. */
object NotificationChannels {
    const val LEGACY_TODO_REMINDERS = "todo_reminders"
    const val TODO_REMINDERS = "todo_reminders_v2"

    val DEFAULT_VIBRATION_PATTERN = longArrayOf(0L, 250L, 150L, 250L)

    fun create(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        manager.createNotificationChannel(
            NotificationChannel(
                TODO_REMINDERS,
                context.getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply {
                description = context.getString(R.string.notification_channel_description)
                enableVibration(true)
                vibrationPattern = DEFAULT_VIBRATION_PATTERN
            },
        )
    }
}
