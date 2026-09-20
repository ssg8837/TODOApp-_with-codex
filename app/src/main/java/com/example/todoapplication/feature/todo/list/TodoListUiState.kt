package com.example.todoapplication.feature.todo.list

import java.time.LocalDate
import com.example.todoapplication.domain.model.CategoryColor

data class TodoListUiState(
    val selectedDate: LocalDate,
    val todos: List<TodoListItemUiModel> = emptyList(),
    val isLoading: Boolean = true,
    val error: TodoListError? = null,
    val selectedCategoryId: Long? = null,
    val incompleteOnly: Boolean = false,
    val categories: List<CategoryFilterUiModel> = emptyList(),
    val emptyState: TodoListEmptyState = TodoListEmptyState.NO_TODOS,
)

data class CategoryFilterUiModel(val id: Long, val name: String, val color: CategoryColor)

enum class TodoListEmptyState { NO_TODOS, NO_MATCHES }

enum class TodoListError {
    TODO_NOT_FOUND,
    CATEGORY_NOT_FOUND,
    INVALID_TODO,
    PERSISTENCE_FAILURE,
    OPERATION_FAILED,
}
