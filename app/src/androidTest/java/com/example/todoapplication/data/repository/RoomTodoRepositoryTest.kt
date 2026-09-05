package com.example.todoapplication.data.repository

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.todoapplication.data.local.TodoDatabase
import com.example.todoapplication.domain.model.Todo
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RoomTodoRepositoryTest {
    private val databaseName = "room-todo-repository-test.db"
    private lateinit var context: android.content.Context
    private lateinit var database: TodoDatabase
    private lateinit var repository: RoomTodoRepository

    @Before
    fun setUp() {
        val created = createRepositoryTestDatabase(databaseName)
        context = created.first
        database = created.second
        repository = RoomTodoRepository(database.todoDao())
    }

    @After
    fun tearDown() {
        database.close()
        context.deleteDatabase(databaseName)
    }

    @Test
    fun createReadUpdateObserveAndDeleteUseDomainModels() = runBlocking {
        val categoryId = database.categoryDao().getSystemCategory()!!.id
        val created = repository.create(todo(categoryId, "원본"))

        assertEquals(Todo::class, created::class)
        assertEquals(created, repository.getById(created.id))
        assertEquals(listOf(created), repository.observeByDate(TEST_DATE).first())

        val updated = created.copy(title = "수정", isCompleted = true)
        assertEquals(true, repository.update(updated))
        assertEquals(updated, repository.getById(created.id))
        assertEquals(
            listOf(updated),
            repository.observeFiltered(TEST_DATE, categoryId, incompleteOnly = false).first(),
        )
        assertEquals(emptyList<Todo>(), repository.observeFiltered(TEST_DATE, categoryId, true).first())

        assertEquals(true, repository.delete(updated))
        assertNull(repository.getById(created.id))
    }
}
