package com.example.todoapplication.data.repository

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.todoapplication.data.local.TodoDatabase
import com.example.todoapplication.domain.model.Reminder
import com.example.todoapplication.domain.repository.DataAccessException
import java.time.LocalTime
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RoomReminderRepositoryTest {
    private val databaseName = "room-reminder-repository-test.db"
    private lateinit var context: android.content.Context
    private lateinit var database: TodoDatabase
    private lateinit var todoRepository: RoomTodoRepository
    private lateinit var repository: RoomReminderRepository

    @Before
    fun setUp() {
        val created = createRepositoryTestDatabase(databaseName)
        context = created.first
        database = created.second
        todoRepository = RoomTodoRepository(database.todoDao())
        repository = RoomReminderRepository(database.reminderDao())
    }

    @After
    fun tearDown() {
        database.close()
        context.deleteDatabase(databaseName)
    }

    @Test
    fun createObserveDeleteAndRecoveryQueryUseDomainModels() = runBlocking {
        val categoryId = database.categoryDao().getSystemCategory()!!.id
        val savedTodo = todoRepository.create(todo(categoryId, "알림", LocalTime.of(10, 0)))
        val future = repository.create(Reminder(0, savedTodo.id, 15))
        val past = repository.create(Reminder(0, savedTodo.id, 300))

        assertEquals(Reminder::class, future::class)
        assertEquals(listOf(15, 300), repository.observeByTodoId(savedTodo.id).first().map { it.minutesBefore })
        assertEquals(
            listOf(future),
            repository.getFutureAlarmRecoveryCandidates(TEST_DATE, LocalTime.of(8, 20)),
        )

        assertEquals(true, repository.deleteById(past.id))
        assertEquals(1, repository.deleteByTodoId(savedTodo.id))
        assertEquals(emptyList<Reminder>(), repository.observeByTodoId(savedTodo.id).first())
    }

    @Test
    fun foreignKeyConstraintIsConvertedToDataAccessException() {
        assertThrows(DataAccessException::class.java) {
            runBlocking { repository.create(Reminder(0, Long.MAX_VALUE, 15)) }
        }
        Unit
    }
}
