package com.example.todoapplication.application.service

import com.example.todoapplication.domain.model.Todo
import com.example.todoapplication.domain.repository.CategoryRepository
import com.example.todoapplication.domain.repository.TodoRepository
import com.example.todoapplication.domain.validation.TodoValidator
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

class DefaultTodoService(
    private val todoRepository: TodoRepository,
    private val categoryRepository: CategoryRepository,
) : TodoService {
    override suspend fun create(todo: Todo): ServiceResult<Todo> = serviceCall {
        val errors = TodoValidator.validate(todo)
        if (errors.isNotEmpty()) {
            return@serviceCall ServiceResult.Failure(ServiceError.InvalidTodo(errors))
        }
        if (categoryRepository.getById(todo.categoryId) == null) {
            return@serviceCall ServiceResult.Failure(ServiceError.CategoryNotFound)
        }
        ServiceResult.Success(todoRepository.create(todo))
    }

    override suspend fun update(todo: Todo): ServiceResult<Todo> = serviceCall {
        val errors = TodoValidator.validate(todo)
        if (errors.isNotEmpty()) {
            return@serviceCall ServiceResult.Failure(ServiceError.InvalidTodo(errors))
        }
        if (todoRepository.getById(todo.id) == null) {
            return@serviceCall ServiceResult.Failure(ServiceError.TodoNotFound)
        }
        if (categoryRepository.getById(todo.categoryId) == null) {
            return@serviceCall ServiceResult.Failure(ServiceError.CategoryNotFound)
        }
        if (!todoRepository.update(todo)) {
            return@serviceCall ServiceResult.Failure(ServiceError.TodoNotFound)
        }
        ServiceResult.Success(todo)
    }

    override suspend fun delete(todoId: Long): ServiceResult<Unit> = serviceCall {
        val todo = todoRepository.getById(todoId)
            ?: return@serviceCall ServiceResult.Failure(ServiceError.TodoNotFound)
        if (!todoRepository.delete(todo)) {
            return@serviceCall ServiceResult.Failure(ServiceError.TodoNotFound)
        }
        ServiceResult.Success(Unit)
    }

    override suspend fun setCompleted(
        todoId: Long,
        completed: Boolean,
    ): ServiceResult<Todo> = serviceCall {
        val todo = todoRepository.getById(todoId)
            ?: return@serviceCall ServiceResult.Failure(ServiceError.TodoNotFound)
        val updated = todo.copy(isCompleted = completed)
        if (!todoRepository.update(updated)) {
            return@serviceCall ServiceResult.Failure(ServiceError.TodoNotFound)
        }
        ServiceResult.Success(updated)
    }

    override suspend fun getById(todoId: Long): ServiceResult<Todo> = serviceCall {
        val todo = todoRepository.getById(todoId)
            ?: return@serviceCall ServiceResult.Failure(ServiceError.TodoNotFound)
        ServiceResult.Success(todo)
    }

    override fun observeByDate(date: LocalDate): Flow<ServiceResult<List<Todo>>> =
        todoRepository.observeByDate(date).asServiceResult()

    override fun observeFiltered(
        date: LocalDate,
        categoryId: Long?,
        incompleteOnly: Boolean,
    ): Flow<ServiceResult<List<Todo>>> = todoRepository.observeFiltered(
        date = date,
        categoryId = categoryId,
        incompleteOnly = incompleteOnly,
    ).asServiceResult()
}
