package com.example.todoapplication.application.service

import com.example.todoapplication.domain.repository.DataAccessException
import com.example.todoapplication.domain.validation.TodoValidationError
import com.example.todoapplication.test.FakeCategoryRepository
import com.example.todoapplication.test.FakeTodoRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class DefaultTodoServiceTest {
    @Test
    fun createValidTodoChecksCategoryAndReturnsSavedTodo() = runBlocking {
        val todoRepository = FakeTodoRepository()
        val categoryRepository = FakeCategoryRepository(listOf(systemCategory()))
        val service = DefaultTodoService(todoRepository, categoryRepository)

        val result = service.create(todo(id = 0))

        val saved = (result as ServiceResult.Success).value
        assertEquals(1L, saved.id)
        assertEquals(listOf("getById"), categoryRepository.calls)
        assertEquals(1, todoRepository.writeCount)
    }

    @Test
    fun createInvalidTodoDoesNotWriteRepository() = runBlocking {
        val todoRepository = FakeTodoRepository()
        val service = DefaultTodoService(
            todoRepository,
            FakeCategoryRepository(listOf(systemCategory())),
        )

        val result = service.create(todo(id = 0, title = "   "))

        assertEquals(
            ServiceResult.Failure(
                ServiceError.InvalidTodo(setOf(TodoValidationError.BLANK_TITLE)),
            ),
            result,
        )
        assertEquals(0, todoRepository.writeCount)
    }

    @Test
    fun updateExistingTodoReturnsUpdatedDomainState() = runBlocking {
        val existing = todo()
        val repository = FakeTodoRepository(listOf(existing))
        val service = DefaultTodoService(
            repository,
            FakeCategoryRepository(listOf(systemCategory())),
        )
        val updated = existing.copy(title = "수정")

        assertEquals(ServiceResult.Success(updated), service.update(updated))
        assertEquals(1, repository.writeCount)
    }

    @Test
    fun updateMissingTodoReturnsNotFoundWithoutWrite() = runBlocking {
        val repository = FakeTodoRepository()
        val service = DefaultTodoService(
            repository,
            FakeCategoryRepository(listOf(systemCategory())),
        )

        assertEquals(
            ServiceResult.Failure(ServiceError.TodoNotFound),
            service.update(todo(id = 99)),
        )
        assertEquals(0, repository.writeCount)
    }

    @Test
    fun deleteAndSetCompletedExecuteTodoUseCases() = runBlocking {
        val first = todo(id = 1)
        val second = todo(id = 2)
        val repository = FakeTodoRepository(listOf(first, second))
        val service = DefaultTodoService(
            repository,
            FakeCategoryRepository(listOf(systemCategory())),
        )

        assertEquals(
            ServiceResult.Success(second.copy(isCompleted = true)),
            service.setCompleted(second.id, true),
        )
        assertEquals(ServiceResult.Success(Unit), service.delete(first.id))
        assertEquals(listOf("getById", "update", "getById", "delete"), repository.calls)
    }

    @Test
    fun dateAndFilterObservationAreExposedAsServiceResults() = runBlocking {
        val included = todo(id = 1)
        val otherDate = todo(id = 2).copy(date = TEST_DATE.plusDays(1))
        val repository = FakeTodoRepository(listOf(included, otherDate))
        val service = DefaultTodoService(
            repository,
            FakeCategoryRepository(listOf(systemCategory())),
        )

        assertEquals(
            ServiceResult.Success(listOf(included)),
            service.observeByDate(TEST_DATE).first(),
        )
        assertEquals(
            ServiceResult.Success(listOf(included)),
            service.observeFiltered(TEST_DATE, included.categoryId, false).first(),
        )
    }

    @Test
    fun repositoryErrorBecomesPersistenceFailure() = runBlocking {
        val repository = FakeTodoRepository()
        repository.failWithPersistenceError = true
        val service = DefaultTodoService(
            repository,
            FakeCategoryRepository(listOf(systemCategory())),
        )

        assertEquals(
            ServiceResult.Failure(ServiceError.PersistenceFailure),
            service.create(todo(id = 0)),
        )
        assertEquals(DataAccessException::class, repositoryErrorType())
    }

    private fun repositoryErrorType() = DataAccessException::class
}
