package com.example.todoapplication.domain.repository

import com.example.todoapplication.domain.model.Reminder
import java.time.LocalDate
import java.time.LocalTime
import kotlinx.coroutines.flow.Flow

/**
 * Reminder 영속 상태를 Domain 모델로 다루는 Repository 계약.
 *
 * OS Alarm 예약은 포함하지 않으며, 교체 동작의 transaction은 DAO 구현에 위임한다.
 */
interface ReminderRepository {
    suspend fun create(reminder: Reminder): Reminder

    fun observeByTodoId(todoId: Long): Flow<List<Reminder>>

    suspend fun getById(id: Long): Reminder?

    suspend fun deleteById(id: Long): Boolean

    suspend fun deleteByTodoId(todoId: Long): Int

    /** 기존 항목 삭제와 신규 목록 저장을 원자적으로 수행하고 저장된 Domain 모델을 반환한다. */
    suspend fun replaceReminders(
        todoId: Long,
        reminders: List<Reminder>,
    ): List<Reminder>

    /** 재부팅 후 Alarm 복구를 위해 현재 DB 기준의 미래 후보를 조회한다. */
    suspend fun getFutureAlarmRecoveryCandidates(
        currentDate: LocalDate,
        currentTime: LocalTime,
    ): List<Reminder>
}
