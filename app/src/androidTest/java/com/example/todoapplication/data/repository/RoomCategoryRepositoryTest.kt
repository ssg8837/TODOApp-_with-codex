package com.example.todoapplication.data.repository

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.todoapplication.data.local.TodoDatabase
import com.example.todoapplication.domain.model.Category
import com.example.todoapplication.domain.repository.DataAccessException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RoomCategoryRepositoryTest {
    private val databaseName = "room-category-repository-test.db"
    private lateinit var context: android.content.Context
    private lateinit var database: TodoDatabase
    private lateinit var categoryRepository: RoomCategoryRepository
    private lateinit var todoRepository: RoomTodoRepository

    @Before
    fun setUp() {
        val created = createRepositoryTestDatabase(databaseName)
        context = created.first
        database = created.second
        categoryRepository = RoomCategoryRepository(database.categoryDao())
        todoRepository = RoomTodoRepository(database.todoDao())
    }

    @After
    fun tearDown() {
        database.close()
        context.deleteDatabase(databaseName)
    }

    @Test
    fun createReadSystemLookupNextOrderAndSaveOrderUseDomainModels() = runBlocking {
        val first = categoryRepository.create(category("첫째", 1))
        val second = categoryRepository.create(category("둘째", 2))

        assertEquals(Category::class, first::class)
        assertEquals(first, categoryRepository.getById(first.id))
        assertEquals(first, categoryRepository.getByName(first.name))
        assertEquals(true, categoryRepository.isNameTaken(first.name))
        assertEquals("일반", categoryRepository.getSystemCategory()!!.name)
        assertEquals(3, categoryRepository.getNextUserSortOrder())

        categoryRepository.saveUserCategoryOrder(listOf(second.id, first.id))
        val categories = categoryRepository.observeAll().first()
        assertEquals(listOf("일반", "둘째", "첫째"), categories.map { it.name })
        assertEquals(listOf(0, 1, 2), categories.map { it.sortOrder })
    }

    @Test
    fun deleteCategoryDelegatesAtomicReassignmentAndPreservesTodo() = runBlocking {
        val userCategory = categoryRepository.create(category("삭제 대상", 1))
        val savedTodo = todoRepository.create(todo(userCategory.id, "보존"))

        assertEquals(true, categoryRepository.deleteCategory(userCategory.id))

        assertNull(categoryRepository.getById(userCategory.id))
        val preservedTodo = todoRepository.getById(savedTodo.id)
        assertNotNull(preservedTodo)
        assertEquals(categoryRepository.getSystemCategory()!!.id, preservedTodo!!.categoryId)
    }

    @Test
    fun duplicateNameConstraintIsConvertedToDataAccessException() {
        runBlocking {
            categoryRepository.create(category("중복", 1))

            assertThrows(DataAccessException::class.java) {
                runBlocking { categoryRepository.create(category("중복", 2)) }
            }
        }
    }
}
