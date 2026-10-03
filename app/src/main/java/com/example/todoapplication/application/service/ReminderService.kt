package com.example.todoapplication.application.service

import com.example.todoapplication.domain.model.Reminder
import java.time.LocalDate
import java.time.LocalTime
import kotlinx.coroutines.flow.Flow

/**
 * Reminder 데이터 유스케이스와 도메인 검증을 제공하는 Application Service 계약.
 *
 * TODO 존재 및 시간 유무를 확인하지만 OS Alarm 예약은 직접 수행하지 않는다.
 */
interface ReminderService {
    suspend fun create(reminder: Reminder): ServiceResult<Reminder>

    fun observeByTodoId(todoId: Long): Flow<ServiceResult<List<Reminder>>>

    suspend fun getById(reminderId: Long): ServiceResult<Reminder>

    /** 검증된 전체 목록을 Repository의 단일 원자적 교체 동작으로 저장한다. */
    suspend fun replaceReminders(
        todoId: Long,
        reminders: List<Reminder>,
    ): ServiceResult<List<Reminder>>

    suspend fun delete(reminderId: Long): ServiceResult<Unit>

    suspend fun deleteByTodoId(todoId: Long): ServiceResult<Int>

    /** 부팅 복구 시 재검증할 미래 Reminder 후보를 조회한다. */
    suspend fun getFutureAlarmRecoveryCandidates(
        currentDate: LocalDate,
        currentTime: LocalTime,
    ): ServiceResult<List<Reminder>>
}
