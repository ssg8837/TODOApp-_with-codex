package com.example.todoapplication.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.example.todoapplication.data.local.entity.TodoEntity
import kotlinx.coroutines.flow.Flow

/**
 * TODO 테이블의 저장·조회 계약을 정의하는 Room DAO.
 *
 * 날짜별 Flow는 시간 있는 TODO 우선, 시간 및 생성 시각 오름차순으로 갱신을 방출한다.
 */
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

    /** 날짜와 선택 Category, 미완료 여부를 함께 적용한 최신 목록을 관찰한다. */
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
