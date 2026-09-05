package com.example.todoapplication.data.local

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.todoapplication.domain.model.CategoryColor
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CategoryDaoTest {
    private val databaseName = "category-dao-test.db"
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
    fun duplicateNameIsRejectedByDatabaseConstraint() = runBlocking {
        database.categoryDao().insert(userCategory("업무", 1))

        try {
            database.categoryDao().insert(userCategory("업무", 2))
            fail("Duplicate category name must fail")
        } catch (_: Exception) {
            // Expected: Room surfaces SQLite's UNIQUE constraint failure.
        }

        assertEquals(1, database.categoryDao().countByName("업무"))
    }

    @Test
    fun categoriesAreOrderedWithSystemFirstThenSavedUserOrder() = runBlocking {
        val thirdId = database.categoryDao().insert(userCategory("세 번째", 3))
        val firstId = database.categoryDao().insert(userCategory("첫 번째", 1))
        val secondId = database.categoryDao().insert(userCategory("두 번째", 2))

        assertEquals(4, database.categoryDao().getNextUserSortOrder())
        assertEquals(
            listOf("일반", "첫 번째", "두 번째", "세 번째"),
            database.categoryDao().observeAll().first().map { it.name },
        )

        database.categoryDao().saveUserCategoryOrder(listOf(thirdId, secondId, firstId))

        val reordered = database.categoryDao().observeAll().first()
        assertEquals(listOf("일반", "세 번째", "두 번째", "첫 번째"), reordered.map { it.name })
        assertEquals(listOf(0, 1, 2, 3), reordered.map { it.sortOrder })
    }

    @Test
    fun userCategoryCanBeUpdatedButSystemCategoryCannot() = runBlocking {
        val userId = database.categoryDao().insert(userCategory("기존", 1))
        val system = database.categoryDao().getSystemCategory()!!

        assertEquals(
            1,
            database.categoryDao().updateUserCategory(
                userId,
                "변경",
                CategoryColor.RED.storageValue,
                1,
            ),
        )
        assertEquals("변경", database.categoryDao().getById(userId)!!.name)
        assertEquals(
            0,
            database.categoryDao().updateUserCategory(
                system.id,
                "변경 불가",
                CategoryColor.RED.storageValue,
                1,
            ),
        )
        assertEquals("일반", database.categoryDao().getById(system.id)!!.name)
    }

    @Test
    fun deletingUserCategoryReassignsTodoToSystemWithoutDeletingTodo() = runBlocking {
        val userId = database.categoryDao().insert(userCategory("업무", 1))
        val todoId = database.todoDao().insert(todo(userId, "보존할 TODO"))

        assertEquals(true, database.categoryDao().deleteUserCategoryAndReassignTodos(userId))

        assertNull(database.categoryDao().getById(userId))
        val preservedTodo = database.todoDao().getById(todoId)
        assertNotNull(preservedTodo)
        assertEquals(database.categoryDao().getSystemCategory()!!.id, preservedTodo!!.categoryId)
    }

    @Test
    fun systemCategoryDeletionIsRejected() = runBlocking {
        val system = database.categoryDao().getSystemCategory()!!

        try {
            database.categoryDao().deleteUserCategoryAndReassignTodos(system.id)
            fail("System category deletion must fail")
        } catch (_: IllegalArgumentException) {
            // Expected protection at the transaction boundary.
        }

        assertNotNull(database.categoryDao().getById(system.id))
    }

    @Test
    fun failedCategoryDeletionRollsBackTodoReassignment() = runBlocking {
        val userId = database.categoryDao().insert(userCategory("롤백", 1))
        val todoId = database.todoDao().insert(todo(userId, "원래 Category 유지"))
        database.openHelper.writableDatabase.execSQL(
            """
            CREATE TRIGGER force_category_delete_failure
            BEFORE DELETE ON categories
            WHEN OLD.id = $userId
            BEGIN
                SELECT RAISE(ABORT, 'forced failure');
            END
            """.trimIndent(),
        )

        try {
            database.categoryDao().deleteUserCategoryAndReassignTodos(userId)
            fail("Forced deletion failure must propagate")
        } catch (_: Exception) {
            // Expected: transaction must roll back the preceding Todo update.
        }

        assertNotNull(database.categoryDao().getById(userId))
        assertEquals(userId, database.todoDao().getById(todoId)!!.categoryId)
    }
}
