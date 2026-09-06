package com.example.todoapplication.application.service

import com.example.todoapplication.domain.model.Reminder
import com.example.todoapplication.domain.repository.ReminderRepository
import com.example.todoapplication.domain.repository.TodoRepository
import com.example.todoapplication.domain.validation.ReminderValidator
import java.time.LocalDate
import java.time.LocalTime
import kotlinx.coroutines.flow.Flow

class DefaultReminderService(
    private val reminderRepository: ReminderRepository,
    private val todoRepository: TodoRepository,
) : ReminderService {
    override suspend fun create(reminder: Reminder): ServiceResult<Reminder> = serviceCall {
        validateAndFindTodo(reminder)?.let { return@serviceCall ServiceResult.Failure(it) }
        ServiceResult.Success(reminderRepository.create(reminder))
    }

    override fun observeByTodoId(todoId: Long): Flow<ServiceResult<List<Reminder>>> =
        reminderRepository.observeByTodoId(todoId).asServiceResult()

    override suspend fun replaceReminders(
        todoId: Long,
        reminders: List<Reminder>,
    ): ServiceResult<List<Reminder>> = serviceCall {
        val todo = todoRepository.getById(todoId)
            ?: return@serviceCall ServiceResult.Failure(ServiceError.TodoNotFound)
        if (todo.time == null && reminders.isNotEmpty()) {
            return@serviceCall ServiceResult.Failure(ServiceError.ReminderRequiresTodoTime)
        }
        reminders.forEach { reminder ->
            val errors = ReminderValidator.validate(reminder, expectedTodoId = todoId)
            if (errors.isNotEmpty()) {
                return@serviceCall ServiceResult.Failure(
                    ServiceError.InvalidReminder(errors),
                )
            }
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
