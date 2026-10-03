package com.example.todoapplication.notification

import com.example.todoapplication.domain.model.Reminder
import com.example.todoapplication.domain.model.Todo
import com.example.todoapplication.domain.reminder.ReminderCalculator
import java.time.ZoneId

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
