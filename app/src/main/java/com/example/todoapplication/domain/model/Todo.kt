package com.example.todoapplication.domain.model

import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

data class Todo(
    val id: Long,
    val title: String,
    val date: LocalDate,
    val time: LocalTime?,
    val categoryId: Long,
    val isCompleted: Boolean,
    val createdAt: Instant,
    val updatedAt: Instant,
)
