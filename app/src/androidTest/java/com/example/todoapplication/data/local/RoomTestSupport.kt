package com.example.todoapplication.data.local

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.todoapplication.data.local.entity.CategoryEntity
import com.example.todoapplication.data.local.entity.TodoEntity
import com.example.todoapplication.domain.model.CategoryColor

internal const val TEST_DATE_EPOCH_DAY = 20_000L

internal fun createTestDatabase(name: String): Pair<Context, TodoDatabase> {
    val context = ApplicationProvider.getApplicationContext<Context>()
    context.deleteDatabase(name)
    val database = TodoDatabase.create(context, name)
    database.openHelper.writableDatabase
    return context to database
}

internal fun userCategory(
    name: String,
    sortOrder: Int,
    color: CategoryColor = CategoryColor.BLUE,
): CategoryEntity = CategoryEntity(
    name = name,
    colorValue = color.storageValue,
    sortOrder = sortOrder,
    isSystem = false,
    createdAtEpochMillis = sortOrder.toLong(),
)

internal fun todo(
    categoryId: Long,
    title: String,
    timeMinuteOfDay: Int? = null,
    createdAtEpochMillis: Long = 1L,
    isCompleted: Boolean = false,
): TodoEntity = TodoEntity(
    title = title,
    dateEpochDay = TEST_DATE_EPOCH_DAY,
    timeMinuteOfDay = timeMinuteOfDay,
    categoryId = categoryId,
    isCompleted = isCompleted,
    createdAtEpochMillis = createdAtEpochMillis,
    updatedAtEpochMillis = createdAtEpochMillis,
)
