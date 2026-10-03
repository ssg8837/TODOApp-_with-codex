package com.example.todoapplication.application.service

import com.example.todoapplication.domain.reminder.ReminderCalculator
import com.example.todoapplication.notification.AlarmOperationResult
import com.example.todoapplication.notification.AlarmScheduler
import java.time.Clock
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import kotlinx.coroutines.CancellationException

/** 재부팅 이후 저장된 Reminder를 source of truth로 삼아 OS Alarm을 복구하는 계약. */
interface ReminderRecoveryService {
    /**
     * 미래 후보를 최신 상태와 다시 비교해 예약한다.
     *
     * @return 후보·예약·제외·실패 건수와 전체 결과를 포함한 복구 결과.
     */
    suspend fun restoreScheduledReminders(): ReminderRecoveryResult
}

/**
 * Room에 저장된 Reminder를 기준으로 재부팅 이후 OS Alarm을 복구한다.
 *
 * 후보마다 최신 Reminder와 TODO를 재조회해 삭제·변경된 Reminder, 삭제·완료된 TODO,
 * 시간이 없는 TODO와 이미 지난 발화 시각을 제외한다. 개별 후보의 조회/예약 실패는 다른
 * 후보를 중단시키지 않지만 Coroutine cancellation은 삼키지 않고 상위 호출자에게 전파한다.
 * 전체 후보 처리 결과는 성공·일부 실패·전체 실패로 구분한다.
 */
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

/** Alarm 복구 과정의 후보, 성공, 제외, 실패 및 inexact 예약 건수. */
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

/** 전체 후보 처리 결과를 요약하는 복구 상태. */
enum class ReminderRecoveryOutcome {
    SUCCESS,
    PARTIAL_FAILURE,
    FAILURE,
}
