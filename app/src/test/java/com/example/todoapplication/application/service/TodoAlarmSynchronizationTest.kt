package com.example.todoapplication.application.service

import com.example.todoapplication.domain.model.Reminder
import com.example.todoapplication.notification.AlarmOperationResult
import com.example.todoapplication.notification.AlarmScheduler
import com.example.todoapplication.test.FakeCategoryRepository
import com.example.todoapplication.test.FakeReminderRepository
import com.example.todoapplication.test.FakeTodoRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TodoAlarmSynchronizationTest {
    @Test
    fun createUsesGeneratedTodoIdAndSchedulesEachSavedReminderOnce() = runBlocking {
        val reminders = FakeReminderRepository()
        val scheduler = FakeAlarmScheduler()
        val service = service(FakeTodoRepository(), reminders, scheduler)

        val result = service.saveWithReminders(
            todo(id = 0),
            setOf(Reminder.ONE_DAY_BEFORE, Reminder.FIFTEEN_MINUTES_BEFORE),
        ) as ServiceResult.Success

        assertTrue(result.value.todo.id > 0)
        assertEquals(2, scheduler.scheduled.size)
        assertTrue(scheduler.scheduled.all { (_, reminder) -> reminder.todoId == result.value.todo.id })
        assertTrue(scheduler.scheduled.all { (_, reminder) -> reminder.id > 0 })
    }

    @Test
    fun updateCancelsOldIdentityThenSchedulesReplacement() = runBlocking {
        val existing = todo(id = 5)
        val old = Reminder(7, existing.id, Reminder.ONE_DAY_BEFORE)
        val reminders = FakeReminderRepository(listOf(old))
        val scheduler = FakeAlarmScheduler()
        val service = service(FakeTodoRepository(listOf(existing)), reminders, scheduler)

        service.saveWithReminders(existing.copy(title = "변경"), setOf(Reminder.FIFTEEN_MINUTES_BEFORE))

        assertEquals(listOf(old), scheduler.cancelled)
        assertEquals(listOf(Reminder.FIFTEEN_MINUTES_BEFORE), scheduler.scheduled.map { it.second.minutesBefore })
    }

    @Test
    fun completionCancelsAndRestorationReschedulesStoredReminders() = runBlocking {
        val existing = todo(id = 5)
        val reminder = Reminder(7, existing.id, Reminder.FIFTEEN_MINUTES_BEFORE)
        val reminders = FakeReminderRepository(listOf(reminder))
        val scheduler = FakeAlarmScheduler()
        val service = service(FakeTodoRepository(listOf(existing)), reminders, scheduler)

        service.setCompleted(existing.id, true)
        service.setCompleted(existing.id, false)

        assertEquals(listOf(reminder, reminder), scheduler.cancelled)
        assertEquals(listOf(reminder), scheduler.scheduled.map { it.second })
        assertEquals(1, reminders.observeByTodoId(existing.id).first().size)
    }

    @Test
    fun deleteCapturesReminderBeforeCascadeEquivalentTodoDeleteAndCancelsIt() = runBlocking {
        val existing = todo(id = 5)
        val reminder = Reminder(7, existing.id, Reminder.FIFTEEN_MINUTES_BEFORE)
        val scheduler = FakeAlarmScheduler()
        val service = service(
            FakeTodoRepository(listOf(existing)),
            FakeReminderRepository(listOf(reminder)),
            scheduler,
        )

        service.delete(existing.id)

        assertEquals(listOf(reminder), scheduler.cancelled)
    }

    @Test
    fun persistenceFailureDoesNotScheduleAndSchedulerFailureIsReportedForRetry() = runBlocking {
        val reminders = FakeReminderRepository().apply { failReplaceWithPersistenceError = true }
        val scheduler = FakeAlarmScheduler()
        val service = service(FakeTodoRepository(), reminders, scheduler)
        assertEquals(
            ServiceResult.Failure(ServiceError.PersistenceFailure),
            service.saveWithReminders(todo(id = 0), setOf(Reminder.FIFTEEN_MINUTES_BEFORE)),
        )
        assertTrue(scheduler.scheduled.isEmpty())

        reminders.failReplaceWithPersistenceError = false
        scheduler.scheduleResult = AlarmOperationResult.FAILURE
        val saved = service.saveWithReminders(todo(id = 0), setOf(Reminder.FIFTEEN_MINUTES_BEFORE))
            as ServiceResult.Success
        assertEquals(AlarmSyncStatus.FAILED, saved.value.alarmSyncStatus)
        assertEquals(
            ServiceResult.Success(AlarmSyncStatus.FAILED),
            service.resynchronizeAlarms(saved.value.todo.id),
        )
    }

    private fun service(
        todos: FakeTodoRepository,
        reminders: FakeReminderRepository,
        scheduler: FakeAlarmScheduler,
    ) = DefaultTodoService(
        todoRepository = todos,
        categoryRepository = FakeCategoryRepository(listOf(systemCategory())),
        reminderRepository = reminders,
        alarmScheduler = scheduler,
    )

    private class FakeAlarmScheduler : AlarmScheduler {
        val scheduled = mutableListOf<Pair<com.example.todoapplication.domain.model.Todo, Reminder>>()
        val cancelled = mutableListOf<Reminder>()
        var scheduleResult = AlarmOperationResult.SUCCESS

        override fun schedule(
            todo: com.example.todoapplication.domain.model.Todo,
            reminder: Reminder,
        ): AlarmOperationResult {
            scheduled += todo to reminder
            return scheduleResult
        }

        override fun cancel(reminder: Reminder): AlarmOperationResult {
            cancelled += reminder
            return AlarmOperationResult.SUCCESS
        }
    }
}
