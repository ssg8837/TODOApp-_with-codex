package com.example.todoapplication.feature.todo.list

import java.time.LocalDate

/** TODO 목록 화면에서 Presenter가 처리하는 사용자 의도. */
sealed interface TodoListEvent {
    data class SelectCategory(val categoryId: Long?) : TodoListEvent

    data class SetIncompleteOnly(val enabled: Boolean) : TodoListEvent
    data object PreviousDate : TodoListEvent

    data object NextDate : TodoListEvent

    data class SelectDate(val date: LocalDate) : TodoListEvent

    data class SetCompleted(
        val todoId: Long,
        val completed: Boolean,
    ) : TodoListEvent
}
