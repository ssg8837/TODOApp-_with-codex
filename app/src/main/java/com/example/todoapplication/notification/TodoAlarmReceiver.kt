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

/**
 * Reminder Alarm을 받아 최신 DB 상태를 재조회한 뒤 유효한 경우에만 Notification을 표시한다.
 *
 * Intent의 ID와 예정 시각만으로 알림을 결정하지 않으며 삭제·완료·변경된 Reminder를 제외한다.
 * 알림 권한이 없으면 안전하게 종료한다. DB 조회는 [goAsync] 이후 IO coroutine에서 수행하고
 * 모든 coroutine 종료 경로에서 `PendingResult.finish()`를 호출한다.
 */
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
