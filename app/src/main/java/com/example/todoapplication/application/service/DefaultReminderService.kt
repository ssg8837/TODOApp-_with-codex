package com.example.todoapplication.application.service

import com.example.todoapplication.domain.model.Reminder
import com.example.todoapplication.domain.repository.ReminderRepository
import com.example.todoapplication.domain.repository.TodoRepository
import com.example.todoapplication.domain.validation.ReminderValidationError
import com.example.todoapplication.domain.validation.ReminderValidator
import java.time.LocalDate
import java.time.LocalTime
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

/**
 * Reminder Validator와 TODO 상태를 조합해 Reminder 유스케이스를 수행한다.
 *
 * 시간이 없는 TODO의 Reminder를 거부한다. 목록 교체는 검증 후 Repository API를 한 번만
 * 호출하므로 삭제·일괄 삽입의 transaction 순서를 Service에서 재구현하지 않는다.
 */
class DefaultReminderService(
    private val reminderRepository: ReminderRepository,
    private val todoRepository: TodoRepository,
) : ReminderService {
    override suspend fun create(reminder: Reminder): ServiceResult<Reminder> = serviceCall {
        validateAndFindTodo(reminder)?.let { return@serviceCall ServiceResult.Failure(it) }
        val existing = reminderRepository.observeByTodoId(reminder.todoId).first()
        if (existing.any { it.minutesBefore == reminder.minutesBefore }) {
            return@serviceCall ServiceResult.Failure(
                ServiceError.InvalidReminder(setOf(ReminderValidationError.DUPLICATE_MINUTES_BEFORE)),
            )
        }
        ServiceResult.Success(reminderRepository.create(reminder))
    }

    override fun observeByTodoId(todoId: Long): Flow<ServiceResult<List<Reminder>>> =
        reminderRepository.observeByTodoId(todoId).asServiceResult()

    override suspend fun getById(reminderId: Long): ServiceResult<Reminder> = serviceCall {
        val reminder = reminderRepository.getById(reminderId)
            ?: return@serviceCall ServiceResult.Failure(ServiceError.ReminderNotFound)
        ServiceResult.Success(reminder)
    }

    override suspend fun replaceReminders(
        todoId: Long,
        reminders: List<Reminder>,
    ): ServiceResult<List<Reminder>> = serviceCall {
        val todo = todoRepository.getById(todoId)
            ?: return@serviceCall ServiceResult.Failure(ServiceError.TodoNotFound)
        if (todo.time == null && reminders.isNotEmpty()) {
            return@serviceCall ServiceResult.Failure(ServiceError.ReminderRequiresTodoTime)
        }
        val errors = ReminderValidator.validateAll(reminders, expectedTodoId = todoId)
        if (errors.isNotEmpty()) {
            return@serviceCall ServiceResult.Failure(
                ServiceError.InvalidReminder(errors),
            )
        }

        ServiceResult.Success(reminderRepository.replaceReminders(todoId, reminders))
    }

    override suspend fun delete(reminderId: Long): ServiceResult<Unit> = serviceCall {
        if (!reminderRepository.deleteById(reminderId)) {
            return@serviceCall ServiceResult.Failure(ServiceError.ReminderNotFound)
        }
        ServiceResult.Success(Unit)
    }

    override suspend fun deleteByTodoId(todoId: Long): ServiceResult<Int> = serviceCall {
        ServiceResult.Success(reminderRepository.deleteByTodoId(todoId))
    }

    override suspend fun getFutureAlarmRecoveryCandidates(
        currentDate: LocalDate,
        currentTime: LocalTime,
    ): ServiceResult<List<Reminder>> = serviceCall {
        ServiceResult.Success(
            reminderRepository.getFutureAlarmRecoveryCandidates(currentDate, currentTime),
        )
    }

    private suspend fun validateAndFindTodo(reminder: Reminder): ServiceError? {
        val errors = ReminderValidator.validate(reminder)
        if (errors.isNotEmpty()) return ServiceError.InvalidReminder(errors)
        val todo = todoRepository.getById(reminder.todoId) ?: return ServiceError.TodoNotFound
        return if (todo.time == null) ServiceError.ReminderRequiresTodoTime else null
    }
}
