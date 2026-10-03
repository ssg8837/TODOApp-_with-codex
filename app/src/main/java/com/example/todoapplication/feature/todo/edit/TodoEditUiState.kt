package com.example.todoapplication.feature.todo.edit

import com.example.todoapplication.domain.model.CategoryColor
import java.time.LocalDate
import java.time.LocalTime

/**
 * TODO 편집 화면의 지속 상태.
 *
 * [todoId]는 신규 모드에서 `null`이고 [time]이 `null`이면 Reminder 선택이 비활성화된다.
 * [isSaving]과 [isDeleting]은 동일 작업의 중복 요청을 막는 진행 상태다.
 */
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
    val showDeleteConfirmation: Boolean = false,
    val isDeleting: Boolean = false,
    val validationErrors: Set<TodoEditValidationError> = emptySet(),
    val error: TodoEditError? = null,
    val remindOneDayBefore: Boolean = false,
    val remindFifteenMinutesBefore: Boolean = false,
    val notificationPermissionDenied: Boolean = false,
)

/** TODO 편집 화면의 신규 등록 또는 기존 항목 수정 모드. */
enum class TodoEditMode {
    CREATE,
    EDIT,
}

/** 편집 화면의 Category 선택 항목. */
data class TodoEditCategoryUiModel(
    val id: Long,
    val name: String,
    val color: CategoryColor,
)

/** 즉시 표시할 수 있는 편집 입력 검증 오류. */
enum class TodoEditValidationError {
    TITLE_REQUIRED,
    CATEGORY_REQUIRED,
}

/** Service 결과를 화면 메시지 리소스에 연결하기 위한 편집 오류. */
enum class TodoEditError {
    TODO_NOT_FOUND,
    CATEGORY_NOT_FOUND,
    INVALID_TODO,
    PERSISTENCE_FAILURE,
    OPERATION_FAILED,
    ALARM_NOT_SCHEDULED,
    NOTIFICATION_PERMISSION_DENIED,
    INEXACT_ALARM_SCHEDULED,
}
