package com.example.todoapplication.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.example.todoapplication.data.local.entity.TodoEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TodoDao {
    @Insert
    suspend fun insert(todo: TodoEntity): Long

    @Update
    suspend fun update(todo: TodoEntity): Int

    @Delete
    suspend fun delete(todo: TodoEntity): Int

    @Query("SELECT * FROM todos WHERE id = :id")
    suspend fun getById(id: Long): TodoEntity?

    @Query(
        """
        SELECT * FROM todos
        WHERE date_epoch_day = :dateEpochDay
        ORDER BY
            time_minute_of_day IS NULL ASC,
            time_minute_of_day ASC,
            created_at_epoch_millis ASC,
            id ASC
        """,
    )
    fun observeByDate(dateEpochDay: Long): Flow<List<TodoEntity>>

    @Query(
        """
        SELECT * FROM todos
        WHERE date_epoch_day = :dateEpochDay
          AND (:categoryId IS NULL OR category_id = :categoryId)
          AND (:incompleteOnly = 0 OR is_completed = 0)
        ORDER BY
            time_minute_of_day IS NULL ASC,
            time_minute_of_day ASC,
            created_at_epoch_millis ASC,
            id ASC
        """,
    )
    fun observeFiltered(
        dateEpochDay: Long,
        categoryId: Long?,
        incompleteOnly: Boolean,
    ): Flow<List<TodoEntity>>
}
