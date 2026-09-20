package com.example.todoapplication.feature.todo.list

import java.time.LocalDate

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
