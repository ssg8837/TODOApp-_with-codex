package com.example.todoapplication.feature.todo.list

import com.example.todoapplication.application.service.ServiceError
import com.example.todoapplication.application.service.ServiceResult
import com.example.todoapplication.domain.model.CategoryColor
import com.example.todoapplication.domain.model.Todo
import com.example.todoapplication.test.MainDispatcherRule
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
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
class TodoListPresenterTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun initialDateUsesInjectedClockAndStartsObservingToday() = runTest {
        val service = FakeTodoService()
        val presenter = createPresenter(service)

        advanceUntilIdle()

        assertEquals(TODAY, presenter.state.value.selectedDate)
        assertTrue(presenter.state.value.isLoading)
        assertEquals(listOf(TODAY), service.observedDates)
    }

    @Test
    fun previousAndNextEventsChangeDateAndObservation() = runTest {
        val service = FakeTodoService()
        val presenter = createPresenter(service)
        advanceUntilIdle()

        presenter.onEvent(TodoListEvent.PreviousDate)
        advanceUntilIdle()
        assertEquals(TODAY.minusDays(1), presenter.state.value.selectedDate)
        assertEquals(TODAY.minusDays(1), service.observedDates.last())

        presenter.onEvent(TodoListEvent.NextDate)
        advanceUntilIdle()
        assertEquals(TODAY, presenter.state.value.selectedDate)
        assertEquals(TODAY, service.observedDates.last())
    }

    @Test
    fun selectDateObservesRequestedDate() = runTest {
        val service = FakeTodoService()
        val presenter = createPresenter(service)
        val selectedDate = TODAY.plusDays(10)
        advanceUntilIdle()

        presenter.onEvent(TodoListEvent.SelectDate(selectedDate))
        advanceUntilIdle()

        assertEquals(selectedDate, presenter.state.value.selectedDate)
        assertEquals(selectedDate, service.observedDates.last())
    }

    @Test
    fun latestDateFlowWinsAfterRapidDateChanges() = runTest {
        val service = FakeTodoService()
        val presenter = createPresenter(service)
        val firstDate = TODAY.plusDays(1)
        val latestDate = TODAY.plusDays(2)
        val staleTodo = todo(id = 1, date = firstDate, title = "이전")
        val latestTodo = todo(id = 2, date = latestDate, title = "최신")
        advanceUntilIdle()

        presenter.onEvent(TodoListEvent.SelectDate(firstDate))
        advanceUntilIdle()
        presenter.onEvent(TodoListEvent.SelectDate(latestDate))
        advanceUntilIdle()

        service.dateFlow(firstDate).emit(ServiceResult.Success(listOf(staleTodo)))
        service.dateFlow(latestDate).emit(ServiceResult.Success(listOf(latestTodo)))
        advanceUntilIdle()

        assertEquals(latestDate, presenter.state.value.selectedDate)
        assertEquals(listOf(item(latestTodo)), presenter.state.value.todos)
    }

    @Test
    fun receivedTodosAndEmptyListAreNormalLoadedStates() = runTest {
        val service = FakeTodoService()
        val presenter = createPresenter(service)
        val todoItem = todo()
        advanceUntilIdle()

        service.dateFlow(TODAY).emit(ServiceResult.Success(listOf(todoItem)))
        advanceUntilIdle()
        assertEquals(listOf(item(todoItem)), presenter.state.value.todos)
        assertFalse(presenter.state.value.isLoading)
        assertNull(presenter.state.value.error)

        service.dateFlow(TODAY).emit(ServiceResult.Success(emptyList()))
        advanceUntilIdle()
        assertEquals(emptyList<TodoListItemUiModel>(), presenter.state.value.todos)
        assertFalse(presenter.state.value.isLoading)
        assertNull(presenter.state.value.error)
    }

    @Test
    fun listFailureMapsServiceErrorToPresentationError() = runTest {
        val service = FakeTodoService()
        val presenter = createPresenter(service)
        advanceUntilIdle()

        service.dateFlow(TODAY).emit(
            ServiceResult.Failure(ServiceError.PersistenceFailure),
        )
        advanceUntilIdle()

        assertEquals(TodoListError.PERSISTENCE_FAILURE, presenter.state.value.error)
        assertFalse(presenter.state.value.isLoading)
        assertEquals(emptyList<TodoListItemUiModel>(), presenter.state.value.todos)
    }

    @Test
    fun setCompletedEventCallsServiceForCompletedAndIncompleteTransitions() = runTest {
        val service = FakeTodoService().apply {
            completionResult = ServiceResult.Success(todo(isCompleted = true))
        }
        val presenter = createPresenter(service)
        advanceUntilIdle()

        presenter.onEvent(TodoListEvent.SetCompleted(todoId = 1, completed = true))
        presenter.onEvent(TodoListEvent.SetCompleted(todoId = 1, completed = false))
        advanceUntilIdle()

        assertEquals(listOf(1L to true, 1L to false), service.completionCalls)
        assertNull(presenter.state.value.error)
    }

    @Test
    fun completionFailureMapsToPresentationError() = runTest {
        val service = FakeTodoService().apply {
            completionResult = ServiceResult.Failure(ServiceError.TodoNotFound)
        }
        val presenter = createPresenter(service)
        advanceUntilIdle()

        presenter.onEvent(TodoListEvent.SetCompleted(todoId = 99, completed = true))
        advanceUntilIdle()

        assertEquals(TodoListError.TODO_NOT_FOUND, presenter.state.value.error)
    }

    @Test
    fun todosAndCategoriesCombineIntoPresentationItems() = runTest {
        val todoService = FakeTodoService()
        val categoryService = FakeCategoryService(
            listOf(FakeCategoryService.defaultCategory(name = "업무", color = CategoryColor.BLUE)),
        )
        val presenter = createPresenter(todoService, categoryService)
        val todo = todo(title = "회의")
        advanceUntilIdle()

        todoService.dateFlow(TODAY).emit(ServiceResult.Success(listOf(todo)))
        advanceUntilIdle()

        assertEquals(
            listOf(
                TodoListItemUiModel(
                    id = todo.id,
                    title = "회의",
                    time = todo.time,
                    isCompleted = false,
                    categoryName = "업무",
                    categoryColor = CategoryColor.BLUE,
                ),
            ),
            presenter.state.value.todos,
        )
    }

    @Test
    fun categoryChangesReactivelyUpdateMultipleItemsUsingSameCategory() = runTest {
        val todoService = FakeTodoService()
        val categoryService = FakeCategoryService(
            listOf(FakeCategoryService.defaultCategory(name = "업무", color = CategoryColor.BLUE)),
        )
        val presenter = createPresenter(todoService, categoryService)
        val todos = listOf(todo(id = 1), todo(id = 2, title = "두 번째"))
        advanceUntilIdle()
        todoService.dateFlow(TODAY).emit(ServiceResult.Success(todos))
        advanceUntilIdle()

        categoryService.categories.emit(
            ServiceResult.Success(
                listOf(
                    FakeCategoryService.defaultCategory(
                        name = "변경된 이름",
                        color = CategoryColor.RED,
                    ),
                ),
            ),
        )
        advanceUntilIdle()

        assertEquals(listOf("변경된 이름", "변경된 이름"), presenter.state.value.todos.map { it.categoryName })
        assertEquals(listOf(CategoryColor.RED, CategoryColor.RED), presenter.state.value.todos.map { it.categoryColor })
    }

    @Test
    fun missingCategoryBecomesSafePresentationError() = runTest {
        val todoService = FakeTodoService()
        val presenter = createPresenter(todoService, FakeCategoryService(emptyList()))
        advanceUntilIdle()

        todoService.dateFlow(TODAY).emit(ServiceResult.Success(listOf(todo())))
        advanceUntilIdle()

        assertEquals(TodoListError.CATEGORY_NOT_FOUND, presenter.state.value.error)
        assertEquals(emptyList<TodoListItemUiModel>(), presenter.state.value.todos)
    }

    private fun createPresenter(
        service: FakeTodoService,
        categoryService: FakeCategoryService = FakeCategoryService(),
    ): TodoListPresenter = TodoListPresenter(service, categoryService, FIXED_CLOCK)

    private fun item(todo: Todo): TodoListItemUiModel = TodoListItemUiModel(
        id = todo.id,
        title = todo.title,
        time = todo.time,
        isCompleted = todo.isCompleted,
        categoryName = "Category",
        categoryColor = CategoryColor.NEUTRAL,
    )

    private fun todo(
        id: Long = 1,
        date: LocalDate = TODAY,
        title: String = "할 일",
        isCompleted: Boolean = false,
    ): Todo = Todo(
        id = id,
        title = title,
        date = date,
        time = null,
        categoryId = 1,
        isCompleted = isCompleted,
        createdAt = Instant.parse("2026-09-06T00:00:00Z"),
        updatedAt = Instant.parse("2026-09-06T00:00:00Z"),
    )

    private companion object {
        val TODAY: LocalDate = LocalDate.of(2026, 9, 6)
        val FIXED_CLOCK: Clock = Clock.fixed(
            Instant.parse("2026-09-06T03:00:00Z"),
            ZoneId.of("Asia/Tokyo"),
        )
    }
}
