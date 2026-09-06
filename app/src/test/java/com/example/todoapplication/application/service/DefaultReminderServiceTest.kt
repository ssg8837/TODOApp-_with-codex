package com.example.todoapplication.application.service

import com.example.todoapplication.domain.model.Reminder
import com.example.todoapplication.domain.validation.ReminderValidationError
import com.example.todoapplication.test.FakeReminderRepository
import com.example.todoapplication.test.FakeTodoRepository
import java.time.LocalTime
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class DefaultReminderServiceTest {
    @Test
    fun createValidReminderChecksTodoAndReturnsSavedReminder() = runBlocking {
        val reminderRepository = FakeReminderRepository()
        val service = DefaultReminderService(reminderRepository, FakeTodoRepository(listOf(todo())))

        val result = service.create(Reminder(0, 1, 15))

        assertEquals(ServiceResult.Success(Reminder(1, 1, 15)), result)
        assertEquals(1, reminderRepository.writeCount)
    }

    @Test
    fun reminderForUntimedTodoIsRejectedWithoutWrite() = runBlocking {
        val reminderRepository = FakeReminderRepository()
        val service = DefaultReminderService(
            reminderRepository,
            FakeTodoRepository(listOf(todo(time = null))),
        )

        assertEquals(
            ServiceResult.Failure(ServiceError.ReminderRequiresTodoTime),
            service.create(Reminder(0, 1, 15)),
        )
        assertEquals(0, reminderRepository.writeCount)
    }

    @Test
    fun invalidReminderIsRejectedBeforeRepositoryWrite() = runBlocking {
        val reminderRepository = FakeReminderRepository()
        val service = DefaultReminderService(reminderRepository, FakeTodoRepository(listOf(todo())))

        val result = service.create(Reminder(0, 1, 10))

        assertEquals(
            ServiceResult.Failure(
                ServiceError.InvalidReminder(
                    setOf(ReminderValidationError.UNSUPPORTED_MINUTES_BEFORE),
                ),
            ),
            result,
        )
        assertEquals(0, reminderRepository.writeCount)
    }

    @Test
    fun observeDeleteAndFutureCandidatesAreExposedAsServiceResults() = runBlocking {
        val existing = Reminder(1, 1, 15)
        val reminderRepository = FakeReminderRepository(listOf(existing)).apply {
            recoveryCandidates = listOf(existing)
        }
        val service = DefaultReminderService(reminderRepository, FakeTodoRepository(listOf(todo())))

        assertEquals(
            ServiceResult.Success(listOf(existing)),
            service.observeByTodoId(1).first(),
        )
        assertEquals(
            ServiceResult.Success(listOf(existing)),
            service.getFutureAlarmRecoveryCandidates(TEST_DATE, LocalTime.of(9, 0)),
        )
        assertEquals(ServiceResult.Success(Unit), service.delete(existing.id))
    }

    @Test
    fun replaceRemindersValidatesThenCallsAtomicRepositoryOperationOnce() = runBlocking {
        val reminderRepository = FakeReminderRepository(listOf(Reminder(1, 1, 15)))
        val service = DefaultReminderService(reminderRepository, FakeTodoRepository(listOf(todo())))
        val replacements = listOf(Reminder(0, 1, 15), Reminder(0, 1, 1_440))

        val result = service.replaceReminders(1, replacements)

        assertEquals(
            ServiceResult.Success(listOf(Reminder(1, 1, 15), Reminder(2, 1, 1_440))),
            result,
        )
        assertEquals(listOf("replaceReminders"), reminderRepository.calls)
        assertEquals(1, reminderRepository.replaceCallCount)
    }

    @Test
    fun invalidReplacementDoesNotDeleteOrCreateReminders() = runBlocking {
        val reminderRepository = FakeReminderRepository(listOf(Reminder(1, 1, 15)))
        val service = DefaultReminderService(reminderRepository, FakeTodoRepository(listOf(todo())))

        val result = service.replaceReminders(1, listOf(Reminder(0, 2, 15)))

        assertEquals(
            ServiceResult.Failure(
                ServiceError.InvalidReminder(setOf(ReminderValidationError.TODO_ID_MISMATCH)),
            ),
            result,
        )
        assertEquals(0, reminderRepository.writeCount)
        assertEquals(0, reminderRepository.replaceCallCount)
    }

    @Test
    fun replacementPersistenceFailureBecomesServiceError() = runBlocking {
        val reminderRepository = FakeReminderRepository(listOf(Reminder(1, 1, 15))).apply {
            failReplaceWithPersistenceError = true
        }
        val service = DefaultReminderService(reminderRepository, FakeTodoRepository(listOf(todo())))

        val result = service.replaceReminders(1, listOf(Reminder(0, 1, 1_440)))

        assertEquals(
            ServiceResult.Failure(ServiceError.PersistenceFailure),
            result,
        )
        assertEquals(listOf("replaceReminders"), reminderRepository.calls)
        assertEquals(1, reminderRepository.replaceCallCount)
    }

    @Test
    fun deleteAllRemindersReturnsDeletedCount() = runBlocking {
        val repository = FakeReminderRepository(
            listOf(Reminder(1, 1, 15), Reminder(2, 1, 1_440)),
        )
        val service = DefaultReminderService(repository, FakeTodoRepository(listOf(todo())))

        assertEquals(ServiceResult.Success(2), service.deleteByTodoId(1))
    }
}
