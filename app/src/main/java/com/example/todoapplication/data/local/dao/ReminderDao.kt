package com.example.todoapplication.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import com.example.todoapplication.data.local.entity.ReminderEntity
import kotlinx.coroutines.flow.Flow

/** TODO별 Reminder 영속 상태와 재부팅 복구 후보 조회를 담당하는 Room DAO. */
@Dao
abstract class ReminderDao {
    @Insert
    abstract suspend fun insert(reminder: ReminderEntity): Long

    @Insert
    protected abstract suspend fun insertAll(reminders: List<ReminderEntity>): List<Long>

    @Query("SELECT * FROM reminders WHERE todo_id = :todoId ORDER BY minutes_before ASC, id ASC")
    abstract fun observeByTodoId(todoId: Long): Flow<List<ReminderEntity>>

    @Query("SELECT * FROM reminders WHERE id = :id")
    abstract suspend fun getById(id: Long): ReminderEntity?

    @Query("DELETE FROM reminders WHERE id = :id")
    abstract suspend fun deleteById(id: Long): Int

    @Query("DELETE FROM reminders WHERE todo_id = :todoId")
    abstract suspend fun deleteByTodoId(todoId: Long): Int

    /**
     * [todoId]의 기존 Reminder를 모두 삭제하고 [reminders]를 일괄 삽입한다.
     *
     * 삭제와 삽입은 하나의 transaction으로 실행된다. 삽입 하나라도 실패하면 삭제를 포함한
     * 변경 전체가 rollback되며 빈 목록은 기존 Reminder 삭제만 commit한다.
     */
    @Transaction
    open suspend fun replaceByTodoId(
        todoId: Long,
        reminders: List<ReminderEntity>,
    ): List<Long> {
        deleteByTodoId(todoId)
        return insertAll(reminders)
    }

    /**
     * 미완료이며 시간이 있는 TODO 중 DB 계산 기준으로 현재보다 미래인 복구 후보를 조회한다.
     * Application Service는 예약 직전에 최신 상태와 정확한 발화 시각을 다시 검증한다.
     */
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
