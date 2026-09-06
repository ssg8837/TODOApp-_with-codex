package com.example.todoapplication.feature.todo.list

import com.example.todoapplication.application.service.ServiceError
import com.example.todoapplication.application.service.ServiceResult
import com.example.todoapplication.application.service.TodoService
import com.example.todoapplication.domain.model.Todo
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow

internal class FakeTodoService : TodoService {
    val observedDates = mutableListOf<LocalDate>()
    val completionCalls = mutableListOf<Pair<Long, Boolean>>()
    var completionResult: ServiceResult<Todo> =
        ServiceResult.Failure(ServiceError.TodoNotFound)
    private val dateFlows = mutableMapOf<LocalDate, MutableSharedFlow<ServiceResult<List<Todo>>>>()

    fun dateFlow(date: LocalDate): MutableSharedFlow<ServiceResult<List<Todo>>> =
        dateFlows.getOrPut(date) { MutableSharedFlow(replay = 1) }

    override fun observeByDate(date: LocalDate): Flow<ServiceResult<List<Todo>>> {
        observedDates += date
        return dateFlow(date)
    }

    override suspend fun setCompleted(
        todoId: Long,
        completed: Boolean,
    ): ServiceResult<Todo> {
        completionCalls += todoId to completed
        return completionResult
    }

    override suspend fun create(todo: Todo): ServiceResult<Todo> = unused()

    override suspend fun update(todo: Todo): ServiceResult<Todo> = unused()

    override suspend fun delete(todoId: Long): ServiceResult<Unit> = unused()

    override suspend fun getById(todoId: Long): ServiceResult<Todo> = unused()

    override fun observeFiltered(
        date: LocalDate,
        categoryId: Long?,
        incompleteOnly: Boolean,
    ): Flow<ServiceResult<List<Todo>>> = error("Not used by TodoListPresenter in Phase 5")

    private fun <T> unused(): ServiceResult<T> = error("Not used by TodoListPresenter in Phase 5")
}
