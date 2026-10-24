package com.example.todoapplication.feature.todo.detail

import com.example.todoapplication.domain.model.CategoryColor
import java.time.LocalDate
import java.time.LocalTime

/** TODO 확인 화면의 불변 UI 상태. */
data class TodoDetailUiState(
    val todoId: Long? = null,
    val title: String = "",
    val date: LocalDate? = null,
    val time: LocalTime? = null,
    val categoryName: String = "",
    val categoryColor: CategoryColor = CategoryColor.NEUTRAL,
    val isCompleted: Boolean = false,
    val reminders: List<Int> = emptyList(),
    val isLoading: Boolean = true,
    val error: TodoDetailError? = null,
)

enum class TodoDetailError {
    TODO_NOT_FOUND,
    PERSISTENCE_FAILURE,
}

