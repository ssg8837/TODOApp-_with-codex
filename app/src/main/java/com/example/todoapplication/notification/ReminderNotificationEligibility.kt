package com.example.todoapplication.notification

import com.example.todoapplication.domain.model.Reminder
import com.example.todoapplication.domain.model.Todo
import com.example.todoapplication.domain.reminder.ReminderCalculator
import java.time.ZoneId

/**
 * Alarm Intent의 과거 정보를 신뢰하지 않고 최신 TODO·Reminder로 알림 가능 여부를 판정한다.
 * 삭제·완료·연결 변경·발화 시각 변경이 있으면 Notification을 표시하지 않는다.
 */
object ReminderNotificationEligibility {
    fun canNotify(
        todo: Todo?,
        reminder: Reminder?,
        expectedScheduledAtEpochMillis: Long,
        zoneId: ZoneId,
    ): Boolean {
        if (todo == null || reminder == null || todo.isCompleted || reminder.todoId != todo.id) {
            return false
        }
        return ReminderCalculator.scheduledAt(todo, reminder, zoneId)?.toEpochMilli() ==
            expectedScheduledAtEpochMillis
    }
}
