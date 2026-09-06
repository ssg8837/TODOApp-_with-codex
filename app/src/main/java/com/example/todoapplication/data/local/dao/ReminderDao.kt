package com.example.todoapplication.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import com.example.todoapplication.data.local.entity.ReminderEntity
import kotlinx.coroutines.flow.Flow

@Dao
abstract class ReminderDao {
    @Insert
    abstract suspend fun insert(reminder: ReminderEntity): Long

    @Insert
    protected abstract suspend fun insertAll(reminders: List<ReminderEntity>): List<Long>

    @Query("SELECT * FROM reminders WHERE todo_id = :todoId ORDER BY minutes_before ASC, id ASC")
    abstract fun observeByTodoId(todoId: Long): Flow<List<ReminderEntity>>

    @Query("DELETE FROM reminders WHERE id = :id")
    abstract suspend fun deleteById(id: Long): Int

    @Query("DELETE FROM reminders WHERE todo_id = :todoId")
    abstract suspend fun deleteByTodoId(todoId: Long): Int

    @Transaction
    open suspend fun replaceByTodoId(
        todoId: Long,
        reminders: List<ReminderEntity>,
    ): List<Long> {
        deleteByTodoId(todoId)
        return insertAll(reminders)
    }

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
    abstract suspend fun getFutureAlarmRecoveryCandidates(
        currentDateEpochDay: Long,
        currentMinuteOfDay: Int,
    ): List<ReminderEntity>
}
