package com.example.todoapplication.feature.todo.edit

import com.example.todoapplication.application.service.CategoryService
import com.example.todoapplication.application.service.ServiceError
import com.example.todoapplication.application.service.ServiceResult
import com.example.todoapplication.application.service.TodoService
import com.example.todoapplication.domain.model.Category
import com.example.todoapplication.domain.model.CategoryColor
import com.example.todoapplication.domain.model.Todo
import com.example.todoapplication.test.MainDispatcherRule
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TodoEditPresenterTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun createModeUsesEntryDateAndSystemCategoryThenEditsInputs() = runTest {
        val todoService = FakeTodoService()
        val presenter = presenter(todoService = todoService)
        advanceUntilIdle()

        assertEquals(ENTRY_DATE, presenter.state.value.date)
        assertEquals(SYSTEM_CATEGORY.id, presenter.state.value.selectedCategoryId)
        assertEquals(listOf("일반", "업무"), presenter.state.value.categories.map { it.name })

        val changedDate = ENTRY_DATE.minusDays(3)
        presenter.onEvent(TodoEditEvent.TitleChanged("회의"))
        presenter.onEvent(TodoEditEvent.DateChanged(changedDate))
        presenter.onEvent(TodoEditEvent.TimeChanged(LocalTime.of(9, 30)))
        presenter.onEvent(TodoEditEvent.CategoryChanged(USER_CATEGORY.id))
        assertEquals("회의", presenter.state.value.title)
        assertEquals(changedDate, presenter.state.value.date)
        assertEquals(LocalTime.of(9, 30), presenter.state.value.time)
        assertEquals(USER_CATEGORY.id, presenter.state.value.selectedCategoryId)

        presenter.onEvent(TodoEditEvent.TimeCleared)
        assertNull(presenter.state.value.time)
    }

    @Test
    fun validCreateCallsServiceAndEmitsSavedEffect() = runTest {
        val service = FakeTodoService()
        val presenter = presenter(todoService = service)
        advanceUntilIdle()
        presenter.onEvent(TodoEditEvent.TitleChanged("새 TODO"))
        presenter.onEvent(TodoEditEvent.TimeChanged(LocalTime.of(8, 15)))
        val effect = async { presenter.effects.first() }

        presenter.onEvent(TodoEditEvent.Save)
        advanceUntilIdle()

        assertEquals(1, service.created.size)
        assertEquals("새 TODO", service.created.single().title)
        assertEquals(SYSTEM_CATEGORY.id, service.created.single().categoryId)
        assertFalse(service.created.single().isCompleted)
        assertEquals(TodoEditEffect.Saved, effect.await())
    }

    @Test
    fun blankTitleDoesNotCallServiceAndShowsFieldError() = runTest {
        val service = FakeTodoService()
        val presenter = presenter(todoService = service)
        advanceUntilIdle()

        presenter.onEvent(TodoEditEvent.Save)
        advanceUntilIdle()

        assertTrue(service.created.isEmpty())
        assertTrue(TodoEditValidationError.TITLE_REQUIRED in presenter.state.value.validationErrors)
    }

    @Test
    fun failedSaveKeepsInputAndMapsError() = runTest {
        val service = FakeTodoService().apply {
            createResult = ServiceResult.Failure(ServiceError.PersistenceFailure)
        }
        val presenter = presenter(todoService = service)
        advanceUntilIdle()
        presenter.onEvent(TodoEditEvent.TitleChanged("유지할 제목"))

        presenter.onEvent(TodoEditEvent.Save)
        advanceUntilIdle()

        assertEquals("유지할 제목", presenter.state.value.title)
        assertEquals(TodoEditError.PERSISTENCE_FAILURE, presenter.state.value.error)
        assertFalse(presenter.state.value.isSaving)
    }

    @Test
    fun savingStatePreventsDuplicateCreate() = runTest {
        val gate = CompletableDeferred<ServiceResult<Todo>>()
        val service = FakeTodoService().apply { createGate = gate }
        val presenter = presenter(todoService = service)
        advanceUntilIdle()
        presenter.onEvent(TodoEditEvent.TitleChanged("한 번만"))

        presenter.onEvent(TodoEditEvent.Save)
        presenter.onEvent(TodoEditEvent.Save)
        runCurrent()
        assertTrue(presenter.state.value.isSaving)
        assertEquals(1, service.createAttempts)

        gate.complete(ServiceResult.Success(todo(title = "한 번만")))
        advanceUntilIdle()
        assertEquals(1, service.createAttempts)
    }

    @Test
    fun editModeLoadsExistingTodoAndUpdatesPreservingCompletionAndCreationTime() = runTest {
        val existing = todo(
            id = 42,
            title = "기존",
            date = ENTRY_DATE.plusDays(2),
            time = LocalTime.of(13, 0),
            categoryId = USER_CATEGORY.id,
            isCompleted = true,
        )
        val service = FakeTodoService().apply {
            getResult = ServiceResult.Success(existing)
        }
        val presenter = presenter(
            todoService = service,
            mode = TodoEditMode.EDIT,
            todoId = existing.id,
        )
        advanceUntilIdle()

        assertEquals("기존", presenter.state.value.title)
        assertEquals(existing.date, presenter.state.value.date)
        assertEquals(existing.time, presenter.state.value.time)
        assertEquals(USER_CATEGORY.id, presenter.state.value.selectedCategoryId)

        presenter.onEvent(TodoEditEvent.TitleChanged("수정됨"))
        presenter.onEvent(TodoEditEvent.Save)
        advanceUntilIdle()

        val updated = service.updated.single()
        assertEquals(42, updated.id)
        assertEquals("수정됨", updated.title)
        assertTrue(updated.isCompleted)
        assertEquals(existing.createdAt, updated.createdAt)
    }

    @Test
    fun missingEditTodoShowsNotFoundAndDoesNotUpdate() = runTest {
        val service = FakeTodoService().apply {
            getResult = ServiceResult.Failure(ServiceError.TodoNotFound)
        }
        val presenter = presenter(
            todoService = service,
            mode = TodoEditMode.EDIT,
            todoId = 999,
        )
        advanceUntilIdle()

        assertEquals(TodoEditError.TODO_NOT_FOUND, presenter.state.value.error)
        presenter.onEvent(TodoEditEvent.Save)
        advanceUntilIdle()
        assertTrue(service.updated.isEmpty())
    }

    private fun presenter(
        todoService: FakeTodoService,
        mode: TodoEditMode = TodoEditMode.CREATE,
        todoId: Long? = null,
    ) = TodoEditPresenter(
        todoService = todoService,
        categoryService = FakeCategoryService(),
        mode = mode,
        initialDate = ENTRY_DATE,
        todoId = todoId,
        clock = FIXED_CLOCK,
    )

    private class FakeTodoService : TodoService {
        val created = mutableListOf<Todo>()
        val updated = mutableListOf<Todo>()
        var createAttempts = 0
        var createResult: ServiceResult<Todo>? = null
        var createGate: CompletableDeferred<ServiceResult<Todo>>? = null
        var getResult: ServiceResult<Todo> = ServiceResult.Failure(ServiceError.TodoNotFound)

        override suspend fun create(todo: Todo): ServiceResult<Todo> {
            createAttempts++
            created += todo
            return createGate?.await() ?: createResult ?: ServiceResult.Success(todo.copy(id = 1))
        }

        override suspend fun update(todo: Todo): ServiceResult<Todo> {
            updated += todo
            return ServiceResult.Success(todo)
        }

        override suspend fun getById(todoId: Long): ServiceResult<Todo> = getResult
        override suspend fun delete(todoId: Long): ServiceResult<Unit> = error("Not used")
        override suspend fun setCompleted(todoId: Long, completed: Boolean): ServiceResult<Todo> = error("Not used")
        override fun observeByDate(date: LocalDate): Flow<ServiceResult<List<Todo>>> = error("Not used")
        override fun observeFiltered(date: LocalDate, categoryId: Long?, incompleteOnly: Boolean): Flow<ServiceResult<List<Todo>>> = error("Not used")
    }

    private class FakeCategoryService : CategoryService {
        private val categories = MutableSharedFlow<ServiceResult<List<Category>>>(replay = 1).apply {
            tryEmit(ServiceResult.Success(listOf(SYSTEM_CATEGORY, USER_CATEGORY)))
        }

        override fun observeAll(): Flow<ServiceResult<List<Category>>> = categories
        override suspend fun create(category: Category): ServiceResult<Category> = error("Not used")
        override suspend fun update(category: Category): ServiceResult<Category> = error("Not used")
        override suspend fun delete(categoryId: Long): ServiceResult<Unit> = error("Not used")
        override suspend fun getById(categoryId: Long): ServiceResult<Category> = error("Not used")
        override suspend fun reorder(categoryIds: List<Long>): ServiceResult<Unit> = error("Not used")
    }

    private companion object {
        val ENTRY_DATE: LocalDate = LocalDate.of(2026, 9, 6)
        val FIXED_INSTANT: Instant = Instant.parse("2026-09-06T03:00:00Z")
        val FIXED_CLOCK: Clock = Clock.fixed(FIXED_INSTANT, ZoneOffset.UTC)
        val SYSTEM_CATEGORY = category(1, "일반", CategoryColor.NEUTRAL, true)
        val USER_CATEGORY = category(2, "업무", CategoryColor.BLUE, false)

        fun category(id: Long, name: String, color: CategoryColor, isSystem: Boolean) = Category(
            id = id,
            name = name,
            color = color,
            sortOrder = if (isSystem) 0 else 1,
            isSystem = isSystem,
            createdAt = FIXED_INSTANT,
        )

        fun todo(
            id: Long = 0,
            title: String,
            date: LocalDate = ENTRY_DATE,
            time: LocalTime? = null,
            categoryId: Long = 1,
            isCompleted: Boolean = false,
        ) = Todo(
            id = id,
            title = title,
            date = date,
            time = time,
            categoryId = categoryId,
            isCompleted = isCompleted,
            createdAt = Instant.parse("2026-01-01T00:00:00Z"),
            updatedAt = Instant.parse("2026-01-01T00:00:00Z"),
        )
    }
}
