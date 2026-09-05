package com.example.todoapplication.app

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.todoapplication.data.local.TodoDatabase
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DefaultAppContainerTest {
    private lateinit var context: Context
    private lateinit var container: AppContainer

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        context.deleteDatabase(TodoDatabase.DATABASE_NAME)
        container = DefaultAppContainer(context)
        container.database.openHelper.writableDatabase
    }

    @After
    fun tearDown() {
        container.database.close()
        context.deleteDatabase(TodoDatabase.DATABASE_NAME)
    }

    @Test
    fun containerAssemblesDatabaseAndAllRepositoryInterfaces() = runBlocking {
        assertNotNull(container.todoRepository)
        assertNotNull(container.categoryRepository)
        assertNotNull(container.reminderRepository)
        assertNotNull(container.categoryRepository.getSystemCategory())
    }
}
