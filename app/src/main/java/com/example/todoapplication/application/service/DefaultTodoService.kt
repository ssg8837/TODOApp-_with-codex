package com.example.todoapplication.application.service

import com.example.todoapplication.domain.model.Todo
import com.example.todoapplication.domain.model.Reminder
import com.example.todoapplication.domain.repository.CategoryRepository
import com.example.todoapplication.domain.repository.TodoRepository
import com.example.todoapplication.domain.repository.ReminderRepository
import com.example.todoapplication.notification.AlarmOperationResult
import com.example.todoapplication.notification.AlarmScheduler
import com.example.todoapplication.domain.validation.TodoValidator
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

/**
 * TODO validation과 Category 유효성, Reminder 저장 및 Alarm 동기화를 조정한다.
 *
 * Repository를 통해 Domain 데이터를 다루며 Room transaction 내부 절차를 알지 않는다.
 * Alarm 예약 실패는 저장을 rollback하지 않고 [TodoSaveResult]의 상태로 보고한다.
 */
class DefaultTodoService(
    private val todoRepository: TodoRepository,
    private val categoryRepository: CategoryRepository,
    private val reminderRepository: ReminderRepository? = null,
    private val alarmScheduler: AlarmScheduler? = null,
) : TodoService {
    override suspend fun create(todo: Todo): ServiceResult<Todo> = serviceCall {
        val errors = TodoValidator.validate(todo)
        if (errors.isNotEmpty()) {
            return@serviceCall ServiceResult.Failure(ServiceError.InvalidTodo(errors))
        }
        if (categoryRepository.getById(todo.categoryId) == null) {
            return@serviceCall ServiceResult.Failure(ServiceError.CategoryNotFound)
        }
        ServiceResult.Success(todoRepository.create(todo))
    }

    override suspend fun update(todo: Todo): ServiceResult<Todo> = serviceCall {
        val errors = TodoValidator.validate(todo)
        if (errors.isNotEmpty()) {
            return@serviceCall ServiceResult.Failure(ServiceError.InvalidTodo(errors))
        }
        if (todoRepository.getById(todo.id) == null) {
            return@serviceCall ServiceResult.Failure(ServiceError.TodoNotFound)
        }
        if (categoryRepository.getById(todo.categoryId) == null) {
            return@serviceCall ServiceResult.Failure(ServiceError.CategoryNotFound)
        }
        if (!todoRepository.update(todo)) {
            return@serviceCall ServiceResult.Failure(ServiceError.TodoNotFound)
        }
        ServiceResult.Success(todo)
    }

    override suspend fun saveWithReminders(
        todo: Todo,
        reminderMinutesBefore: Set<Int>,
    ): ServiceResult<TodoSaveResult> = serviceCall {
        val reminderStore = reminderRepository
            ?: return@serviceCall ServiceResult.Failure(ServiceError.PersistenceFailure)
        val errors = TodoValidator.validate(todo)
        if (errors.isNotEmpty()) {
            return@serviceCall ServiceResult.Failure(ServiceError.InvalidTodo(errors))
        }
        if (categoryRepository.getById(todo.categoryId) == null) {
            return@serviceCall ServiceResult.Failure(ServiceError.CategoryNotFound)
        }
        if (todo.time == null && reminderMinutesBefore.isNotEmpty()) {
            return@serviceCall ServiceResult.Failure(ServiceError.ReminderRequiresTodoTime)
        }
        if (!Reminder.SUPPORTED_MINUTES_BEFORE.containsAll(reminderMinutesBefore)) {
            return@serviceCall ServiceResult.Failure(ServiceError.InvalidReminder(emptySet()))
        }

        val oldReminders = if (todo.id > 0) reminderStore.observeByTodoId(todo.id).first() else emptyList()
        val savedTodo = if (todo.id == 0L) {
            todoRepository.create(todo)
        } else {
            if (todoRepository.getById(todo.id) == null) {
                return@serviceCall ServiceResult.Failure(ServiceError.TodoNotFound)
            }
            if (!todoRepository.update(todo)) {
                return@serviceCall ServiceResult.Failure(ServiceError.TodoNotFound)
            }
            todo
        }
        val requested = reminderMinutesBefore.sorted().map { minutes ->
            Reminder(id = 0, todoId = savedTodo.id, minutesBefore = minutes)
        }
        val savedReminders = reminderStore.replaceReminders(savedTodo.id, requested)
        val status = synchronize(savedTodo, oldReminders, savedReminders)
        ServiceResult.Success(TodoSaveResult(savedTodo, status))
    }

    override fun observeReminders(todoId: Long): Flow<ServiceResult<List<Reminder>>> =
        requireNotNull(reminderRepository) { "Reminder repository is not configured" }
            .observeByTodoId(todoId)
            .asServiceResult()

    override suspend fun resynchronizeAlarms(todoId: Long): ServiceResult<AlarmSyncStatus> = serviceCall {
        val reminderStore = reminderRepository
            ?: return@serviceCall ServiceResult.Failure(ServiceError.PersistenceFailure)
        val todo = todoRepository.getById(todoId)
            ?: return@serviceCall ServiceResult.Failure(ServiceError.TodoNotFound)
        val reminders = reminderStore.observeByTodoId(todoId).first()
        ServiceResult.Success(synchronize(todo, reminders, reminders))
    }

    override suspend fun delete(todoId: Long): ServiceResult<Unit> = serviceCall {
        val todo = todoRepository.getById(todoId)
            ?: return@serviceCall ServiceResult.Failure(ServiceError.TodoNotFound)
        val reminders = reminderRepository?.observeByTodoId(todoId)?.first().orEmpty()
        if (!todoRepository.delete(todo)) {
            return@serviceCall ServiceResult.Failure(ServiceError.TodoNotFound)
        }
        reminders.forEach { alarmScheduler?.cancel(it) }
        ServiceResult.Success(Unit)
    }

    override suspend fun setCompleted(
        todoId: Long,
        completed: Boolean,
    ): ServiceResult<Todo> = serviceCall {
        val todo = todoRepository.getById(todoId)
            ?: return@serviceCall ServiceResult.Failure(ServiceError.TodoNotFound)
        val updated = todo.copy(isCompleted = completed)
        if (!todoRepository.update(updated)) {
            return@serviceCall ServiceResult.Failure(ServiceError.TodoNotFound)
        }
        val reminders = reminderRepository?.observeByTodoId(todoId)?.first().orEmpty()
        if (completed) {
            reminders.forEach { alarmScheduler?.cancel(it) }
        } else {
            synchronize(updated, reminders, reminders)
        }
        ServiceResult.Success(updated)
    }

    override suspend fun getById(todoId: Long): ServiceResult<Todo> = serviceCall {
        val todo = todoRepository.getById(todoId)
            ?: return@serviceCall ServiceResult.Failure(ServiceError.TodoNotFound)
        ServiceResult.Success(todo)
    }

    override fun observeByDate(date: LocalDate): Flow<ServiceResult<List<Todo>>> =
        todoRepository.observeByDate(date).asServiceResult()

    override fun observeFiltered(
        date: LocalDate,
        categoryId: Long?,
        incompleteOnly: Boolean,
    ): Flow<ServiceResult<List<Todo>>> = todoRepository.observeFiltered(
        date = date,
        categoryId = categoryId,
        incompleteOnly = incompleteOnly,
    ).asServiceResult()

    private fun synchronize(
        todo: Todo,
        oldReminders: List<Reminder>,
        newReminders: List<Reminder>,
    ): AlarmSyncStatus {
        val scheduler = alarmScheduler ?: return AlarmSyncStatus.SYNCHRONIZED
        var status = AlarmSyncStatus.SYNCHRONIZED
        oldReminders.forEach { reminder ->
            if (scheduler.cancel(reminder) == AlarmOperationResult.FAILURE) {
                status = AlarmSyncStatus.FAILED
            }
        }
        if (!todo.isCompleted) {
            newReminders.forEach { reminder ->
                when (scheduler.schedule(todo, reminder)) {
                    AlarmOperationResult.INEXACT_SCHEDULED ->
                        if (status == AlarmSyncStatus.SYNCHRONIZED) {
                            status = AlarmSyncStatus.INEXACT_SCHEDULED
                        }
                    AlarmOperationResult.NOTIFICATION_PERMISSION_DENIED ->
                        if (status != AlarmSyncStatus.FAILED) {
                            status = AlarmSyncStatus.NOTIFICATION_PERMISSION_DENIED
                        }
                    AlarmOperationResult.FAILURE -> status = AlarmSyncStatus.FAILED
                    AlarmOperationResult.SUCCESS,
                    AlarmOperationResult.SKIPPED_PAST,
                    -> Unit
                }
            }
        }
        return status
    }
}
