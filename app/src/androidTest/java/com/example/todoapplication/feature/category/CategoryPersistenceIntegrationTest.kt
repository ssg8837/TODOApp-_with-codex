package com.example.todoapplication.feature.category

import android.content.Context
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
import com.example.todoapplication.feature.todo.edit.TodoEditMode
import com.example.todoapplication.feature.todo.edit.TodoEditPresenter
import com.example.todoapplication.feature.todo.list.TodoListPresenter
import java.time.Instant
import java.time.LocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CategoryPersistenceIntegrationTest {
    @Test
    fun presenterChangesPersistAcrossDatabaseReopenAndUpdateTodoPresenters() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val name = "phase9-${System.nanoTime()}.db"
        var db = TodoDatabase.create(context, name)
        val store = ViewModelStore()
        try {
            val repository = RoomCategoryRepository(db.categoryDao())
            val service = DefaultCategoryService(repository)
            val todoService = DefaultTodoService(RoomTodoRepository(db.todoDao()), repository)
            val created = listOf("업무", "개인", "공부").map {
                (service.create(Category(0, it, CategoryColor.RED, 1, false, Instant.EPOCH))
                    as ServiceResult.Success).value
            }
            val work = created[0]
            val system = repository.observeAll().first().first { it.isSystem }
            val date = LocalDate.now()
            todoService.create(Todo(0, "색상 반영", date, null, work.id, false, Instant.EPOCH, Instant.EPOCH))
            val management = withContext(Dispatchers.Main) {
                CategoryManagementPresenter(service).also { store.put("management", it) }
            }
            val list = withContext(Dispatchers.Main) {
                TodoListPresenter(todoService, service).also { store.put("list", it) }
            }
            val edit = withContext(Dispatchers.Main) {
                TodoEditPresenter(todoService, service, TodoEditMode.CREATE, date)
                    .also { store.put("edit", it) }
            }
            withTimeout(5_000) { management.state.first { it.categories.size == 4 } }
            val order = listOf(created[2].id, work.id, created[1].id)
            withContext(Dispatchers.Main) {
                management.onEvent(CategoryManagementEvent.ReorderCategories(order))
            }
            withTimeout(5_000) {
                management.state.first { !it.isReordering && it.categories.map { item -> item.id } == listOf(system.id) + order }
            }
            withContext(Dispatchers.Main) {
                management.onEvent(CategoryManagementEvent.RequestEdit(work.id))
                management.onEvent(CategoryManagementEvent.CategoryColorChanged(CategoryColor.BLUE))
                management.onEvent(CategoryManagementEvent.SaveCategory)
            }
            withTimeout(5_000) {
                list.state.first { it.todos.singleOrNull()?.categoryColor == CategoryColor.BLUE }
                edit.state.first {
                    it.categories.map { item -> item.id } == listOf(system.id) + order &&
                        it.categories.first { item -> item.id == work.id }.color == CategoryColor.BLUE
                }
                management.state.first { !it.isSaving }
            }
            withContext(Dispatchers.Main) {
                management.onEvent(CategoryManagementEvent.ReorderCategories(listOf(system.id) + order))
                management.onEvent(CategoryManagementEvent.RequestEdit(system.id))
                management.onEvent(CategoryManagementEvent.CategoryColorChanged(CategoryColor.PINK))
                management.onEvent(CategoryManagementEvent.SaveCategory)
            }
            assertEquals(CategoryManagementError.SYSTEM_OPERATION_PROHIBITED, management.state.value.error)
            withContext(Dispatchers.Main) { store.clear() }
            db.close()
            db = TodoDatabase.create(context, name)
            val reloaded = RoomCategoryRepository(db.categoryDao()).observeAll().first()
            assertEquals(listOf(system.id) + order, reloaded.map { it.id })
            assertEquals(listOf(0, 1, 2, 3), reloaded.map { it.sortOrder })
            assertEquals(CategoryColor.BLUE, reloaded.first { it.id == work.id }.color)
            assertEquals(system, reloaded.first())
        } finally {
            withContext(Dispatchers.Main) { store.clear() }
            db.close()
            context.deleteDatabase(name)
        }
    }

    @Test
    fun failedReorderRollsBackAllPriorUpdates() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val name = "phase9-rollback-${System.nanoTime()}.db"
        val db = TodoDatabase.create(context, name)
        try {
            val repository = RoomCategoryRepository(db.categoryDao())
            val service = DefaultCategoryService(repository)
            val created = listOf("업무", "개인", "공부").map {
                (service.create(Category(0, it, CategoryColor.RED, 1, false, Instant.EPOCH))
                    as ServiceResult.Success).value
            }
            val before = repository.observeAll().first()
            db.openHelper.writableDatabase.execSQL(
                """CREATE TRIGGER phase9_reorder_failure BEFORE UPDATE OF sort_order ON categories
                   WHEN OLD.id = ${created[0].id}
                   BEGIN SELECT RAISE(ABORT, 'test failure'); END""",
            )
            val result = service.reorder(listOf(created[2].id, created[0].id, created[1].id))
            assertTrue(result is ServiceResult.Failure)
            assertEquals(before, repository.observeAll().first())
        } finally {
            db.close()
            context.deleteDatabase(name)
        }
    }
}
