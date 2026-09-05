package com.example.todoapplication.data.mapper

import com.example.todoapplication.data.local.RoomConverters
import com.example.todoapplication.data.local.entity.CategoryEntity
import com.example.todoapplication.domain.model.Category

object CategoryMapper {
    private val converters = RoomConverters()

    fun toDomain(entity: CategoryEntity): Category = Category(
        id = entity.id,
        name = entity.name,
        color = converters.storageValueToCategoryColor(entity.colorValue),
        sortOrder = entity.sortOrder,
        isSystem = entity.isSystem,
        createdAt = converters.epochMillisToInstant(entity.createdAtEpochMillis),
    )

    fun toEntity(domain: Category): CategoryEntity = CategoryEntity(
        id = domain.id,
        name = domain.name,
        colorValue = converters.categoryColorToStorageValue(domain.color),
        sortOrder = domain.sortOrder,
        isSystem = domain.isSystem,
        createdAtEpochMillis = converters.instantToEpochMillis(domain.createdAt),
    )
}
