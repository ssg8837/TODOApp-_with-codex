package com.example.todoapplication.data.local

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.todoapplication.domain.model.Category
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TodoDatabaseTest {
    private val databaseName = "todo-database-test.db"
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
    fun firstCreationSeedsExactlyOneSystemCategoryWithoutAssumingItsId() = runBlocking {
        val systemCategory = database.categoryDao().getSystemCategory()

        assertNotNull(systemCategory)
        assertNotEquals(0L, systemCategory!!.id)
        assertEquals(Category.SYSTEM_DEFAULT_NAME, systemCategory.name)
        assertEquals(Category.SYSTEM_DEFAULT_COLOR.storageValue, systemCategory.colorValue)
        assertEquals(Category.SYSTEM_DEFAULT_SORT_ORDER, systemCategory.sortOrder)
        assertEquals(1, database.categoryDao().countSystemCategories())
        assertEquals(1, database.categoryDao().countByName(Category.SYSTEM_DEFAULT_NAME))

        database.close()
        database = TodoDatabase.create(context, databaseName)
        database.openHelper.writableDatabase

        assertEquals(1, database.categoryDao().countSystemCategories())
        assertEquals(1, database.categoryDao().countByName(Category.SYSTEM_DEFAULT_NAME))
    }
}
