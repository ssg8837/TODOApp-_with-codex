package com.example.todoapplication.notification

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.example.todoapplication.MainActivity
import com.example.todoapplication.R
import com.example.todoapplication.domain.model.Reminder
import com.example.todoapplication.domain.model.Todo
import com.example.todoapplication.domain.reminder.ReminderDurationFormatter

/** Reminder Notification과 TODO 확인 화면으로 이동하는 content PendingIntent를 구성한다. */
object NotificationFactory {
    fun create(context: Context, todo: Todo, reminder: Reminder): android.app.Notification {
        val contentIntent = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java).apply {
                data = AlarmIdentity.uri(reminder).buildUpon().authority("notification").build()
                putExtra(MainActivity.EXTRA_TODO_ID, todo.id)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val durationText = ReminderDurationFormatter.formatDuration(reminder.minutesBefore)
        val detail = context.getString(R.string.notification_reminder_before, durationText)
        return NotificationCompat.Builder(context, NotificationChannels.TODO_REMINDERS)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(todo.title)
            .setContentText(detail)
            .setContentIntent(contentIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
    }
}
