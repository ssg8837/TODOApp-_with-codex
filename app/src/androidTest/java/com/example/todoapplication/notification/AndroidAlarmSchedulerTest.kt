package com.example.todoapplication.notification

import android.app.PendingIntent
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.todoapplication.domain.model.Reminder
import com.example.todoapplication.domain.model.Todo
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AndroidAlarmSchedulerTest {
    @Test
    fun exactAllowedUsesExactAndSecurityFailureFallsBackToInexact() {
        val backend = FakeBackend(canExact = true, failExact = true)
        val scheduler = scheduler(backend)

        assertEquals(AlarmOperationResult.INEXACT_SCHEDULED, scheduler.schedule(todo(), reminder(1)))
        assertEquals(1, backend.exactCalls)
        assertEquals(1, backend.inexactCalls)
    }

    @Test
    fun exactUnavailableUsesInexactAndPastReminderIsSkipped() {
        val backend = FakeBackend(canExact = false)
        val scheduler = scheduler(backend)

        scheduler.schedule(todo(), reminder(1))
        assertEquals(0, backend.exactCalls)
        assertEquals(1, backend.inexactCalls)

        val pastTodo = todo().copy(date = LocalDate.of(2026, 9, 19))
        assertEquals(AlarmOperationResult.SKIPPED_PAST, scheduler.schedule(pastTodo, reminder(2)))
        assertEquals(1, backend.inexactCalls)
    }

    @Test
    fun notificationPermissionDenialDoesNotTouchAlarmManager() {
        val backend = FakeBackend(canExact = true)
        val scheduler = AndroidAlarmScheduler(
            context = ApplicationProvider.getApplicationContext(),
            clock = CLOCK,
            zoneId = ZoneOffset.UTC,
            backend = backend,
            notificationPermissionGranted = { false },
        )

        assertEquals(
            AlarmOperationResult.NOTIFICATION_PERMISSION_DENIED,
            scheduler.schedule(todo(), reminder(1)),
        )
        assertEquals(0, backend.exactCalls + backend.inexactCalls)
    }

    @Test
    fun onlyFutureReminderIsScheduledWhenOneDayReminderIsAlreadyPast() {
        val backend = FakeBackend(canExact = true)
        val scheduler = scheduler(backend)
        val nearTodo = todo().copy(date = LocalDate.of(2026, 9, 20), time = LocalTime.of(1, 0))

        assertEquals(
            AlarmOperationResult.SKIPPED_PAST,
            scheduler.schedule(nearTodo, Reminder(1, nearTodo.id, Reminder.ONE_DAY_BEFORE)),
        )
        assertEquals(
            AlarmOperationResult.SUCCESS,
            scheduler.schedule(nearTodo, Reminder(2, nearTodo.id, Reminder.FIFTEEN_MINUTES_BEFORE)),
        )
        assertEquals(1, backend.exactCalls)
    }

    @Test
    fun pendingIntentIdentityInputsDoNotCollideAcrossTodoOrReminder() {
        val first = AlarmIdentity.uri(Reminder(1, 1, 15))
        val differentReminder = AlarmIdentity.uri(Reminder(2, 1, 1_440))
        val differentTodo = AlarmIdentity.uri(Reminder(1, 2, 15))

        assertNotEquals(first, differentReminder)
        assertNotEquals(first, differentTodo)
        assertNotEquals(
            AlarmIdentity.notificationId(Reminder(1, 1, 15)),
            AlarmIdentity.notificationId(Reminder(2, 1, 1_440)),
        )
    }

    private fun scheduler(backend: FakeBackend) = AndroidAlarmScheduler(
        context = ApplicationProvider.getApplicationContext(),
        clock = CLOCK,
        zoneId = ZoneOffset.UTC,
        backend = backend,
        notificationPermissionGranted = { true },
    )

    private fun todo() = Todo(
        id = 1,
        title = "TODO",
        date = LocalDate.of(2026, 9, 21),
        time = LocalTime.NOON,
        categoryId = 1,
        isCompleted = false,
        createdAt = Instant.EPOCH,
        updatedAt = Instant.EPOCH,
    )

    private fun reminder(id: Long) = Reminder(id, 1, Reminder.FIFTEEN_MINUTES_BEFORE)

    private class FakeBackend(
        private val canExact: Boolean,
        private val failExact: Boolean = false,
    ) : AlarmBackend {
        var exactCalls = 0
        var inexactCalls = 0

        override fun canScheduleExactAlarms(): Boolean = canExact
        override fun scheduleExact(triggerAtMillis: Long, operation: PendingIntent) {
            exactCalls++
            if (failExact) throw SecurityException("revoked")
        }
        override fun scheduleInexact(triggerAtMillis: Long, operation: PendingIntent) {
            inexactCalls++
        }
        override fun cancel(operation: PendingIntent) = Unit
    }

    private companion object {
        val CLOCK: Clock = Clock.fixed(Instant.parse("2026-09-20T00:00:00Z"), ZoneOffset.UTC)
    }
}
