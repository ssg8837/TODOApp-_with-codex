package com.example.todoapplication.feature.category

import com.example.todoapplication.domain.model.CategoryColor

/**
 * Category 목록과 editor/dialog 진행 상태를 함께 표현하는 화면 상태.
 *
 * [isReordering], [isSaving], [isDeleting]은 충돌하는 중복 작업을 차단한다.
 */
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

/** Category 행 렌더링과 시스템 항목 보호에 필요한 presentation 모델. */
data class CategoryManagementItemUiModel(
    val id: Long,
    val name: String,
    val color: CategoryColor,
    val isSystem: Boolean,
)

/** Category editor의 닫힘·생성·수정 모드. */
enum class CategoryEditorMode { NONE, CREATE, EDIT }

/** Category 이름 입력에 대응하는 validation 상태. */
enum class CategoryNameValidationError { REQUIRED, RESERVED, DUPLICATED, INVALID }

/** Category Service 결과를 화면 메시지에 연결하는 오류. */
enum class CategoryManagementError {
    INVALID_ORDER,
    CATEGORY_NOT_FOUND,
    SYSTEM_OPERATION_PROHIBITED,
    PERSISTENCE_FAILURE,
    OPERATION_FAILED,
}
