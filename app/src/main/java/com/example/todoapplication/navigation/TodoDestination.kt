package com.example.todoapplication.navigation

import java.time.LocalDate

/**
 * 앱의 route와 최소 navigation argument 계약.
 * 신규 화면에는 날짜, 수정 화면에는 TODO ID만 전달하며 Domain 객체는 전달하지 않는다.
 */
object TodoDestination {
    const val LIST = "todo/list"
    const val CATEGORY_MANAGEMENT = "category/manage"
    const val NEW_DATE_ARGUMENT = "date"
    const val TODO_ID_ARGUMENT = "todoId"
    const val NEW_PATTERN = "todo/new/{$NEW_DATE_ARGUMENT}"
    const val EDIT_PATTERN = "todo/edit/{$TODO_ID_ARGUMENT}"

    fun newTodo(date: LocalDate): String = "todo/new/$date"

    fun editTodo(todoId: Long): String = "todo/edit/$todoId"
}
