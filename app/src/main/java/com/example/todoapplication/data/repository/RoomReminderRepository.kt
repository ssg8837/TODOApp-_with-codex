package com.example.todoapplication.data.repository

import com.example.todoapplication.data.local.RoomConverters
import com.example.todoapplication.data.local.dao.ReminderDao
import com.example.todoapplication.data.mapper.ReminderMapper
import com.example.todoapplication.domain.model.Reminder
import com.example.todoapplication.domain.repository.ReminderRepository
import java.time.LocalDate
import java.time.LocalTime
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

internal class RoomReminderRepository(
    private val reminderDao: ReminderDao,
) : ReminderRepository {
    private val converters = RoomConverters()

    override suspend fun create(reminder: Reminder): Reminder = dataAccess {
        val id = reminderDao.insert(ReminderMapper.toEntity(reminder))
        reminder.copy(id = id)
    }

    override fun observeByTodoId(todoId: Long): Flow<List<Reminder>> =
        reminderDao.observeByTodoId(todoId)
            .map { entities -> entities.map(ReminderMapper::toDomain) }
            .mapDataAccessErrors()

    override suspend fun deleteById(id: Long): Boolean = dataAccess {
        reminderDao.deleteById(id) == 1
    }

    override suspend fun deleteByTodoId(todoId: Long): Int = dataAccess {
        reminderDao.deleteByTodoId(todoId)
    }

    override suspend fun replaceReminders(
        todoId: Long,
        reminders: List<Reminder>,
    ): List<Reminder> = dataAccess {
        val entities = reminders.map(ReminderMapper::toEntity)
        val ids = reminderDao.replaceByTodoId(todoId, entities)
        reminders.zip(ids) { reminder, id -> reminder.copy(id = id) }
    }

    override suspend fun getFutureAlarmRecoveryCandidates(
        currentDate: LocalDate,
        currentTime: LocalTime,
    ): List<Reminder> = dataAccess {
        reminderDao.getFutureAlarmRecoveryCandidates(
            currentDateEpochDay = converters.localDateToEpochDay(currentDate),
            currentMinuteOfDay = checkNotNull(converters.localTimeToMinuteOfDay(currentTime)),
        ).map(ReminderMapper::toDomain)
    }
}
