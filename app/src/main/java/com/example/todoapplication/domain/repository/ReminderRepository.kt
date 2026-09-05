package com.example.todoapplication.domain.repository

import com.example.todoapplication.domain.model.Reminder
import java.time.LocalDate
import java.time.LocalTime
import kotlinx.coroutines.flow.Flow

interface ReminderRepository {
    suspend fun create(reminder: Reminder): Reminder

    fun observeByTodoId(todoId: Long): Flow<List<Reminder>>

    suspend fun deleteById(id: Long): Boolean

    suspend fun deleteByTodoId(todoId: Long): Int

    suspend fun getFutureAlarmRecoveryCandidates(
        currentDate: LocalDate,
        currentTime: LocalTime,
    ): List<Reminder>
}
