package com.example.todoapplication.domain.validation

import com.example.todoapplication.domain.model.Reminder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReminderValidatorTest {
    @Test
    fun supportedReminderHasNoErrors() {
        val reminder = Reminder(
            id = 0,
            todoId = 1,
            minutesBefore = Reminder.FIFTEEN_MINUTES_BEFORE,
        )

        assertTrue(ReminderValidator.validate(reminder).isEmpty())
    }

    @Test
    fun reminderRequiresTodoAndSupportedOffset() {
        val reminder = Reminder(id = 0, todoId = 0, minutesBefore = 10)

        assertEquals(
            setOf(
                ReminderValidationError.INVALID_TODO_ID,
                ReminderValidationError.UNSUPPORTED_MINUTES_BEFORE,
            ),
            ReminderValidator.validate(reminder),
        )
    }

    @Test
    fun reminderTodoMustMatchReplacementTarget() {
        val reminder = Reminder(id = 0, todoId = 2, minutesBefore = 15)

        assertEquals(
            setOf(ReminderValidationError.TODO_ID_MISMATCH),
            ReminderValidator.validate(reminder, expectedTodoId = 1),
        )
    }
}
