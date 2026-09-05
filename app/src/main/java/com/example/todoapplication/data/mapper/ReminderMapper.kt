package com.example.todoapplication.data.mapper

import com.example.todoapplication.data.local.entity.ReminderEntity
import com.example.todoapplication.domain.model.Reminder

object ReminderMapper {
    fun toDomain(entity: ReminderEntity): Reminder = Reminder(
        id = entity.id,
        todoId = entity.todoId,
        minutesBefore = entity.minutesBefore,
    )

    fun toEntity(domain: Reminder): ReminderEntity = ReminderEntity(
        id = domain.id,
        todoId = domain.todoId,
        minutesBefore = domain.minutesBefore,
    )
}
