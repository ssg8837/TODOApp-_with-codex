package com.example.todoapplication.feature.todo.list

import java.time.LocalDate

sealed interface TodoListEvent {
    data object PreviousDate : TodoListEvent

    data object NextDate : TodoListEvent

    data class SelectDate(val date: LocalDate) : TodoListEvent

    data class SetCompleted(
        val todoId: Long,
        val completed: Boolean,
    ) : TodoListEvent
}
