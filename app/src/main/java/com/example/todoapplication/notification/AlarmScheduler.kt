package com.example.todoapplication.notification

import com.example.todoapplication.domain.model.Reminder
import com.example.todoapplication.domain.model.Todo

interface AlarmScheduler {
    fun schedule(todo: Todo, reminder: Reminder): AlarmOperationResult

    fun cancel(reminder: Reminder): AlarmOperationResult
}

enum class AlarmOperationResult {
    SUCCESS,
    INEXACT_SCHEDULED,
    SKIPPED_PAST,
    NOTIFICATION_PERMISSION_DENIED,
    FAILURE,
}
