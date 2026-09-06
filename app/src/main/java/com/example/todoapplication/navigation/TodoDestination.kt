package com.example.todoapplication.navigation

import java.time.LocalDate

object TodoDestination {
    const val LIST = "todo/list"
    const val NEW_DATE_ARGUMENT = "date"
    const val TODO_ID_ARGUMENT = "todoId"
    const val NEW_PATTERN = "todo/new/{$NEW_DATE_ARGUMENT}"
    const val EDIT_PATTERN = "todo/edit/{$TODO_ID_ARGUMENT}"

    fun newTodo(date: LocalDate): String = "todo/new/$date"

    fun editTodo(todoId: Long): String = "todo/edit/$todoId"
}
