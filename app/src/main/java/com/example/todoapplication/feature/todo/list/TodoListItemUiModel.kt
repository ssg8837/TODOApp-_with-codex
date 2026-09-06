package com.example.todoapplication.feature.todo.list

import com.example.todoapplication.domain.model.CategoryColor
import java.time.LocalTime

data class TodoListItemUiModel(
    val id: Long,
    val title: String,
    val time: LocalTime?,
    val isCompleted: Boolean,
    val categoryName: String,
    val categoryColor: CategoryColor,
)
