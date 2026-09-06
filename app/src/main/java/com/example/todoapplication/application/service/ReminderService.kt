package com.example.todoapplication.application.service

import com.example.todoapplication.domain.model.Reminder
import java.time.LocalDate
import java.time.LocalTime
import kotlinx.coroutines.flow.Flow

interface ReminderService {
    suspend fun create(reminder: Reminder): ServiceResult<Reminder>

    fun observeByTodoId(todoId: Long): Flow<ServiceResult<List<Reminder>>>

    suspend fun replaceReminders(
        todoId: Long,
        reminders: List<Reminder>,
    ): ServiceResult<List<Reminder>>

    suspend fun delete(reminderId: Long): ServiceResult<Unit>

    suspend fun deleteByTodoId(todoId: Long): ServiceResult<Int>

    suspend fun getFutureAlarmRecoveryCandidates(
        currentDate: LocalDate,
        currentTime: LocalTime,
    ): ServiceResult<List<Reminder>>
}
