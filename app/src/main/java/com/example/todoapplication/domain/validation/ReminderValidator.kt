package com.example.todoapplication.domain.validation

import com.example.todoapplication.domain.model.Reminder

enum class ReminderValidationError {
    INVALID_TODO_ID,
    UNSUPPORTED_MINUTES_BEFORE,
}

object ReminderValidator {
    fun validate(reminder: Reminder): Set<ReminderValidationError> = buildSet {
        if (reminder.todoId <= 0) add(ReminderValidationError.INVALID_TODO_ID)
        if (reminder.minutesBefore !in Reminder.SUPPORTED_MINUTES_BEFORE) {
            add(ReminderValidationError.UNSUPPORTED_MINUTES_BEFORE)
        }
    }
}
