package com.example.todoapplication.feature.todo.list

import com.example.todoapplication.application.service.ServiceError
import com.example.todoapplication.application.service.ServiceResult
import com.example.todoapplication.domain.model.CategoryColor
import com.example.todoapplication.domain.model.Todo
import com.example.todoapplication.test.MainDispatcherRule
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TodoFilterPresenterTest {
    @get:Rule val main = MainDispatcherRule()
    private val today = LocalDate.of(2026, 9, 20)
    private val clock = Clock.fixed(today.atStartOfDay().toInstant(ZoneOffset.UTC), ZoneOffset.UTC)
    private val todos = FakeTodoService()
    private val general = FakeCategoryService.defaultCategory(id = 1, name = "일반")
    private val work = general.copy(id = 2, name = "업무", isSystem = false, sortOrder = 1)
    private val personal = work.copy(id = 3, name = "개인", sortOrder = 2)
    private val categories = FakeCategoryService(listOf(general, work, personal))

    @Test
    fun defaultsAndEveryFilterCombinationArePassedToServiceAndDatesKeepFilters() = runTest {
        val presenter = TodoListPresenter(todos, categories, clock)
        advanceUntilIdle()
        assertNull(presenter.state.value.selectedCategoryId)
        assertFalse(presenter.state.value.incompleteOnly)
        assertEquals(FakeTodoService.FilterQuery(today, null, false), todos.filteredCalls.last())
        presenter.onEvent(TodoListEvent.SelectCategory(2))
        advanceUntilIdle()
        assertEquals(FakeTodoService.FilterQuery(today, 2, false), todos.filteredCalls.last())
        presenter.onEvent(TodoListEvent.SetIncompleteOnly(true))
        advanceUntilIdle()
        assertEquals(FakeTodoService.FilterQuery(today, 2, true), todos.filteredCalls.last())
        presenter.onEvent(TodoListEvent.NextDate)
        advanceUntilIdle()
        assertEquals(FakeTodoService.FilterQuery(today.plusDays(1), 2, true), todos.filteredCalls.last())
        presenter.onEvent(TodoListEvent.PreviousDate)
        advanceUntilIdle()
        assertEquals(FakeTodoService.FilterQuery(today, 2, true), todos.filteredCalls.last())
        presenter.onEvent(TodoListEvent.SelectCategory(null))
        advanceUntilIdle()
        assertEquals(FakeTodoService.FilterQuery(today, null, true), todos.filteredCalls.last())
        presenter.onEvent(TodoListEvent.SelectDate(today.plusDays(10)))
        advanceUntilIdle()
        assertEquals(FakeTodoService.FilterQuery(today.plusDays(10), null, true), todos.filteredCalls.last())
        presenter.onEvent(TodoListEvent.SetIncompleteOnly(false))
        advanceUntilIdle()
        assertEquals(FakeTodoService.FilterQuery(today.plusDays(10), null, false), todos.filteredCalls.last())
        assertEquals(1, categories.observationCount)
    }

    @Test
    fun rapidFilterChangesCancelOldCollectorsAndRejectTheirLateResults() = runTest {
        val presenter = TodoListPresenter(todos, categories, clock)
        advanceUntilIdle()
        presenter.onEvent(TodoListEvent.SelectCategory(2))
        advanceUntilIdle()
        presenter.onEvent(TodoListEvent.SelectCategory(3))
        presenter.onEvent(TodoListEvent.SetIncompleteOnly(true))
        advanceUntilIdle()
        todos.filterFlow(today, 3, true).emit(ServiceResult.Success(listOf(todo(3))))
        todos.filterFlow(today, 2).emit(ServiceResult.Success(listOf(todo(2))))
        advanceUntilIdle()
        assertEquals(3L, presenter.state.value.selectedCategoryId)
        assertEquals(listOf(3L), presenter.state.value.todos.map { it.id })
        assertEquals(0, todos.filterFlow(today, 2).subscriptionCount.value)
        assertEquals(0, todos.dateFlow(today).subscriptionCount.value)
    }

    @Test
    fun categoryMetadataAndOrderUpdatesKeepSelectionWithoutRestartingTodoQuery() = runTest {
        val presenter = TodoListPresenter(todos, categories, clock)
        advanceUntilIdle()
        presenter.onEvent(TodoListEvent.SelectCategory(2))
        advanceUntilIdle()
        todos.filterFlow(today, 2).emit(ServiceResult.Success(listOf(todo(2))))
        advanceUntilIdle()
        val count = todos.filteredCalls.size
        categories.categories.emit(ServiceResult.Success(listOf(
            general, personal, work.copy(name = "회사", color = CategoryColor.BLUE),
        )))
        advanceUntilIdle()
        assertEquals(listOf(1L, 3L, 2L), presenter.state.value.categories.map { it.id })
        assertEquals(2L, presenter.state.value.selectedCategoryId)
        assertEquals("회사", presenter.state.value.todos.single().categoryName)
        assertEquals(CategoryColor.BLUE, presenter.state.value.todos.single().categoryColor)
        assertEquals(count, todos.filteredCalls.size)
    }

    @Test
    fun deletingSelectedCategoryResetsToAllWhileKeepingIncompleteCondition() = runTest {
        val presenter = TodoListPresenter(todos, categories, clock)
        advanceUntilIdle()
        presenter.onEvent(TodoListEvent.SelectCategory(2))
        presenter.onEvent(TodoListEvent.SetIncompleteOnly(true))
        advanceUntilIdle()
        categories.categories.emit(ServiceResult.Success(listOf(general, personal)))
        advanceUntilIdle()
        assertNull(presenter.state.value.selectedCategoryId)
        assertEquals(listOf(1L, 3L), presenter.state.value.categories.map { it.id })
        assertTrue(presenter.state.value.isLoading)
        assertEquals(FakeTodoService.FilterQuery(today, null, true), todos.filteredCalls.last())
        todos.filterFlow(today, null, true).emit(ServiceResult.Success(listOf(todo(2).copy(categoryId = 1))))
        todos.filterFlow(today, 2, true).emit(ServiceResult.Success(listOf(todo(2))))
        advanceUntilIdle()
        assertEquals("일반", presenter.state.value.todos.single().categoryName)
        assertNull(presenter.state.value.error)
    }

    @Test
    fun completingTodoWaitsForFilteredFlowAndAllFilterKeepsCompletedTodo() = runTest {
        val presenter = TodoListPresenter(todos, categories, clock)
        val item = todo(2)
        advanceUntilIdle()
        presenter.onEvent(TodoListEvent.SetIncompleteOnly(true))
        advanceUntilIdle()
        todos.filterFlow(today, null, true).emit(ServiceResult.Success(listOf(item)))
        advanceUntilIdle()
        todos.completionResult = ServiceResult.Success(item.copy(isCompleted = true))
        presenter.onEvent(TodoListEvent.SetCompleted(item.id, true))
        advanceUntilIdle()
        assertEquals(1, presenter.state.value.todos.size)
        todos.dateFlow(today).emit(ServiceResult.Success(listOf(item.copy(isCompleted = true))))
        todos.filterFlow(today, null, true).emit(ServiceResult.Success(emptyList()))
        advanceUntilIdle()
        assertTrue(presenter.state.value.todos.isEmpty())
        assertEquals(TodoListEmptyState.NO_MATCHES, presenter.state.value.emptyState)
        presenter.onEvent(TodoListEvent.SetIncompleteOnly(false))
        advanceUntilIdle()
        assertTrue(presenter.state.value.todos.single().isCompleted)
    }

    @Test
    fun extraDateObservationOnlyExistsForEmptyFilteredResultsAndIsReactive() = runTest {
        val presenter = TodoListPresenter(todos, categories, clock)
        advanceUntilIdle()
        todos.dateFlow(today).emit(ServiceResult.Success(emptyList()))
        advanceUntilIdle()
        assertEquals(TodoListEmptyState.NO_TODOS, presenter.state.value.emptyState)
        assertTrue(todos.dateOnlyCalls.isEmpty())
        presenter.onEvent(TodoListEvent.SelectCategory(2))
        advanceUntilIdle()
        todos.filterFlow(today, 2).emit(ServiceResult.Success(emptyList()))
        advanceUntilIdle()
        assertEquals(TodoListEmptyState.NO_TODOS, presenter.state.value.emptyState)
        todos.dateFlow(today).emit(ServiceResult.Success(listOf(todo(3))))
        todos.filterFlow(today, 2).emit(ServiceResult.Success(emptyList()))
        advanceUntilIdle()
        assertEquals(TodoListEmptyState.NO_MATCHES, presenter.state.value.emptyState)
        assertEquals(listOf(today), todos.dateOnlyCalls)
        assertNull(presenter.state.value.error)
        todos.filterFlow(today, 2).emit(ServiceResult.Success(listOf(todo(2))))
        advanceUntilIdle()
        assertEquals(0, todos.dateFlow(today).subscriptionCount.value)
    }

    @Test
    fun filteredAndEmptyCheckFailuresMapToPresentationError() = runTest {
        val presenter = TodoListPresenter(todos, categories, clock)
        advanceUntilIdle()
        presenter.onEvent(TodoListEvent.SelectCategory(2))
        advanceUntilIdle()
        todos.filterFlow(today, 2).emit(ServiceResult.Failure(ServiceError.PersistenceFailure))
        advanceUntilIdle()
        assertEquals(TodoListError.PERSISTENCE_FAILURE, presenter.state.value.error)
        assertFalse(presenter.state.value.isLoading)
        todos.filterFlow(today, 2).emit(ServiceResult.Success(emptyList()))
        todos.dateFlow(today).emit(ServiceResult.Failure(ServiceError.PersistenceFailure))
        advanceUntilIdle()
        assertEquals(TodoListError.PERSISTENCE_FAILURE, presenter.state.value.error)
    }

    private fun todo(categoryId: Long) = Todo(
        categoryId, "TODO", today, null, categoryId, false, Instant.EPOCH, Instant.EPOCH,
    )
}
