package com.example.todoapplication.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * TODO에 연결된 Reminder를 저장하는 Room Entity.
 *
 * TODO 삭제 시 FK cascade로 함께 삭제되며, 알림 간격은 TODO 시각 이전의 분 단위 값이다.
 */
@Entity(
    tableName = "reminders",
    foreignKeys = [
        ForeignKey(
            entity = TodoEntity::class,
            parentColumns = ["id"],
            childColumns = ["todo_id"],
            onDelete = ForeignKey.CASCADE,
            onUpdate = ForeignKey.NO_ACTION,
        ),
    ],
    indices = [Index(value = ["todo_id"])],
)
data class ReminderEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "todo_id")
    val todoId: Long,
    @ColumnInfo(name = "minutes_before")
    val minutesBefore: Int,
)
