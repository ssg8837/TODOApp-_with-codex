package com.example.todoapplication.feature.category

import com.example.todoapplication.domain.model.CategoryColor

data class CategoryManagementUiState(
    val categories: List<CategoryManagementItemUiModel> = emptyList(),
    val isLoading: Boolean = true,
    val error: CategoryManagementError? = null,
    val editorMode: CategoryEditorMode = CategoryEditorMode.NONE,
    val editingCategoryId: Long? = null,
    val categoryNameInput: String = "",
    val selectedCategoryColor: CategoryColor = CategoryColor.NEUTRAL,
    val isReordering: Boolean = false,
    val showDeleteConfirmation: Boolean = false,
    val deleteTargetCategoryId: Long? = null,
    val isSaving: Boolean = false,
    val isDeleting: Boolean = false,
    val validationError: CategoryNameValidationError? = null,
)

data class CategoryManagementItemUiModel(
    val id: Long,
    val name: String,
    val color: CategoryColor,
    val isSystem: Boolean,
)

enum class CategoryEditorMode { NONE, CREATE, EDIT }

enum class CategoryNameValidationError { REQUIRED, RESERVED, DUPLICATED, INVALID }

enum class CategoryManagementError {
    INVALID_ORDER,
    CATEGORY_NOT_FOUND,
    SYSTEM_OPERATION_PROHIBITED,
    PERSISTENCE_FAILURE,
    OPERATION_FAILED,
}
