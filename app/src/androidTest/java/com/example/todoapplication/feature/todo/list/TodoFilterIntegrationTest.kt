package com.example.todoapplication.feature.todo.list

import android.content.Context
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.lifecycle.ViewModelStore
import androidx.test.core.app.ApplicationProvider
import com.example.todoapplication.application.service.DefaultCategoryService
import com.example.todoapplication.application.service.DefaultTodoService
import com.example.todoapplication.application.service.ServiceResult
import com.example.todoapplication.data.local.TodoDatabase
import com.example.todoapplication.data.repository.RoomCategoryRepository
import com.example.todoapplication.data.repository.RoomTodoRepository
import com.example.todoapplication.domain.model.Category
import com.example.todoapplication.domain.model.CategoryColor
import com.example.todoapplication.domain.model.Todo
import com.example.todoapplication.ui.theme.ToDOApplicationTheme
import java.time.Instant
import java.time.LocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class TodoFilterIntegrationTest {
    @get:Rule val rule = createComposeRule()

    @Test
    fun actualScreenCombinesFiltersAndCategoryDeletionReassignsTodosAndClearsSelection() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val dbName = "phase10-${System.nanoTime()}.db"
        val db = TodoDatabase.create(context, dbName)
        val store = ViewModelStore()
        try {
            val repository = RoomCategoryRepository(db.categoryDao())
            val categories = DefaultCategoryService(repository)
            val todos = DefaultTodoService(RoomTodoRepository(db.todoDao()), repository)
            val work = (categories.create(Category(0, "업무", CategoryColor.BLUE, 1, false, Instant.EPOCH))
                as ServiceResult.Success).value
            val personal = (categories.create(work.copy(id = 0, name = "개인"))
                as ServiceResult.Success).value
            val general = repository.observeAll().first().first { it.isSystem }
            val today = LocalDate.now()
            suspend fun add(title: String, categoryId: Long, completed: Boolean = false, date: LocalDate = today) =
                (todos.create(Todo(0, title, date, null, categoryId, completed, Instant.EPOCH, Instant.EPOCH))
                    as ServiceResult.Success).value
            val workTodo = add("업무 미완료", work.id)
            add("업무 완료", work.id, true)
            add("개인 미완료", personal.id)
            add("일반 미완료", general.id)
            add("내일 업무", work.id, date = today.plusDays(1))
            add("내일 개인", personal.id, date = today.plusDays(1))
            val presenter = withContext(Dispatchers.Main) {
                TodoListPresenter(todos, categories).also { store.put("list", it) }
            }
            suspend fun awaitState(predicate: (TodoListUiState) -> Boolean) =
                withTimeout(5_000) { presenter.state.first { !it.isLoading && it.error == null && predicate(it) } }
            awaitState { it.todos.size == 4 }
            rule.setContent {
                val state by presenter.state.collectAsState()
                ToDOApplicationTheme { TodoListScreen(state, presenter::onEvent) }
            }
            rule.onNodeWithTag(CATEGORY_FILTER_TAG).performClick()
            rule.onNodeWithTag(CATEGORY_FILTER_OPTION_PREFIX + work.id).performClick()
            awaitState { it.todos.size == 2 }
            rule.onNodeWithTag(INCOMPLETE_FILTER_TAG).performClick()
            awaitState { it.todos.map { item -> item.id } == listOf(workTodo.id) }
            rule.onNodeWithText("업무 미완료").assertIsDisplayed()
            rule.onNodeWithText("업무 완료").assertDoesNotExist()
            rule.onNodeWithTag(NEXT_DATE_TAG).performClick()
            awaitState { it.selectedDate == today.plusDays(1) && it.todos.singleOrNull()?.title == "내일 업무" }
            rule.onNodeWithTag(CATEGORY_FILTER_TAG).performClick()
            rule.onNodeWithTag(CATEGORY_FILTER_OPTION_PREFIX + "all").performClick()
            awaitState { it.todos.size == 2 }
            rule.onNodeWithTag(PREVIOUS_DATE_TAG).performClick()
            awaitState { it.selectedDate == today && it.todos.size == 3 }
            rule.onNodeWithTag(CATEGORY_FILTER_TAG).performClick()
            rule.onNodeWithTag(CATEGORY_FILTER_OPTION_PREFIX + work.id).performClick()
            awaitState { it.todos.singleOrNull()?.id == workTodo.id }
            rule.onNodeWithContentDescription("업무 미완료 완료 상태").performClick()
            val empty = awaitState { it.todos.isEmpty() }
            assertEquals(TodoListEmptyState.NO_MATCHES, empty.emptyState)
            rule.onNodeWithText("조건에 해당하는 TODO가 없습니다.").assertIsDisplayed()
            rule.onNodeWithTag(ALL_COMPLETION_FILTER_TAG).performClick()
            awaitState { it.todos.size == 2 }
            assertEquals(ServiceResult.Success(Unit), categories.delete(work.id))
            val reassigned = awaitState { it.selectedCategoryId == null && it.todos.size == 4 }
            assertNull(reassigned.selectedCategoryId)
            assertEquals("일반", reassigned.todos.first { it.id == workTodo.id }.categoryName)
            assertTrue(reassigned.categories.none { it.id == work.id })
            assertEquals(general.id, (todos.getById(workTodo.id) as ServiceResult.Success).value.categoryId)
        } finally {
            withContext(Dispatchers.Main) { store.clear() }
            db.close()
            context.deleteDatabase(dbName)
        }
    }
}
