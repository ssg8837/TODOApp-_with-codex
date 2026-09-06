package com.example.todoapplication.application.service

import com.example.todoapplication.domain.model.Todo
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

interface TodoService {
    suspend fun create(todo: Todo): ServiceResult<Todo>

    suspend fun update(todo: Todo): ServiceResult<Todo>

    suspend fun delete(todoId: Long): ServiceResult<Unit>

    suspend fun setCompleted(todoId: Long, completed: Boolean): ServiceResult<Todo>

    suspend fun getById(todoId: Long): ServiceResult<Todo>

    fun observeByDate(date: LocalDate): Flow<ServiceResult<List<Todo>>>

    fun observeFiltered(
        date: LocalDate,
        categoryId: Long?,
        incompleteOnly: Boolean,
    ): Flow<ServiceResult<List<Todo>>>
}
