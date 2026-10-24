package com.example.todoapplication.feature.todo.detail

import com.example.todoapplication.application.service.CategoryService
import com.example.todoapplication.application.service.ReminderService
import com.example.todoapplication.application.service.ServiceError
import com.example.todoapplication.application.service.ServiceResult
import com.example.todoapplication.application.service.TodoService
import com.example.todoapplication.domain.model.Category
import com.example.todoapplication.domain.model.CategoryColor
import com.example.todoapplication.domain.model.Reminder
import com.example.todoapplication.domain.model.Todo
import com.example.todoapplication.test.MainDispatcherRule
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TodoDetailPresenterTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val sampleDate = LocalDate.of(2026, 10, 24)
    private val sampleTime = LocalTime.of(15, 30)

    private val sampleTodo = Todo(
        id = 1L,
        title = "상세 확인 테스트",
        date = sampleDate,
        time = sampleTime,
        categoryId = 2L,
        isCompleted = false,
        createdAt = Instant.EPOCH,
        updatedAt = Instant.EPOCH,
    )

    private val sampleCategories = listOf(
        Category(id = 1L, name = "일반", color = CategoryColor.NEUTRAL, sortOrder = 0, isSystem = true, createdAt = Instant.EPOCH),
        Category(id = 2L, name = "업무", color = CategoryColor.BLUE, sortOrder = 1, isSystem = false, createdAt = Instant.EPOCH),
    )

    private val sampleReminders = listOf(
        Reminder(id = 10L, todoId = 1L, minutesBefore = Reminder.FIFTEEN_MINUTES_BEFORE),
    )

    @Test
    fun loadsDataByIdAndPopulatesUiState() = runTest {
        val todoFlow = MutableStateFlow<ServiceResult<Todo>>(ServiceResult.Success(sampleTodo))
        val categoryFlow = MutableStateFlow<ServiceResult<List<Category>>>(ServiceResult.Success(sampleCategories))
        val reminderFlow = MutableStateFlow<ServiceResult<List<Reminder>>>(ServiceResult.Success(sampleReminders))

        val presenter = TodoDetailPresenter(
            todoService = FakeTodoService(todoFlow),
            categoryService = FakeCategoryService(categoryFlow),
            reminderService = FakeReminderService(reminderFlow),
            todoId = 1L,
        )
        advanceUntilIdle()

        val state = presenter.state.value
        assertFalse(state.isLoading)
        assertNull(state.error)
        assertEquals(1L, state.todoId)
        assertEquals("상세 확인 테스트", state.title)
        assertEquals(sampleDate, state.date)
        assertEquals(sampleTime, state.time)
        assertEquals("업무", state.categoryName)
        assertEquals(CategoryColor.BLUE, state.categoryColor)
        assertFalse(state.isCompleted)
        assertEquals(listOf(Reminder.FIFTEEN_MINUTES_BEFORE), state.reminders)
    }

    @Test
    fun showsCompletedStateWhenTodoIsCompleted() = runTest {
        val completedTodo = sampleTodo.copy(isCompleted = true)
        val todoFlow = MutableStateFlow<ServiceResult<Todo>>(ServiceResult.Success(completedTodo))
        val categoryFlow = MutableStateFlow<ServiceResult<List<Category>>>(ServiceResult.Success(sampleCategories))
        val reminderFlow = MutableStateFlow<ServiceResult<List<Reminder>>>(ServiceResult.Success(emptyList()))

        val presenter = TodoDetailPresenter(
            todoService = FakeTodoService(todoFlow),
            categoryService = FakeCategoryService(categoryFlow),
            reminderService = FakeReminderService(reminderFlow),
            todoId = 1L,
        )
        advanceUntilIdle()

        val state = presenter.state.value
        assertTrue(state.isCompleted)
        assertTrue(state.reminders.isEmpty())
    }

    @Test
    fun showsErrorWhenTodoIsNotFound() = runTest {
        val todoFlow = MutableStateFlow<ServiceResult<Todo>>(ServiceResult.Failure(ServiceError.TodoNotFound))
        val categoryFlow = MutableStateFlow<ServiceResult<List<Category>>>(ServiceResult.Success(sampleCategories))
        val reminderFlow = MutableStateFlow<ServiceResult<List<Reminder>>>(ServiceResult.Success(emptyList()))

        val presenter = TodoDetailPresenter(
            todoService = FakeTodoService(todoFlow),
            categoryService = FakeCategoryService(categoryFlow),
            reminderService = FakeReminderService(reminderFlow),
            todoId = 1L,
        )
        advanceUntilIdle()

        val state = presenter.state.value
        assertFalse(state.isLoading)
        assertEquals(TodoDetailError.TODO_NOT_FOUND, state.error)
    }

    @Test
    fun showsErrorWhenTodoIdIsInvalid() = runTest {
        val todoFlow = MutableStateFlow<ServiceResult<Todo>>(ServiceResult.Success(sampleTodo))
        val categoryFlow = MutableStateFlow<ServiceResult<List<Category>>>(ServiceResult.Success(sampleCategories))
        val reminderFlow = MutableStateFlow<ServiceResult<List<Reminder>>>(ServiceResult.Success(emptyList()))

        val presenter = TodoDetailPresenter(
            todoService = FakeTodoService(todoFlow),
            categoryService = FakeCategoryService(categoryFlow),
            reminderService = FakeReminderService(reminderFlow),
            todoId = -1L,
        )
        advanceUntilIdle()

        val state = presenter.state.value
        assertFalse(state.isLoading)
        assertEquals(TodoDetailError.TODO_NOT_FOUND, state.error)
    }

    @Test
    fun updatesUiStateWhenDataChangesReactively() = runTest {
        val todoFlow = MutableStateFlow<ServiceResult<Todo>>(ServiceResult.Success(sampleTodo))
        val categoryFlow = MutableStateFlow<ServiceResult<List<Category>>>(ServiceResult.Success(sampleCategories))
        val reminderFlow = MutableStateFlow<ServiceResult<List<Reminder>>>(ServiceResult.Success(sampleReminders))

        val presenter = TodoDetailPresenter(
            todoService = FakeTodoService(todoFlow),
            categoryService = FakeCategoryService(categoryFlow),
            reminderService = FakeReminderService(reminderFlow),
            todoId = 1L,
        )
        advanceUntilIdle()

        // 1. Todo 수정 반영
        val updatedTodo = sampleTodo.copy(title = "수정된 제목", categoryId = 1L)
        todoFlow.value = ServiceResult.Success(updatedTodo)
        advanceUntilIdle()

        assertEquals("수정된 제목", presenter.state.value.title)
        assertEquals("일반", presenter.state.value.categoryName)
        assertEquals(CategoryColor.NEUTRAL, presenter.state.value.categoryColor)

        // 2. Reminder 변경 반영
        val updatedReminders = listOf(
            Reminder(id = 10L, todoId = 1L, minutesBefore = Reminder.FIFTEEN_MINUTES_BEFORE),
            Reminder(id = 11L, todoId = 1L, minutesBefore = Reminder.ONE_DAY_BEFORE),
        )
        reminderFlow.value = ServiceResult.Success(updatedReminders)
        advanceUntilIdle()

        assertEquals(
            listOf(Reminder.FIFTEEN_MINUTES_BEFORE, Reminder.ONE_DAY_BEFORE),
            presenter.state.value.reminders,
        )

        // 3. Todo 삭제 시 NotFound 에러 반영
        todoFlow.value = ServiceResult.Failure(ServiceError.TodoNotFound)
        advanceUntilIdle()

        assertEquals(TodoDetailError.TODO_NOT_FOUND, presenter.state.value.error)
    }

    private class FakeTodoService(
        private val todoFlow: Flow<ServiceResult<Todo>>,
    ) : TodoService {
        override fun observeById(todoId: Long): Flow<ServiceResult<Todo>> = todoFlow
        override suspend fun create(todo: Todo): ServiceResult<Todo> = error("unused")
        override suspend fun update(todo: Todo): ServiceResult<Todo> = error("unused")
        override suspend fun delete(todoId: Long): ServiceResult<Unit> = error("unused")
        override suspend fun setCompleted(todoId: Long, completed: Boolean): ServiceResult<Todo> = error("unused")
        override suspend fun getById(todoId: Long): ServiceResult<Todo> = error("unused")
        override fun observeByDate(date: LocalDate): Flow<ServiceResult<List<Todo>>> = error("unused")
        override fun observeFiltered(date: LocalDate, categoryId: Long?, incompleteOnly: Boolean): Flow<ServiceResult<List<Todo>>> = error("unused")
    }

    private class FakeCategoryService(
        private val categoryFlow: Flow<ServiceResult<List<Category>>>,
    ) : CategoryService {
        override fun observeAll(): Flow<ServiceResult<List<Category>>> = categoryFlow
        override suspend fun create(category: Category): ServiceResult<Category> = error("unused")
        override suspend fun update(category: Category): ServiceResult<Category> = error("unused")
        override suspend fun delete(categoryId: Long): ServiceResult<Unit> = error("unused")
        override suspend fun getById(categoryId: Long): ServiceResult<Category> = error("unused")
        override suspend fun reorder(categoryIds: List<Long>): ServiceResult<Unit> = error("unused")
    }

    private class FakeReminderService(
        private val reminderFlow: Flow<ServiceResult<List<Reminder>>>,
    ) : ReminderService {
        override fun observeByTodoId(todoId: Long): Flow<ServiceResult<List<Reminder>>> = reminderFlow
        override suspend fun create(reminder: Reminder): ServiceResult<Reminder> = error("unused")
        override suspend fun getById(reminderId: Long): ServiceResult<Reminder> = error("unused")
        override suspend fun replaceReminders(todoId: Long, reminders: List<Reminder>): ServiceResult<List<Reminder>> = error("unused")
        override suspend fun delete(reminderId: Long): ServiceResult<Unit> = error("unused")
        override suspend fun deleteByTodoId(todoId: Long): ServiceResult<Int> = error("unused")
        override suspend fun getFutureAlarmRecoveryCandidates(currentDate: LocalDate, currentTime: LocalTime): ServiceResult<List<Reminder>> = error("unused")
    }
}

