package com.example.todoapplication.application.service

import com.example.todoapplication.domain.model.Todo
import com.example.todoapplication.domain.model.Reminder
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

/**
 * TODO 유스케이스와 Reminder/Alarm 동기화를 조정하는 Application Service 계약.
 *
 * Presenter에는 [ServiceResult]와 Domain 모델만 노출하며 DAO, Room, Android UI를 알지 않는다.
 */
interface TodoService {
    suspend fun create(todo: Todo): ServiceResult<Todo>

    suspend fun update(todo: Todo): ServiceResult<Todo>

    /** TODO와 선택 Reminder를 저장한 뒤 저장된 Reminder에 맞춰 OS Alarm을 동기화한다. */
    suspend fun saveWithReminders(
        todo: Todo,
        reminderMinutesBefore: Set<Int>,
    ): ServiceResult<TodoSaveResult> = error("Reminder save is not implemented")

    fun observeReminders(todoId: Long): Flow<ServiceResult<List<Reminder>>> =
        error("Reminder observation is not implemented")

    /** 현재 저장 상태를 기준으로 TODO의 Alarm을 다시 맞춘다. */
    suspend fun resynchronizeAlarms(todoId: Long): ServiceResult<AlarmSyncStatus> =
        error("Alarm synchronization is not implemented")

    suspend fun delete(todoId: Long): ServiceResult<Unit>

    /** 완료 시 Alarm을 취소하고 미완료로 복귀하면 유효한 Alarm을 다시 예약한다. */
    suspend fun setCompleted(todoId: Long, completed: Boolean): ServiceResult<Todo>

    suspend fun getById(todoId: Long): ServiceResult<Todo>

    fun observeByDate(date: LocalDate): Flow<ServiceResult<List<Todo>>>

    fun observeFiltered(
        date: LocalDate,
        categoryId: Long?,
        incompleteOnly: Boolean,
    ): Flow<ServiceResult<List<Todo>>>
}

/** TODO 저장 결과와 후속 Alarm 동기화 상태를 함께 전달한다. */
data class TodoSaveResult(
    val todo: Todo,
    val alarmSyncStatus: AlarmSyncStatus,
)

/** 영속화 성공과 별개로 수행되는 Alarm 동기화 결과. */
enum class AlarmSyncStatus {
    SYNCHRONIZED,
    INEXACT_SCHEDULED,
    NOTIFICATION_PERMISSION_DENIED,
    FAILED,
}
