package com.example.todoapplication.application.service

import com.example.todoapplication.domain.model.Reminder
import com.example.todoapplication.domain.model.Todo
import com.example.todoapplication.notification.AlarmOperationResult
import com.example.todoapplication.notification.AlarmScheduler
import com.example.todoapplication.test.FakeCategoryRepository
import com.example.todoapplication.test.FakeReminderRepository
import com.example.todoapplication.test.FakeTodoRepository
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReminderRecoveryServiceTest {
    @Test
    fun restoresBothReminderTypesAcrossMultipleTodos() = runBlocking {
        val firstTodo = todo(1, LocalDate.of(2026, 10, 5), LocalTime.NOON)
        val secondTodo = todo(2, LocalDate.of(2026, 10, 6), LocalTime.of(9, 0))
        val reminders = listOf(
            Reminder(10, firstTodo.id, Reminder.ONE_DAY_BEFORE),
            Reminder(11, firstTodo.id, Reminder.FIFTEEN_MINUTES_BEFORE),
            Reminder(12, secondTodo.id, Reminder.FIFTEEN_MINUTES_BEFORE),
        )
        val scheduler = RecordingAlarmScheduler()
        val service = recoveryService(listOf(firstTodo, secondTodo), reminders, reminders, scheduler)

        val result = service.restoreScheduledReminders()

        assertEquals(ReminderRecoveryOutcome.SUCCESS, result.outcome)
        assertEquals(3, result.scheduledCount)
        assertEquals(reminders.map(Reminder::id), scheduler.scheduled.map { it.second.id })
    }

    @Test
    fun excludesCompletedMissingTimelessPastDeletedAndChangedReminders() = runBlocking {
        val future = LocalDate.of(2026, 10, 5)
        val completed = todo(1, future, LocalTime.NOON).copy(isCompleted = true)
        val timeless = todo(2, future, null)
        val past = todo(3, LocalDate.of(2026, 10, 3), LocalTime.of(9, 0))
        val validCurrent = Reminder(10, completed.id, Reminder.FIFTEEN_MINUTES_BEFORE)
        val timelessReminder = Reminder(11, timeless.id, Reminder.FIFTEEN_MINUTES_BEFORE)
        val pastReminder = Reminder(12, past.id, Reminder.FIFTEEN_MINUTES_BEFORE)
        val deletedTodoReminder = Reminder(13, 999, Reminder.FIFTEEN_MINUTES_BEFORE)
        val deletedReminder = Reminder(14, completed.id, Reminder.ONE_DAY_BEFORE)
        val storedChanged = Reminder(15, completed.id, Reminder.ONE_DAY_BEFORE)
        val staleCandidate = storedChanged.copy(minutesBefore = Reminder.FIFTEEN_MINUTES_BEFORE)
        val stored = listOf(validCurrent, timelessReminder, pastReminder, storedChanged)
        val candidates = stored + deletedTodoReminder + deletedReminder + staleCandidate
        val scheduler = RecordingAlarmScheduler()
        val service = recoveryService(
            listOf(completed, timeless, past),
            stored,
            candidates,
            scheduler,
        )

        val result = service.restoreScheduledReminders()

        assertEquals(ReminderRecoveryOutcome.SUCCESS, result.outcome)
        assertEquals(0, result.scheduledCount)
        assertEquals(candidates.size, result.skippedCount)
        assertTrue(scheduler.scheduled.isEmpty())
    }

    @Test
    fun equalOrPastFireTimeIsNotScheduled() = runBlocking {
        val equalTodo = todo(1, LocalDate.of(2026, 10, 3), LocalTime.of(12, 15))
        val pastTodo = todo(2, LocalDate.of(2026, 10, 3), LocalTime.of(12, 14))
        val reminders = listOf(
            Reminder(10, equalTodo.id, Reminder.FIFTEEN_MINUTES_BEFORE),
            Reminder(11, pastTodo.id, Reminder.FIFTEEN_MINUTES_BEFORE),
        )
        val scheduler = RecordingAlarmScheduler()
        val service = recoveryService(listOf(equalTodo, pastTodo), reminders, reminders, scheduler)

        val result = service.restoreScheduledReminders()

        assertEquals(2, result.skippedCount)
        assertTrue(scheduler.scheduled.isEmpty())
    }

    @Test
    fun oneScheduleFailureDoesNotStopRemainingCandidates() = runBlocking {
        val todo = todo(1, LocalDate.of(2026, 10, 5), LocalTime.NOON)
        val reminders = listOf(
            Reminder(10, todo.id, Reminder.ONE_DAY_BEFORE),
            Reminder(11, todo.id, Reminder.FIFTEEN_MINUTES_BEFORE),
        )
        val scheduler = RecordingAlarmScheduler(failingReminderIds = setOf(10))
        val service = recoveryService(listOf(todo), reminders, reminders, scheduler)

        val result = service.restoreScheduledReminders()

        assertEquals(ReminderRecoveryOutcome.PARTIAL_FAILURE, result.outcome)
        assertEquals(1, result.failedCount)
        assertEquals(1, result.scheduledCount)
        assertEquals(listOf(10L, 11L), scheduler.attemptedIds)
    }

    @Test
    fun repeatedRecoveryUsesSameStableReminderIdentitiesWithoutDuplicates() = runBlocking {
        val todo = todo(1, LocalDate.of(2026, 10, 5), LocalTime.NOON)
        val reminder = Reminder(10, todo.id, Reminder.FIFTEEN_MINUTES_BEFORE)
        val scheduler = RecordingAlarmScheduler()
        val service = recoveryService(listOf(todo), listOf(reminder), listOf(reminder), scheduler)

        service.restoreScheduledReminders()
        service.restoreScheduledReminders()

        assertEquals(2, scheduler.attemptedIds.size)
        assertEquals(setOf(todo.id to reminder.id), scheduler.activeIdentities)
    }

    private fun recoveryService(
        todos: List<Todo>,
        storedReminders: List<Reminder>,
        candidates: List<Reminder>,
        scheduler: AlarmScheduler,
    ): ReminderRecoveryService {
        val todoRepository = FakeTodoRepository(todos)
        val reminderRepository = FakeReminderRepository(storedReminders).apply {
            recoveryCandidates = candidates
        }
        return DefaultReminderRecoveryService(
            reminderService = DefaultReminderService(reminderRepository, todoRepository),
            todoService = DefaultTodoService(todoRepository, FakeCategoryRepository()),
            alarmScheduler = scheduler,
            clock = CLOCK,
            zoneId = ZoneOffset.UTC,
        )
    }

    private fun todo(id: Long, date: LocalDate, time: LocalTime?) = Todo(
        id = id,
        title = "TODO $id",
        date = date,
        time = time,
        categoryId = 1,
        isCompleted = false,
        createdAt = Instant.EPOCH,
        updatedAt = Instant.EPOCH,
    )

    private class RecordingAlarmScheduler(
        private val failingReminderIds: Set<Long> = emptySet(),
    ) : AlarmScheduler {
        val scheduled = mutableListOf<Pair<Todo, Reminder>>()
        val attemptedIds = mutableListOf<Long>()
        val activeIdentities = mutableSetOf<Pair<Long, Long>>()

        override fun schedule(todo: Todo, reminder: Reminder): AlarmOperationResult {
            attemptedIds += reminder.id
            if (reminder.id in failingReminderIds) return AlarmOperationResult.FAILURE
            scheduled += todo to reminder
            activeIdentities += todo.id to reminder.id
            return AlarmOperationResult.SUCCESS
        }

        override fun cancel(reminder: Reminder): AlarmOperationResult = AlarmOperationResult.SUCCESS
    }

    private companion object {
        val CLOCK: Clock = Clock.fixed(Instant.parse("2026-10-03T12:00:00Z"), ZoneOffset.UTC)
    }
}
