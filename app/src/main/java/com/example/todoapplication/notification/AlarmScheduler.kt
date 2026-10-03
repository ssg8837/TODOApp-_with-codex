package com.example.todoapplication.notification

import com.example.todoapplication.domain.model.Reminder
import com.example.todoapplication.domain.model.Todo

/**
 * Domain TODO/Reminder를 OS Alarm 예약·취소 동작으로 연결하는 외부 시스템 abstraction.
 * Repository에 숨기지 않으며 Application Service가 영속 상태와 함께 조정한다.
 */
interface AlarmScheduler {
    fun schedule(todo: Todo, reminder: Reminder): AlarmOperationResult

    fun cancel(reminder: Reminder): AlarmOperationResult
}

/** Alarm 요청의 성공, fallback, 제외 또는 실패 의미. */
enum class AlarmOperationResult {
    SUCCESS,
    INEXACT_SCHEDULED,
    SKIPPED_PAST,
    NOTIFICATION_PERMISSION_DENIED,
    FAILURE,
}
