package com.example.todoapplication.notification

import android.Manifest
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import com.example.todoapplication.app.TodoApplication
import com.example.todoapplication.application.service.ServiceResult
import java.time.ZoneId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class TodoAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != AndroidAlarmScheduler.ACTION_REMINDER) return
        val reminderId = intent.getLongExtra(AndroidAlarmScheduler.EXTRA_REMINDER_ID, INVALID_ID)
        val scheduledAt = intent.getLongExtra(AndroidAlarmScheduler.EXTRA_SCHEDULED_AT, INVALID_TIME)
        if (reminderId <= 0 || scheduledAt <= 0 || !canPost(context)) return
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val application = context.applicationContext as TodoApplication
                val reminderResult = application.container.reminderService.getById(reminderId)
                val reminder = (reminderResult as? ServiceResult.Success)?.value ?: return@launch
                val todoResult = application.container.todoService.getById(reminder.todoId)
                val todo = (todoResult as? ServiceResult.Success)?.value ?: return@launch
                if (!ReminderNotificationEligibility.canNotify(
                        todo = todo,
                        reminder = reminder,
                        expectedScheduledAtEpochMillis = scheduledAt,
                        zoneId = ZoneId.systemDefault(),
                    )
                ) return@launch
                NotificationChannels.create(context)
                context.getSystemService(NotificationManager::class.java).notify(
                    AlarmIdentity.notificationId(reminder),
                    NotificationFactory.create(context, todo, reminder),
                )
            } finally {
                pending.finish()
            }
        }
    }

    private fun canPost(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED

    private companion object {
        const val INVALID_ID = -1L
        const val INVALID_TIME = -1L
    }
}
