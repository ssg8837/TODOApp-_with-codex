package com.example.todoapplication.domain.validation

import com.example.todoapplication.domain.model.Reminder

enum class ReminderValidationError {
    INVALID_TODO_ID,
    TODO_ID_MISMATCH,
    UNSUPPORTED_MINUTES_BEFORE,
}

object ReminderValidator {
    fun validate(
        reminder: Reminder,
        expectedTodoId: Long? = null,
    ): Set<ReminderValidationError> = buildSet {
        if (reminder.todoId <= 0) add(ReminderValidationError.INVALID_TODO_ID)
        if (expectedTodoId != null && reminder.todoId != expectedTodoId) {
            add(ReminderValidationError.TODO_ID_MISMATCH)
        }
        if (reminder.minutesBefore !in Reminder.SUPPORTED_MINUTES_BEFORE) {
            add(ReminderValidationError.UNSUPPORTED_MINUTES_BEFORE)
        }
    }
}
