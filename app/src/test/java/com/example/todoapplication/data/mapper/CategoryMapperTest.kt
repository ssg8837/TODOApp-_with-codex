package com.example.todoapplication.data.mapper

import com.example.todoapplication.data.local.entity.CategoryEntity
import com.example.todoapplication.domain.model.Category
import com.example.todoapplication.domain.model.CategoryColor
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Test

class CategoryMapperTest {
    @Test
    fun entityConvertsToDomain() {
        val entity = CategoryEntity(
            id = 1,
            name = Category.SYSTEM_DEFAULT_NAME,
            colorValue = CategoryColor.NEUTRAL.storageValue,
            sortOrder = Category.SYSTEM_DEFAULT_SORT_ORDER,
            isSystem = true,
            createdAtEpochMillis = 1_700_000_000_000,
        )

        val domain = CategoryMapper.toDomain(entity)

        assertEquals(1, domain.id)
        assertEquals(Category.SYSTEM_DEFAULT_NAME, domain.name)
        assertEquals(CategoryColor.NEUTRAL, domain.color)
        assertEquals(0, domain.sortOrder)
        assertEquals(true, domain.isSystem)
        assertEquals(Instant.ofEpochMilli(1_700_000_000_000), domain.createdAt)
    }

    @Test
    fun domainConvertsToEntity() {
        val domain = Category(
            id = 4,
            name = "업무",
            color = CategoryColor.BLUE,
            sortOrder = 2,
            isSystem = false,
            createdAt = Instant.ofEpochMilli(1_700_000_100_000),
        )

        val entity = CategoryMapper.toEntity(domain)

        assertEquals(4, entity.id)
        assertEquals("업무", entity.name)
        assertEquals("BLUE", entity.colorValue)
        assertEquals(2, entity.sortOrder)
        assertEquals(false, entity.isSystem)
        assertEquals(domain.createdAt.toEpochMilli(), entity.createdAtEpochMillis)
        assertEquals(domain, CategoryMapper.toDomain(entity))
    }
}
