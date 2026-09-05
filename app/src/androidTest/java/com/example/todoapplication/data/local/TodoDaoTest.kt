package com.example.todoapplication.data.local

import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TodoDaoTest {
    private val databaseName = "todo-dao-test.db"
    private lateinit var database: TodoDatabase
    private lateinit var context: android.content.Context

    @Before
    fun setUp() {
        val created = createTestDatabase(databaseName)
        context = created.first
        database = created.second
    }

    @After
    fun tearDown() {
        database.close()
        context.deleteDatabase(databaseName)
    }

    @Test
    fun insertUpdateFindAndDeleteTodo() = runBlocking {
        val systemId = database.categoryDao().getSystemCategory()!!.id
        val id = database.todoDao().insert(todo(systemId, "original"))

        val inserted = database.todoDao().getById(id)!!
        assertEquals("original", inserted.title)

        assertEquals(1, database.todoDao().update(inserted.copy(title = "updated")))
        assertEquals("updated", database.todoDao().getById(id)!!.title)

        assertEquals(1, database.todoDao().delete(inserted.copy(title = "updated")))
        assertNull(database.todoDao().getById(id))
    }

    @Test
    fun dateQueryUsesTimedThenTimeThenCreationOrderAndIgnoresCompletion() = runBlocking {
        val categoryId = database.categoryDao().getSystemCategory()!!.id
        database.todoDao().insert(todo(categoryId, "no time", null, 1L))
        database.todoDao().insert(todo(categoryId, "late", 600, 2L))
        database.todoDao().insert(todo(categoryId, "same time newer", 480, 4L))
        database.todoDao().insert(todo(categoryId, "same time older completed", 480, 3L, true))
        database.todoDao().insert(
            todo(categoryId, "other date").copy(dateEpochDay = TEST_DATE_EPOCH_DAY + 1),
        )

        val results = database.todoDao().observeByDate(TEST_DATE_EPOCH_DAY).first()

        assertEquals(
            listOf("same time older completed", "same time newer", "late", "no time"),
            results.map { it.title },
        )
    }
}
