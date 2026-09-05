package com.example.todoapplication.domain.repository

import com.example.todoapplication.domain.model.Todo
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

interface TodoRepository {
    suspend fun create(todo: Todo): Todo

    suspend fun update(todo: Todo): Boolean

    suspend fun delete(todo: Todo): Boolean

    suspend fun getById(id: Long): Todo?

    fun observeByDate(date: LocalDate): Flow<List<Todo>>

    fun observeFiltered(
        date: LocalDate,
        categoryId: Long?,
        incompleteOnly: Boolean,
    ): Flow<List<Todo>>
}
