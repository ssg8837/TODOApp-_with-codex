package com.example.todoapplication.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.todoapplication.data.local.entity.ReminderEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ReminderDao {
    @Insert
    suspend fun insert(reminder: ReminderEntity): Long

    @Query("SELECT * FROM reminders WHERE todo_id = :todoId ORDER BY minutes_before ASC, id ASC")
    fun observeByTodoId(todoId: Long): Flow<List<ReminderEntity>>

    @Query("DELETE FROM reminders WHERE id = :id")
    suspend fun deleteById(id: Long): Int

    @Query("DELETE FROM reminders WHERE todo_id = :todoId")
    suspend fun deleteByTodoId(todoId: Long): Int

    @Query(
        """
        SELECT reminders.* FROM reminders
        INNER JOIN todos ON todos.id = reminders.todo_id
        WHERE todos.is_completed = 0
          AND todos.time_minute_of_day IS NOT NULL
          AND (
              todos.date_epoch_day * 1440
              + todos.time_minute_of_day
              - reminders.minutes_before
          ) > (:currentDateEpochDay * 1440 + :currentMinuteOfDay)
        ORDER BY todos.date_epoch_day ASC, todos.time_minute_of_day ASC, reminders.id ASC
        """,
    )
    suspend fun getFutureAlarmRecoveryCandidates(
        currentDateEpochDay: Long,
        currentMinuteOfDay: Int,
    ): List<ReminderEntity>
}
