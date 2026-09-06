package com.example.todoapplication.feature.todo.edit

import com.example.todoapplication.domain.model.CategoryColor
import java.time.LocalDate
import java.time.LocalTime

data class TodoEditUiState(
    val mode: TodoEditMode,
    val todoId: Long? = null,
    val title: String = "",
    val date: LocalDate,
    val time: LocalTime? = null,
    val selectedCategoryId: Long? = null,
    val categories: List<TodoEditCategoryUiModel> = emptyList(),
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val validationErrors: Set<TodoEditValidationError> = emptySet(),
    val error: TodoEditError? = null,
)

enum class TodoEditMode {
    CREATE,
    EDIT,
}

data class TodoEditCategoryUiModel(
    val id: Long,
    val name: String,
    val color: CategoryColor,
)

enum class TodoEditValidationError {
    TITLE_REQUIRED,
    CATEGORY_REQUIRED,
}

enum class TodoEditError {
    TODO_NOT_FOUND,
    CATEGORY_NOT_FOUND,
    INVALID_TODO,
    PERSISTENCE_FAILURE,
    OPERATION_FAILED,
}
