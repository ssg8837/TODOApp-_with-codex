package com.example.todoapplication.feature.todo.list

import java.time.LocalDate
import com.example.todoapplication.domain.model.CategoryColor

/**
 * TODO 목록을 렌더링하는 지속 상태.
 *
 * [selectedCategoryId]가 `null`이면 전체 Category이며 빈 목록은 [emptyState]로 원인을 구분한다.
 */
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

/** Category 필터에 필요한 이름과 의미 색상만 담는 표시 모델. */
data class CategoryFilterUiModel(val id: Long, val name: String, val color: CategoryColor)

/** 날짜 자체가 비었는지, 적용한 필터 결과만 비었는지를 구분한다. */
enum class TodoListEmptyState { NO_TODOS, NO_MATCHES }

/** Service 오류를 UI 문자열 리소스와 분리해 표현하는 목록 오류. */
enum class TodoListError {
    TODO_NOT_FOUND,
    CATEGORY_NOT_FOUND,
    INVALID_TODO,
    PERSISTENCE_FAILURE,
    OPERATION_FAILED,
}
