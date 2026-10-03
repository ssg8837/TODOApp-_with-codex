package com.example.todoapplication.domain.reminder

import com.example.todoapplication.domain.model.Reminder
import com.example.todoapplication.domain.model.Todo
import java.time.Instant
import java.time.ZoneId

object ReminderCalculator {
    fun scheduledAt(
        todo: Todo,
        reminder: Reminder,
        zoneId: ZoneId,
    ): Instant? = todo.time
        ?.let { time -> todo.date.atTime(time).atZone(zoneId).toInstant() }
        ?.minusSeconds(reminder.minutesBefore * SECONDS_PER_MINUTE)

    private const val SECONDS_PER_MINUTE = 60L
}
