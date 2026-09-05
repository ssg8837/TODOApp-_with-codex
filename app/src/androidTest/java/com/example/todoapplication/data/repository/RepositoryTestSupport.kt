package com.example.todoapplication.data.repository

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.todoapplication.data.local.TodoDatabase
import com.example.todoapplication.domain.model.Category
import com.example.todoapplication.domain.model.CategoryColor
import com.example.todoapplication.domain.model.Todo
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

internal val TEST_DATE: LocalDate = LocalDate.of(2026, 9, 5)

internal fun createRepositoryTestDatabase(name: String): Pair<Context, TodoDatabase> {
    val context = ApplicationProvider.getApplicationContext<Context>()
    context.deleteDatabase(name)
    val database = TodoDatabase.create(context, name)
    database.openHelper.writableDatabase
    return context to database
}

internal fun category(
    name: String,
    sortOrder: Int,
    color: CategoryColor = CategoryColor.BLUE,
): Category = Category(
    id = 0,
    name = name,
    color = color,
    sortOrder = sortOrder,
    isSystem = false,
    createdAt = Instant.ofEpochMilli(sortOrder.toLong()),
)

internal fun todo(
    categoryId: Long,
    title: String,
    time: LocalTime? = null,
    createdAt: Instant = Instant.parse("2026-09-01T00:00:00Z"),
): Todo = Todo(
    id = 0,
    title = title,
    date = TEST_DATE,
    time = time,
    categoryId = categoryId,
    isCompleted = false,
    createdAt = createdAt,
    updatedAt = createdAt,
)
