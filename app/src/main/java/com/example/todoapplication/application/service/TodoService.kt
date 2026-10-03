package com.example.todoapplication.application.service

import com.example.todoapplication.domain.model.Todo
import com.example.todoapplication.domain.model.Reminder
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

interface TodoService {
    suspend fun create(todo: Todo): ServiceResult<Todo>

    suspend fun update(todo: Todo): ServiceResult<Todo>

    suspend fun saveWithReminders(
        todo: Todo,
        reminderMinutesBefore: Set<Int>,
    ): ServiceResult<TodoSaveResult> = error("Reminder save is not implemented")

    fun observeReminders(todoId: Long): Flow<ServiceResult<List<Reminder>>> =
        error("Reminder observation is not implemented")

    suspend fun resynchronizeAlarms(todoId: Long): ServiceResult<AlarmSyncStatus> =
        error("Alarm synchronization is not implemented")

    suspend fun delete(todoId: Long): ServiceResult<Unit>

    suspend fun setCompleted(todoId: Long, completed: Boolean): ServiceResult<Todo>

    suspend fun getById(todoId: Long): ServiceResult<Todo>

    fun observeByDate(date: LocalDate): Flow<ServiceResult<List<Todo>>>

    fun observeFiltered(
        date: LocalDate,
        categoryId: Long?,
        incompleteOnly: Boolean,
    ): Flow<ServiceResult<List<Todo>>>
}

data class TodoSaveResult(
    val todo: Todo,
    val alarmSyncStatus: AlarmSyncStatus,
)

enum class AlarmSyncStatus {
    SYNCHRONIZED,
    INEXACT_SCHEDULED,
    NOTIFICATION_PERMISSION_DENIED,
    FAILED,
}
