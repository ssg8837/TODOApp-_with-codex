package com.example.todoapplication.notification

import com.example.todoapplication.domain.model.Reminder
import com.example.todoapplication.domain.model.Todo
import com.example.todoapplication.domain.reminder.ReminderCalculator
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReminderNotificationEligibilityTest {
    private val reminder = Reminder(5, 2, Reminder.FIFTEEN_MINUTES_BEFORE)
    private val todo = Todo(
        id = 2,
        title = "TODO",
        date = LocalDate.of(2026, 9, 21),
        time = LocalTime.NOON,
        categoryId = 1,
        isCompleted = false,
        createdAt = Instant.EPOCH,
        updatedAt = Instant.EPOCH,
    )
    private val scheduledAt = checkNotNull(
        ReminderCalculator.scheduledAt(todo, reminder, ZoneOffset.UTC),
    ).toEpochMilli()

    @Test
    fun onlyCurrentMatchingIncompleteTodoAndReminderCanNotify() {
        assertTrue(eligible(todo, reminder, scheduledAt))
        assertFalse(eligible(null, reminder, scheduledAt))
        assertFalse(eligible(todo, null, scheduledAt))
        assertFalse(eligible(todo.copy(isCompleted = true), reminder, scheduledAt))
        assertFalse(eligible(todo, reminder.copy(todoId = 99), scheduledAt))
        assertFalse(eligible(todo.copy(time = LocalTime.of(13, 0)), reminder, scheduledAt))
    }

    private fun eligible(todo: Todo?, reminder: Reminder?, expected: Long) =
        ReminderNotificationEligibility.canNotify(todo, reminder, expected, ZoneOffset.UTC)
}
