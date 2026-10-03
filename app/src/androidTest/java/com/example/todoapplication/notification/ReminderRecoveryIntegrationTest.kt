package com.example.todoapplication.notification

import android.content.Context
import android.content.pm.PackageManager
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.todoapplication.application.service.DefaultReminderRecoveryService
import com.example.todoapplication.application.service.DefaultReminderService
import com.example.todoapplication.application.service.DefaultTodoService
import com.example.todoapplication.application.service.ReminderRecoveryOutcome
import com.example.todoapplication.application.service.ServiceResult
import com.example.todoapplication.application.service.TodoSaveResult
import com.example.todoapplication.data.local.TodoDatabase
import com.example.todoapplication.data.repository.RoomCategoryRepository
import com.example.todoapplication.data.repository.RoomReminderRepository
import com.example.todoapplication.data.repository.RoomTodoRepository
import com.example.todoapplication.domain.model.Reminder
import com.example.todoapplication.domain.model.Todo
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ReminderRecoveryIntegrationTest {
    @Test
    fun roomSourceOfTruthRestoresFutureAndExcludesCompletedDeletedAndPast() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val name = "phase12-${System.nanoTime()}.db"
        val database = TodoDatabase.create(context, name)
        database.openHelper.writableDatabase
        try {
            val todoRepository = RoomTodoRepository(database.todoDao())
            val categoryRepository = RoomCategoryRepository(database.categoryDao())
            val reminderRepository = RoomReminderRepository(database.reminderDao())
            val scheduler = RecordingScheduler()
            val todoService = DefaultTodoService(
                todoRepository,
                categoryRepository,
                reminderRepository,
                scheduler,
            )
            val reminderService = DefaultReminderService(reminderRepository, todoRepository)
            val recovery = DefaultReminderRecoveryService(
                reminderService,
                todoService,
                scheduler,
                CLOCK,
                ZoneOffset.UTC,
            )
            val categoryId = checkNotNull(categoryRepository.getSystemCategory()).id
            val future = saveTodo(
                todoService,
                categoryId,
                LocalDate.of(2026, 10, 5),
                LocalTime.NOON,
                setOf(Reminder.ONE_DAY_BEFORE, Reminder.FIFTEEN_MINUTES_BEFORE),
            )
            scheduler.clear()

            val restored = recovery.restoreScheduledReminders()
            assertEquals(ReminderRecoveryOutcome.SUCCESS, restored.outcome)
            assertEquals(2, restored.scheduledCount)
            assertEquals(2, scheduler.scheduled.size)

            todoService.setCompleted(future.id, true)
            scheduler.clear()
            assertEquals(0, recovery.restoreScheduledReminders().scheduledCount)

            todoService.setCompleted(future.id, false)
            todoService.delete(future.id)
            scheduler.clear()
            assertTrue(reminderRepository.observeByTodoId(future.id).first().isEmpty())
            assertEquals(0, recovery.restoreScheduledReminders().scheduledCount)

            saveTodo(
                todoService,
                categoryId,
                LocalDate.of(2026, 10, 3),
                LocalTime.of(12, 14),
                setOf(Reminder.FIFTEEN_MINUTES_BEFORE),
            )
            scheduler.clear()
            assertEquals(0, recovery.restoreScheduledReminders().scheduledCount)
        } finally {
            database.close()
            context.deleteDatabase(name)
        }
    }

    @Test
    fun manifestDeclaresEnabledExportedBootReceiverAndBootPermission() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val packageInfo = context.packageManager.getPackageInfo(
            context.packageName,
            PackageManager.GET_PERMISSIONS or PackageManager.GET_RECEIVERS,
        )
        assertTrue(
            packageInfo.requestedPermissions.orEmpty()
                .contains("android.permission.RECEIVE_BOOT_COMPLETED"),
        )
        val receiver = packageInfo.receivers.orEmpty().first {
            it.name == BootReceiver::class.java.name
        }
        assertTrue(receiver.enabled)
        assertTrue(receiver.exported)
    }

    private suspend fun saveTodo(
        service: DefaultTodoService,
        categoryId: Long,
        date: LocalDate,
        time: LocalTime,
        reminderMinutes: Set<Int>,
    ): Todo {
        val todo = Todo(
            id = 0,
            title = "복구 테스트",
            date = date,
            time = time,
            categoryId = categoryId,
            isCompleted = false,
            createdAt = Instant.EPOCH,
            updatedAt = Instant.EPOCH,
        )
        return (service.saveWithReminders(todo, reminderMinutes)
            as ServiceResult.Success<TodoSaveResult>).value.todo
    }

    private class RecordingScheduler : AlarmScheduler {
        val scheduled = mutableListOf<Reminder>()
        override fun schedule(todo: Todo, reminder: Reminder): AlarmOperationResult {
            scheduled += reminder
            return AlarmOperationResult.SUCCESS
        }
        override fun cancel(reminder: Reminder): AlarmOperationResult = AlarmOperationResult.SUCCESS
        fun clear() = scheduled.clear()
    }

    private companion object {
        val CLOCK: Clock = Clock.fixed(Instant.parse("2026-10-03T12:00:00Z"), ZoneOffset.UTC)
    }
}
