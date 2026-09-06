package com.example.todoapplication.application.service

import com.example.todoapplication.domain.model.Category
import com.example.todoapplication.domain.model.CategoryColor
import com.example.todoapplication.domain.model.Todo
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

internal val TEST_DATE: LocalDate = LocalDate.of(2026, 9, 6)
internal val TEST_INSTANT: Instant = Instant.parse("2026-09-06T00:00:00Z")

internal fun systemCategory(): Category = Category(
    id = 1,
    name = Category.SYSTEM_DEFAULT_NAME,
    color = CategoryColor.NEUTRAL,
    sortOrder = 0,
    isSystem = true,
    createdAt = TEST_INSTANT,
)

internal fun userCategory(
    id: Long = 2,
    name: String = "업무",
    sortOrder: Int = 1,
): Category = Category(
    id = id,
    name = name,
    color = CategoryColor.BLUE,
    sortOrder = sortOrder,
    isSystem = false,
    createdAt = TEST_INSTANT,
)

internal fun todo(
    id: Long = 1,
    title: String = "할 일",
    categoryId: Long = 1,
    time: LocalTime? = LocalTime.of(10, 0),
    isCompleted: Boolean = false,
): Todo = Todo(
    id = id,
    title = title,
    date = TEST_DATE,
    time = time,
    categoryId = categoryId,
    isCompleted = isCompleted,
    createdAt = TEST_INSTANT,
    updatedAt = TEST_INSTANT,
)
