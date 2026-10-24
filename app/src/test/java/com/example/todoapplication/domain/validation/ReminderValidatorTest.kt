package com.example.todoapplication.domain.validation

import com.example.todoapplication.domain.model.Reminder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReminderValidatorTest {
    @Test
    fun presetAndCustomPositiveOffsetsHaveNoErrors() {
        val offsets = listOf(
            Reminder.FIFTEEN_MINUTES_BEFORE,
            Reminder.ONE_HOUR_BEFORE,
            Reminder.ONE_DAY_BEFORE,
            30,
            150,
            4_320,
            43_200,
        )

        offsets.forEach { offset ->
            val reminder = Reminder(id = 0, todoId = 1, minutesBefore = offset)
            assertTrue(ReminderValidator.validate(reminder).isEmpty())
        }
    }

    @Test
    fun reminderRequiresTodoAndPositiveOffsetWithinBounds() {
        val zeroOffset = Reminder(id = 0, todoId = 0, minutesBefore = 0)
        val negativeOffset = Reminder(id = 0, todoId = 1, minutesBefore = -15)
        val overflowOffset = Reminder(id = 0, todoId = 1, minutesBefore = Reminder.MAX_MINUTES_BEFORE + 1)

        assertEquals(
            setOf(
                ReminderValidationError.INVALID_TODO_ID,
                ReminderValidationError.UNSUPPORTED_MINUTES_BEFORE,
            ),
            ReminderValidator.validate(zeroOffset),
        )
        assertEquals(
            setOf(ReminderValidationError.UNSUPPORTED_MINUTES_BEFORE),
            ReminderValidator.validate(negativeOffset),
        )
        assertEquals(
            setOf(ReminderValidationError.UNSUPPORTED_MINUTES_BEFORE),
            ReminderValidator.validate(overflowOffset),
        )
    }

    @Test
    fun reminderTodoMustMatchReplacementTargetAndRejectDuplicates() {
        val mismatched = Reminder(id = 0, todoId = 2, minutesBefore = 15)
        assertEquals(
            setOf(ReminderValidationError.TODO_ID_MISMATCH),
            ReminderValidator.validate(mismatched, expectedTodoId = 1),
        )

        val duplicated = listOf(
            Reminder(id = 0, todoId = 1, minutesBefore = 150),
            Reminder(id = 0, todoId = 1, minutesBefore = 150),
        )
        assertEquals(
            setOf(ReminderValidationError.DUPLICATE_MINUTES_BEFORE),
            ReminderValidator.validateAll(duplicated, expectedTodoId = 1),
        )
    }
}
