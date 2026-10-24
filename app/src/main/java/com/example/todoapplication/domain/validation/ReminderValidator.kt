package com.example.todoapplication.domain.validation

import com.example.todoapplication.domain.model.Reminder

/** Reminder 도메인 검증 실패 사유. */
enum class ReminderValidationError {
    INVALID_TODO_ID,
    TODO_ID_MISMATCH,
    UNSUPPORTED_MINUTES_BEFORE,
    DUPLICATE_MINUTES_BEFORE,
}

/** Reminder의 TODO 식별자와 양수 분 단위 간격, 중복 여부를 저장소 접근 없이 검증한다. */
object ReminderValidator {
    fun isValidOffset(minutesBefore: Int): Boolean =
        minutesBefore in 1..Reminder.MAX_MINUTES_BEFORE

    fun validate(
        reminder: Reminder,
        expectedTodoId: Long? = null,
    ): Set<ReminderValidationError> = buildSet {
        if (reminder.todoId <= 0) add(ReminderValidationError.INVALID_TODO_ID)
        if (expectedTodoId != null && reminder.todoId != expectedTodoId) {
            add(ReminderValidationError.TODO_ID_MISMATCH)
        }
        if (!isValidOffset(reminder.minutesBefore)) {
            add(ReminderValidationError.UNSUPPORTED_MINUTES_BEFORE)
        }
    }

    fun validateAll(
        reminders: List<Reminder>,
        expectedTodoId: Long? = null,
    ): Set<ReminderValidationError> = buildSet {
        val seenOffsets = mutableSetOf<Int>()
        reminders.forEach { reminder ->
            addAll(validate(reminder, expectedTodoId))
            if (!seenOffsets.add(reminder.minutesBefore)) {
                add(ReminderValidationError.DUPLICATE_MINUTES_BEFORE)
            }
        }
    }
}
