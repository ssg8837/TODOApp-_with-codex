package com.example.todoapplication.data.mapper

import com.example.todoapplication.data.local.entity.ReminderEntity
import com.example.todoapplication.domain.model.Reminder
import org.junit.Assert.assertEquals
import org.junit.Test

class ReminderMapperTest {
    @Test
    fun entityConvertsToDomain() {
        val entity = ReminderEntity(
            id = 21,
            todoId = 11,
            minutesBefore = Reminder.FIFTEEN_MINUTES_BEFORE,
        )

        assertEquals(
            Reminder(id = 21, todoId = 11, minutesBefore = 15),
            ReminderMapper.toDomain(entity),
        )
    }

    @Test
    fun domainConvertsToEntity() {
        val domain = Reminder(
            id = 22,
            todoId = 12,
            minutesBefore = Reminder.ONE_DAY_BEFORE,
        )

        assertEquals(
            ReminderEntity(id = 22, todoId = 12, minutesBefore = 1_440),
            ReminderMapper.toEntity(domain),
        )
    }
}
