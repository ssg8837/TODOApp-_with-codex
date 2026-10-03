package com.example.todoapplication.notification

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.todoapplication.application.service.DefaultTodoService
import com.example.todoapplication.application.service.ServiceResult
import com.example.todoapplication.application.service.TodoSaveResult
import com.example.todoapplication.data.local.TodoDatabase
import com.example.todoapplication.data.repository.RoomCategoryRepository
import com.example.todoapplication.data.repository.RoomReminderRepository
import com.example.todoapplication.data.repository.RoomTodoRepository
import com.example.todoapplication.domain.model.Reminder
import com.example.todoapplication.domain.model.Todo
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ReminderAlarmIntegrationTest {
    @Test
    fun roomReminderStateAndAlarmSynchronizationFollowTodoLifecycle() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val name = "phase11-${System.nanoTime()}.db"
        val database = TodoDatabase.create(context, name)
        database.openHelper.writableDatabase
        try {
            val todos = RoomTodoRepository(database.todoDao())
            val categories = RoomCategoryRepository(database.categoryDao())
            val reminders = RoomReminderRepository(database.reminderDao())
            val scheduler = RecordingScheduler()
            val service = DefaultTodoService(todos, categories, reminders, scheduler)
            val categoryId = checkNotNull(categories.getSystemCategory()).id
            val input = Todo(
                id = 0,
                title = "알림 통합",
                date = LocalDate.now().plusDays(3),
                time = LocalTime.NOON,
                categoryId = categoryId,
                isCompleted = false,
                createdAt = Instant.now(),
                updatedAt = Instant.now(),
            )

            val created = (service.saveWithReminders(
                input,
                setOf(Reminder.ONE_DAY_BEFORE, Reminder.FIFTEEN_MINUTES_BEFORE),
            ) as ServiceResult.Success<TodoSaveResult>).value.todo
            val originalReminders = reminders.observeByTodoId(created.id).first()
            assertEquals(2, originalReminders.size)
            assertEquals(2, scheduler.scheduled.size)

            service.saveWithReminders(
                created.copy(time = LocalTime.of(13, 0)),
                setOf(Reminder.FIFTEEN_MINUTES_BEFORE),
            )
            assertTrue(scheduler.cancelled.containsAll(originalReminders))
            assertEquals(1, reminders.observeByTodoId(created.id).first().size)

            val cancelCount = scheduler.cancelled.size
            service.setCompleted(created.id, true)
            assertTrue(scheduler.cancelled.size > cancelCount)
            val scheduleCount = scheduler.scheduled.size
            service.setCompleted(created.id, false)
            assertTrue(scheduler.scheduled.size > scheduleCount)

            service.delete(created.id)
            assertTrue(reminders.observeByTodoId(created.id).first().isEmpty())
            assertTrue(todos.getById(created.id) == null)
        } finally {
            database.close()
            context.deleteDatabase(name)
        }
    }

    private class RecordingScheduler : AlarmScheduler {
        val scheduled = mutableListOf<Reminder>()
        val cancelled = mutableListOf<Reminder>()
        override fun schedule(todo: Todo, reminder: Reminder): AlarmOperationResult {
            scheduled += reminder
            return AlarmOperationResult.SUCCESS
        }
        override fun cancel(reminder: Reminder): AlarmOperationResult {
            cancelled += reminder
            return AlarmOperationResult.SUCCESS
        }
    }
}
