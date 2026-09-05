package com.example.todoapplication.data.mapper

import com.example.todoapplication.data.local.RoomConverters
import com.example.todoapplication.data.local.entity.TodoEntity
import com.example.todoapplication.domain.model.Todo

object TodoMapper {
    private val converters = RoomConverters()

    fun toDomain(entity: TodoEntity): Todo = Todo(
        id = entity.id,
        title = entity.title,
        date = converters.epochDayToLocalDate(entity.dateEpochDay),
        time = converters.minuteOfDayToLocalTime(entity.timeMinuteOfDay),
        categoryId = entity.categoryId,
        isCompleted = entity.isCompleted,
        createdAt = converters.epochMillisToInstant(entity.createdAtEpochMillis),
        updatedAt = converters.epochMillisToInstant(entity.updatedAtEpochMillis),
    )

    fun toEntity(domain: Todo): TodoEntity = TodoEntity(
        id = domain.id,
        title = domain.title,
        dateEpochDay = converters.localDateToEpochDay(domain.date),
        timeMinuteOfDay = converters.localTimeToMinuteOfDay(domain.time),
        categoryId = domain.categoryId,
        isCompleted = domain.isCompleted,
        createdAtEpochMillis = converters.instantToEpochMillis(domain.createdAt),
        updatedAtEpochMillis = converters.instantToEpochMillis(domain.updatedAt),
    )
}
