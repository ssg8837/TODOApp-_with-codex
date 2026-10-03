package com.example.todoapplication.domain.reminder

import com.example.todoapplication.domain.model.Reminder
import com.example.todoapplication.domain.model.Todo
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ReminderCalculatorTest {
    @Test
    fun calculatesBothSupportedOffsetsFromTodoLocalDateTime() {
        val todo = todo(LocalDate.of(2026, 9, 20), LocalTime.of(12, 0))
        val zone = ZoneId.of("Asia/Seoul")

        assertEquals(
            Instant.parse("2026-09-19T03:00:00Z"),
            ReminderCalculator.scheduledAt(todo, reminder(1, Reminder.ONE_DAY_BEFORE), zone),
        )
        assertEquals(
            Instant.parse("2026-09-20T02:45:00Z"),
            ReminderCalculator.scheduledAt(todo, reminder(2, Reminder.FIFTEEN_MINUTES_BEFORE), zone),
        )
    }

    @Test
    fun returnsNullWhenTodoHasNoTime() {
        assertNull(
            ReminderCalculator.scheduledAt(
                todo(LocalDate.of(2026, 9, 20), null),
                reminder(1, Reminder.FIFTEEN_MINUTES_BEFORE),
                ZoneId.of("UTC"),
            ),
        )
    }

    @Test
    fun resolvesDaylightSavingBoundaryUsingInjectedZone() {
        val todo = todo(LocalDate.of(2026, 3, 29), LocalTime.of(3, 30))

        assertEquals(
            Instant.parse("2026-03-28T01:30:00Z"),
            ReminderCalculator.scheduledAt(
                todo,
                reminder(1, Reminder.ONE_DAY_BEFORE),
                ZoneId.of("Europe/Berlin"),
            ),
        )
    }

    private fun reminder(id: Long, minutes: Int) = Reminder(id, 1, minutes)

    private fun todo(date: LocalDate, time: LocalTime?) = Todo(
        id = 1,
        title = "TODO",
        date = date,
        time = time,
        categoryId = 1,
        isCompleted = false,
        createdAt = Instant.EPOCH,
        updatedAt = Instant.EPOCH,
    )
}
