package com.example.todoapplication.notification

import com.example.todoapplication.application.service.ReminderRecoveryOutcome
import com.example.todoapplication.application.service.ReminderRecoveryResult
import com.example.todoapplication.application.service.ReminderRecoveryService
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class BootReceiverTest {
    @Test
    fun recoveryLauncherInvokesServiceAndAlwaysFinishes() = runTest {
        var callCount = 0
        var finished = false
        val service = object : ReminderRecoveryService {
            override suspend fun restoreScheduledReminders(): ReminderRecoveryResult {
                callCount++
                return successResult()
            }
        }

        launchRecovery(service, StandardTestDispatcher(testScheduler)) { finished = true }
        advanceUntilIdle()

        assertEquals(1, callCount)
        assertTrue(finished)
    }

    @Test
    fun recoveryLauncherFinishesWhenServiceThrows() = runTest {
        var finished = false
        val service = object : ReminderRecoveryService {
            override suspend fun restoreScheduledReminders(): ReminderRecoveryResult {
                error("failure")
            }
        }

        launchRecovery(service, StandardTestDispatcher(testScheduler)) { finished = true }
        advanceUntilIdle()

        assertTrue(finished)
    }

    @Test
    fun recoveryFinishesAndPropagatesCancellation() = runTest {
        var finished = false
        val service = object : ReminderRecoveryService {
            override suspend fun restoreScheduledReminders(): ReminderRecoveryResult {
                throw CancellationException("cancelled")
            }
        }

        try {
            restoreAndFinish(service) { finished = true }
            fail("CancellationException must be propagated")
        } catch (_: CancellationException) {
            // Expected: cancellation is control flow, not a recoverable boot failure.
        }

        assertTrue(finished)
    }

    private fun successResult() = ReminderRecoveryResult(
        candidateCount = 0,
        scheduledCount = 0,
        skippedCount = 0,
        failedCount = 0,
        inexactCount = 0,
        outcome = ReminderRecoveryOutcome.SUCCESS,
    )
}
