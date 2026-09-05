package com.example.todoapplication.data.mapper

import com.example.todoapplication.data.local.entity.TodoEntity
import com.example.todoapplication.domain.model.Todo
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TodoMapperTest {
    @Test
    fun entityConvertsToDomain() {
        val entity = TodoEntity(
            id = 11,
            title = "회의 준비",
            dateEpochDay = LocalDate.of(2026, 9, 5).toEpochDay(),
            timeMinuteOfDay = 9 * 60 + 30,
            categoryId = 3,
            isCompleted = true,
            createdAtEpochMillis = 1_700_000_000_000,
            updatedAtEpochMillis = 1_700_000_100_000,
        )

        val domain = TodoMapper.toDomain(entity)

        assertEquals(11, domain.id)
        assertEquals("회의 준비", domain.title)
        assertEquals(LocalDate.of(2026, 9, 5), domain.date)
        assertEquals(LocalTime.of(9, 30), domain.time)
        assertEquals(3, domain.categoryId)
        assertEquals(true, domain.isCompleted)
        assertEquals(Instant.ofEpochMilli(1_700_000_000_000), domain.createdAt)
        assertEquals(Instant.ofEpochMilli(1_700_000_100_000), domain.updatedAt)
    }

    @Test
    fun domainConvertsToEntity() {
        val domain = Todo(
            id = 12,
            title = "문서 작성",
            date = LocalDate.of(2026, 9, 6),
            time = LocalTime.of(17, 45),
            categoryId = 4,
            isCompleted = false,
            createdAt = Instant.ofEpochMilli(1_700_000_200_000),
            updatedAt = Instant.ofEpochMilli(1_700_000_300_000),
        )

        val entity = TodoMapper.toEntity(domain)

        assertEquals(12, entity.id)
        assertEquals("문서 작성", entity.title)
        assertEquals(domain.date.toEpochDay(), entity.dateEpochDay)
        assertEquals(17 * 60 + 45, entity.timeMinuteOfDay)
        assertEquals(4, entity.categoryId)
        assertEquals(false, entity.isCompleted)
        assertEquals(domain.createdAt.toEpochMilli(), entity.createdAtEpochMillis)
        assertEquals(domain.updatedAt.toEpochMilli(), entity.updatedAtEpochMillis)
    }

    @Test
    fun nullableTimeIsPreservedInBothDirections() {
        val domain = Todo(
            id = 13,
            title = "시간 없는 일정",
            date = LocalDate.of(2026, 9, 7),
            time = null,
            categoryId = 1,
            isCompleted = false,
            createdAt = Instant.EPOCH,
            updatedAt = Instant.EPOCH,
        )

        val entity = TodoMapper.toEntity(domain)
        val restored = TodoMapper.toDomain(entity)

        assertNull(entity.timeMinuteOfDay)
        assertNull(restored.time)
        assertEquals(domain, restored)
    }
}
