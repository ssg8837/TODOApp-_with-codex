package com.example.todoapplication.data.local

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.todoapplication.data.local.entity.ReminderEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ReminderDaoTest {
    private val databaseName = "reminder-dao-test.db"
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
    fun insertReadDeleteAndTodoCascadeAreSupported() = runBlocking {
        val categoryId = database.categoryDao().getSystemCategory()!!.id
        val todoId = database.todoDao().insert(todo(categoryId, "todo", timeMinuteOfDay = 600))
        val firstId = database.reminderDao().insert(ReminderEntity(todoId = todoId, minutesBefore = 15))
        database.reminderDao().insert(ReminderEntity(todoId = todoId, minutesBefore = 1_440))

        assertEquals(listOf(15, 1_440), database.reminderDao().observeByTodoId(todoId).first().map { it.minutesBefore })
        assertEquals(1, database.reminderDao().deleteById(firstId))
        assertEquals(1, database.reminderDao().observeByTodoId(todoId).first().size)

        database.todoDao().delete(database.todoDao().getById(todoId)!!)
        assertEquals(emptyList<ReminderEntity>(), database.reminderDao().observeByTodoId(todoId).first())
    }

    @Test
    fun recoveryQueryReturnsOnlyFutureRemindersForIncompleteTimedTodos() = runBlocking {
        val categoryId = database.categoryDao().getSystemCategory()!!.id
        val futureTodo = database.todoDao().insert(todo(categoryId, "future", 600))
        val completedTodo = database.todoDao().insert(todo(categoryId, "completed", 700, isCompleted = true))
        val untimedTodo = database.todoDao().insert(todo(categoryId, "untimed"))
        database.reminderDao().insert(ReminderEntity(todoId = futureTodo, minutesBefore = 15))
        database.reminderDao().insert(ReminderEntity(todoId = futureTodo, minutesBefore = 300))
        database.reminderDao().insert(ReminderEntity(todoId = completedTodo, minutesBefore = 15))
        database.reminderDao().insert(ReminderEntity(todoId = untimedTodo, minutesBefore = 15))

        val candidates = database.reminderDao().getFutureAlarmRecoveryCandidates(
            currentDateEpochDay = TEST_DATE_EPOCH_DAY,
            currentMinuteOfDay = 500,
        )

        assertEquals(listOf(15), candidates.map { it.minutesBefore })
    }

    @Test
    fun replaceByTodoIdReplacesExistingRemindersAndAcceptsEmptyList() = runBlocking {
        val categoryId = database.categoryDao().getSystemCategory()!!.id
        val todoId = database.todoDao().insert(todo(categoryId, "replace", 600))
        database.reminderDao().insert(ReminderEntity(todoId = todoId, minutesBefore = 5))
        database.reminderDao().insert(ReminderEntity(todoId = todoId, minutesBefore = 10))

        val ids = database.reminderDao().replaceByTodoId(
            todoId,
            listOf(
                ReminderEntity(todoId = todoId, minutesBefore = 15),
                ReminderEntity(todoId = todoId, minutesBefore = 1_440),
            ),
        )

        assertEquals(2, ids.size)
        assertEquals(
            listOf(15, 1_440),
            database.reminderDao().observeByTodoId(todoId).first().map { it.minutesBefore },
        )

        assertEquals(emptyList<Long>(), database.reminderDao().replaceByTodoId(todoId, emptyList()))
        assertEquals(
            emptyList<ReminderEntity>(),
            database.reminderDao().observeByTodoId(todoId).first(),
        )
    }

    @Test
    fun replaceByTodoIdRollsBackDeletionAndPartialInsertionWhenInsertFails() = runBlocking {
        val categoryId = database.categoryDao().getSystemCategory()!!.id
        val todoId = database.todoDao().insert(todo(categoryId, "rollback", 600))
        database.reminderDao().insert(ReminderEntity(todoId = todoId, minutesBefore = 5))
        database.reminderDao().insert(ReminderEntity(todoId = todoId, minutesBefore = 10))
        database.openHelper.writableDatabase.execSQL(
            """
            CREATE TRIGGER fail_second_replacement_reminder
            BEFORE INSERT ON reminders
            WHEN NEW.minutes_before = 1440
            BEGIN
                SELECT RAISE(ABORT, 'forced replacement failure');
            END
            """.trimIndent(),
        )

        try {
            database.reminderDao().replaceByTodoId(
                todoId,
                listOf(
                    ReminderEntity(todoId = todoId, minutesBefore = 15),
                    ReminderEntity(todoId = todoId, minutesBefore = 1_440),
                ),
            )
            fail("Replacement must fail")
        } catch (_: Exception) {
            // The assertions below verify rollback rather than a specific SQLite exception type.
        }

        assertEquals(
            listOf(5, 10),
            database.reminderDao().observeByTodoId(todoId).first().map { it.minutesBefore },
        )
    }
}
