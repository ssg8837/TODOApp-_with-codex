package com.example.todoapplication.notification

import android.Manifest
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import com.example.todoapplication.domain.model.Reminder
import com.example.todoapplication.domain.model.Todo
import com.example.todoapplication.domain.reminder.ReminderCalculator
import java.time.Clock
import java.time.ZoneId

/**
 * [AlarmManager]를 사용하는 [AlarmScheduler] 구현.
 *
 * 알림 권한과 미래 시각을 확인한 뒤 exact Alarm이 가능하면 사용하고, 권한이 없거나
 * `SecurityException`이 발생하면 inexact 방식으로 fallback한다. Reminder의 안정적인 URI를
 * PendingIntent identity로 사용해 동일 Reminder 재예약은 갱신되고 서로 다른 TODO/Reminder는
 * 충돌하지 않는다. Android API 실패는 [AlarmOperationResult]로 경계화한다.
 */
class AndroidAlarmScheduler internal constructor(
    context: Context,
    private val clock: Clock = Clock.systemDefaultZone(),
    private val zoneId: ZoneId = ZoneId.systemDefault(),
    private val backend: AlarmBackend = SystemAlarmBackend(
        context.applicationContext.getSystemService(AlarmManager::class.java),
    ),
    private val notificationPermissionGranted: () -> Boolean = {
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            context.applicationContext.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
    },
) : AlarmScheduler {
    private val applicationContext = context.applicationContext

    override fun schedule(todo: Todo, reminder: Reminder): AlarmOperationResult {
        val scheduledAt = ReminderCalculator.scheduledAt(todo, reminder, zoneId)
            ?: return AlarmOperationResult.FAILURE
        if (!scheduledAt.isAfter(clock.instant())) return AlarmOperationResult.SKIPPED_PAST
        if (!notificationPermissionGranted()) return AlarmOperationResult.NOTIFICATION_PERMISSION_DENIED
        val operation = pendingIntent(
            reminder,
            scheduledAt.toEpochMilli(),
            PendingIntent.FLAG_UPDATE_CURRENT,
        ) ?: return AlarmOperationResult.FAILURE
        return try {
            var usedInexact = false
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S || backend.canScheduleExactAlarms()) {
                try {
                    backend.scheduleExact(scheduledAt.toEpochMilli(), operation)
                } catch (_: SecurityException) {
                    backend.scheduleInexact(scheduledAt.toEpochMilli(), operation)
                    usedInexact = true
                }
            } else {
                backend.scheduleInexact(scheduledAt.toEpochMilli(), operation)
                usedInexact = true
            }
            if (usedInexact) {
                AlarmOperationResult.INEXACT_SCHEDULED
            } else {
                AlarmOperationResult.SUCCESS
            }
        } catch (_: RuntimeException) {
            AlarmOperationResult.FAILURE
        }
    }

    override fun cancel(reminder: Reminder): AlarmOperationResult = try {
        pendingIntent(reminder, 0, PendingIntent.FLAG_NO_CREATE)?.let(backend::cancel)
        AlarmOperationResult.SUCCESS
    } catch (_: RuntimeException) {
        AlarmOperationResult.FAILURE
    }

    private fun pendingIntent(
        reminder: Reminder,
        scheduledAtEpochMillis: Long,
        creationFlag: Int,
    ): PendingIntent? = PendingIntent.getBroadcast(
        applicationContext,
        REQUEST_CODE,
        Intent(applicationContext, TodoAlarmReceiver::class.java).apply {
            action = ACTION_REMINDER
            data = AlarmIdentity.uri(reminder)
            putExtra(EXTRA_REMINDER_ID, reminder.id)
            putExtra(EXTRA_SCHEDULED_AT, scheduledAtEpochMillis)
        },
        creationFlag or PendingIntent.FLAG_IMMUTABLE,
    )

    companion object {
        const val ACTION_REMINDER = "com.example.todoapplication.action.REMINDER"
        const val EXTRA_REMINDER_ID = "reminder_id"
        const val EXTRA_SCHEDULED_AT = "scheduled_at"
        private const val REQUEST_CODE = 0
    }
}

/** AlarmManager 호출을 기기 없이 검증할 수 있게 분리한 내부 backend 계약. */
internal interface AlarmBackend {
    fun canScheduleExactAlarms(): Boolean
    fun scheduleExact(triggerAtMillis: Long, operation: PendingIntent)
    fun scheduleInexact(triggerAtMillis: Long, operation: PendingIntent)
    fun cancel(operation: PendingIntent)
}

private class SystemAlarmBackend(
    private val alarmManager: AlarmManager,
) : AlarmBackend {
    override fun canScheduleExactAlarms(): Boolean = alarmManager.canScheduleExactAlarms()

    override fun scheduleExact(triggerAtMillis: Long, operation: PendingIntent) {
        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, operation)
    }

    override fun scheduleInexact(triggerAtMillis: Long, operation: PendingIntent) {
        alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, operation)
    }

    override fun cancel(operation: PendingIntent) = alarmManager.cancel(operation)
}
