package com.example.todoapplication.feature.todo.list

import java.time.LocalDate

data class TodoListUiState(
    val selectedDate: LocalDate,
    val todos: List<TodoListItemUiModel> = emptyList(),
    val isLoading: Boolean = true,
    val error: TodoListError? = null,
)

enum class TodoListError {
    TODO_NOT_FOUND,
    CATEGORY_NOT_FOUND,
    INVALID_TODO,
    PERSISTENCE_FAILURE,
    OPERATION_FAILED,
}
