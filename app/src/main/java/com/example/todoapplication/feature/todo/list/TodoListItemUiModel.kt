package com.example.todoapplication.feature.todo.list

import com.example.todoapplication.domain.model.CategoryColor
import java.time.LocalTime

/** TODO 행에 필요한 정보와 연결된 Category 표시 정보를 합친 presentation 모델. */
data class TodoListItemUiModel(
    val id: Long,
    val title: String,
    val time: LocalTime?,
    val isCompleted: Boolean,
    val categoryName: String,
    val categoryColor: CategoryColor,
)
