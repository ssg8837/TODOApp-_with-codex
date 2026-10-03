package com.example.todoapplication.application.service

import com.example.todoapplication.domain.reminder.ReminderCalculator
import com.example.todoapplication.notification.AlarmOperationResult
import com.example.todoapplication.notification.AlarmScheduler
import java.time.Clock
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import kotlinx.coroutines.CancellationException

interface ReminderRecoveryService {
    suspend fun restoreScheduledReminders(): ReminderRecoveryResult
}

class DefaultReminderRecoveryService(
    private val reminderService: ReminderService,
    private val todoService: TodoService,
    private val alarmScheduler: AlarmScheduler,
    private val clock: Clock = Clock.systemDefaultZone(),
    private val zoneId: ZoneId = ZoneId.systemDefault(),
) : ReminderRecoveryService {
    override suspend fun restoreScheduledReminders(): ReminderRecoveryResult {
        val now = clock.instant()
        val localNow = now.atZone(zoneId)
        val candidates = when (
            val result = reminderService.getFutureAlarmRecoveryCandidates(
                currentDate = localNow.toLocalDate(),
                currentTime = localNow.toLocalTime(),
            )
        ) {
            is ServiceResult.Success -> result.value
            is ServiceResult.Failure -> return ReminderRecoveryResult.lookupFailure()
        }

        var scheduledCount = 0
        var skippedCount = 0
        var failedCount = 0
        var inexactCount = 0
        candidates.forEach { candidate ->
            try {
                val reminder = when (val result = reminderService.getById(candidate.id)) {
                    is ServiceResult.Success -> result.value
                    is ServiceResult.Failure -> {
                        skippedCount++
                        return@forEach
                    }
                }
                if (reminder != candidate) {
                    skippedCount++
                    return@forEach
                }
                val todo = when (val result = todoService.getById(reminder.todoId)) {
                    is ServiceResult.Success -> result.value
                    is ServiceResult.Failure -> {
                        skippedCount++
                        return@forEach
                    }
                }
                val scheduledAt = ReminderCalculator.scheduledAt(todo, reminder, zoneId)
                if (todo.isCompleted || todo.time == null || scheduledAt?.isAfter(now) != true) {
                    skippedCount++
                    return@forEach
                }
                when (alarmScheduler.schedule(todo, reminder)) {
                    AlarmOperationResult.SUCCESS -> scheduledCount++
                    AlarmOperationResult.INEXACT_SCHEDULED -> {
                        scheduledCount++
                        inexactCount++
                    }
                    AlarmOperationResult.SKIPPED_PAST -> skippedCount++
                    AlarmOperationResult.NOTIFICATION_PERMISSION_DENIED,
                    AlarmOperationResult.FAILURE,
                    -> failedCount++
                }
            } catch (error: CancellationException) {
                throw error
            } catch (_: RuntimeException) {
                failedCount++
            }
        }
        return ReminderRecoveryResult(
            candidateCount = candidates.size,
            scheduledCount = scheduledCount,
            skippedCount = skippedCount,
            failedCount = failedCount,
            inexactCount = inexactCount,
            outcome = when {
                failedCount == 0 -> ReminderRecoveryOutcome.SUCCESS
                scheduledCount > 0 || skippedCount > 0 -> ReminderRecoveryOutcome.PARTIAL_FAILURE
                else -> ReminderRecoveryOutcome.FAILURE
            },
        )
    }
}

data class ReminderRecoveryResult(
    val candidateCount: Int,
    val scheduledCount: Int,
    val skippedCount: Int,
    val failedCount: Int,
    val inexactCount: Int,
    val outcome: ReminderRecoveryOutcome,
) {
    companion object {
        fun lookupFailure() = ReminderRecoveryResult(
            candidateCount = 0,
            scheduledCount = 0,
            skippedCount = 0,
            failedCount = 1,
            inexactCount = 0,
            outcome = ReminderRecoveryOutcome.FAILURE,
        )
    }
}

enum class ReminderRecoveryOutcome {
    SUCCESS,
    PARTIAL_FAILURE,
    FAILURE,
}
