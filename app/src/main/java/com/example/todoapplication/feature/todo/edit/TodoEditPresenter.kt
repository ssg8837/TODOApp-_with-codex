package com.example.todoapplication.feature.todo.edit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.todoapplication.application.service.CategoryService
import com.example.todoapplication.application.service.ServiceError
import com.example.todoapplication.application.service.ServiceResult
import com.example.todoapplication.application.service.TodoService
import com.example.todoapplication.domain.model.Category
import com.example.todoapplication.domain.model.Todo
import com.example.todoapplication.domain.model.Reminder
import com.example.todoapplication.domain.validation.TodoValidationError
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class TodoEditPresenter(
    private val todoService: TodoService,
    private val categoryService: CategoryService,
    mode: TodoEditMode,
    initialDate: LocalDate,
    todoId: Long? = null,
    private val clock: Clock = Clock.systemDefaultZone(),
) : ViewModel() {
    private val mutableState = MutableStateFlow(
        TodoEditUiState(
            mode = mode,
            todoId = todoId,
            date = initialDate,
        ),
    )
    private val effectChannel = Channel<TodoEditEffect>(Channel.BUFFERED)
    private var categoriesLoaded = false
    private var todoLoaded = mode == TodoEditMode.CREATE
    private var remindersLoaded = mode == TodoEditMode.CREATE
    private var originalTodo: Todo? = null

    val state: StateFlow<TodoEditUiState> = mutableState.asStateFlow()
    val effects: Flow<TodoEditEffect> = effectChannel.receiveAsFlow()

    init {
        observeCategories()
        if (mode == TodoEditMode.EDIT) {
            if (todoId == null || todoId <= 0) {
                mutableState.update { it.copy(isLoading = false, error = TodoEditError.TODO_NOT_FOUND) }
            } else {
                loadTodo(todoId)
            }
        }
    }

    fun onEvent(event: TodoEditEvent) {
        when (event) {
            is TodoEditEvent.SetOneDayReminder -> setReminder(Reminder.ONE_DAY_BEFORE, event.enabled)
            is TodoEditEvent.SetFifteenMinuteReminder ->
                setReminder(Reminder.FIFTEEN_MINUTES_BEFORE, event.enabled)
            is TodoEditEvent.NotificationPermissionResult -> mutableState.update {
                it.copy(notificationPermissionDenied = !event.granted)
            }
            is TodoEditEvent.TitleChanged -> mutableState.update {
                it.copy(
                    title = event.title,
                    validationErrors = it.validationErrors - TodoEditValidationError.TITLE_REQUIRED,
                    error = null,
                )
            }
            is TodoEditEvent.DateChanged -> mutableState.update { it.copy(date = event.date) }
            is TodoEditEvent.TimeChanged -> mutableState.update { it.copy(time = event.time) }
            TodoEditEvent.TimeCleared -> mutableState.update {
                it.copy(
                    time = null,
                    remindOneDayBefore = false,
                    remindFifteenMinutesBefore = false,
                )
            }
            is TodoEditEvent.CategoryChanged -> mutableState.update {
                it.copy(
                    selectedCategoryId = event.categoryId,
                    validationErrors = it.validationErrors - TodoEditValidationError.CATEGORY_REQUIRED,
                    error = null,
                )
            }
            TodoEditEvent.Save -> save()
            TodoEditEvent.RequestDelete -> requestDelete()
            TodoEditEvent.ConfirmDelete -> confirmDelete()
            TodoEditEvent.CancelDelete -> cancelDelete()
        }
    }

    private fun observeCategories() {
        viewModelScope.launch {
            categoryService.observeAll().collect { result ->
                when (result) {
                    is ServiceResult.Success -> applyCategories(result.value)
                    is ServiceResult.Failure -> mutableState.update {
                        it.copy(
                            isLoading = false,
                            error = result.error.toEditError(),
                        )
                    }
                }
            }
        }
    }

    private fun applyCategories(categories: List<Category>) {
        val current = mutableState.value
        val selectedId = when {
            current.selectedCategoryId != null -> current.selectedCategoryId
            current.mode == TodoEditMode.CREATE -> categories.firstOrNull(Category::isSystem)?.id
            else -> null
        }
        categoriesLoaded = true
        mutableState.update {
            it.copy(
                categories = categories.map { category ->
                    TodoEditCategoryUiModel(category.id, category.name, category.color)
                },
                selectedCategoryId = selectedId,
                isLoading = !(categoriesLoaded && todoLoaded && remindersLoaded),
                error = when {
                    selectedId == null && it.mode == TodoEditMode.CREATE -> {
                        TodoEditError.CATEGORY_NOT_FOUND
                    }
                    it.error == TodoEditError.CATEGORY_NOT_FOUND -> null
                    else -> it.error
                },
            )
        }
    }

    private fun loadTodo(todoId: Long) {
        viewModelScope.launch {
            when (val result = todoService.getById(todoId)) {
                is ServiceResult.Success -> {
                    originalTodo = result.value
                    todoLoaded = true
                    mutableState.update {
                        it.copy(
                            todoId = result.value.id,
                            title = result.value.title,
                            date = result.value.date,
                            time = result.value.time,
                            selectedCategoryId = result.value.categoryId,
                            isLoading = !(categoriesLoaded && todoLoaded && remindersLoaded),
                            error = null,
                        )
                    }
                    loadReminders(todoId)
                }
                is ServiceResult.Failure -> mutableState.update {
                    it.copy(isLoading = false, error = result.error.toEditError())
                }
            }
        }
    }

    private fun loadReminders(todoId: Long) {
        viewModelScope.launch {
            todoService.observeReminders(todoId).collect { result ->
                when (result) {
                    is ServiceResult.Success -> {
                        remindersLoaded = true
                        val minutes = result.value.map(Reminder::minutesBefore).toSet()
                        mutableState.update {
                            it.copy(
                                remindOneDayBefore = Reminder.ONE_DAY_BEFORE in minutes,
                                remindFifteenMinutesBefore = Reminder.FIFTEEN_MINUTES_BEFORE in minutes,
                                isLoading = !(categoriesLoaded && todoLoaded && remindersLoaded),
                            )
                        }
                    }
                    is ServiceResult.Failure -> mutableState.update {
                        it.copy(isLoading = false, error = result.error.toEditError())
                    }
                }
            }
        }
    }

    private fun setReminder(minutesBefore: Int, enabled: Boolean) {
        if (mutableState.value.time == null || mutableState.value.isSaving) return
        mutableState.update {
            when (minutesBefore) {
                Reminder.ONE_DAY_BEFORE -> it.copy(remindOneDayBefore = enabled)
                Reminder.FIFTEEN_MINUTES_BEFORE -> it.copy(remindFifteenMinutesBefore = enabled)
                else -> it
            }
        }
    }

    private fun save() {
        val current = mutableState.value
        if (current.isLoading || current.isSaving || current.isDeleting) return
        if (current.mode == TodoEditMode.EDIT && originalTodo == null) {
            mutableState.update { it.copy(error = TodoEditError.TODO_NOT_FOUND) }
            return
        }
        val inputErrors = buildSet {
            if (current.title.isBlank()) add(TodoEditValidationError.TITLE_REQUIRED)
            if (current.selectedCategoryId == null) add(TodoEditValidationError.CATEGORY_REQUIRED)
        }
        if (inputErrors.isNotEmpty()) {
            mutableState.update { it.copy(validationErrors = inputErrors) }
            return
        }
        val categoryId = current.selectedCategoryId ?: return
        mutableState.update { it.copy(isSaving = true, validationErrors = emptySet(), error = null) }
        viewModelScope.launch {
            val now = Instant.now(clock)
            val existing = originalTodo
            val todo = if (current.mode == TodoEditMode.EDIT && existing != null) {
                existing.copy(
                    title = current.title,
                    date = current.date,
                    time = current.time,
                    categoryId = categoryId,
                    updatedAt = now,
                )
            } else {
                Todo(
                    id = 0,
                    title = current.title,
                    date = current.date,
                    time = current.time,
                    categoryId = categoryId,
                    isCompleted = false,
                    createdAt = now,
                    updatedAt = now,
                )
            }
            val reminderMinutes = buildSet {
                if (current.remindOneDayBefore) add(Reminder.ONE_DAY_BEFORE)
                if (current.remindFifteenMinutesBefore) add(Reminder.FIFTEEN_MINUTES_BEFORE)
            }
            val result = todoService.saveWithReminders(todo, reminderMinutes)
            applySaveResult(result)
        }
    }

    private suspend fun applySaveResult(result: ServiceResult<com.example.todoapplication.application.service.TodoSaveResult>) {
        when (result) {
            is ServiceResult.Success -> {
                originalTodo = result.value.todo
                when (result.value.alarmSyncStatus) {
                    com.example.todoapplication.application.service.AlarmSyncStatus.SYNCHRONIZED -> {
                        mutableState.update { it.copy(isSaving = false) }
                        effectChannel.send(TodoEditEffect.Saved)
                    }
                    com.example.todoapplication.application.service.AlarmSyncStatus.INEXACT_SCHEDULED ->
                        mutableState.update {
                            it.copy(isSaving = false, error = TodoEditError.INEXACT_ALARM_SCHEDULED)
                        }
                    com.example.todoapplication.application.service.AlarmSyncStatus.NOTIFICATION_PERMISSION_DENIED ->
                        mutableState.update {
                            it.copy(
                                isSaving = false,
                                error = TodoEditError.NOTIFICATION_PERMISSION_DENIED,
                            )
                        }
                    com.example.todoapplication.application.service.AlarmSyncStatus.FAILED ->
                        mutableState.update {
                            it.copy(isSaving = false, error = TodoEditError.ALARM_NOT_SCHEDULED)
                        }
                }
            }
            is ServiceResult.Failure -> mutableState.update {
                it.copy(
                    isSaving = false,
                    validationErrors = result.error.validationErrors(),
                    error = result.error.toEditError(),
                )
            }
        }
    }

    private fun requestDelete() {
        val current = mutableState.value
        val canDelete = current.mode == TodoEditMode.EDIT &&
            current.todoId != null &&
            current.todoId > 0 &&
            originalTodo != null &&
            !current.isLoading &&
            !current.isSaving &&
            !current.isDeleting
        if (!canDelete) return
        mutableState.update { it.copy(showDeleteConfirmation = true, error = null) }
    }

    private fun cancelDelete() {
        if (mutableState.value.isDeleting) return
        mutableState.update { it.copy(showDeleteConfirmation = false) }
    }

    private fun confirmDelete() {
        val current = mutableState.value
        val todoId = current.todoId ?: return
        val canDelete = current.mode == TodoEditMode.EDIT &&
            todoId > 0 &&
            originalTodo != null &&
            current.showDeleteConfirmation &&
            !current.isLoading &&
            !current.isSaving &&
            !current.isDeleting
        if (!canDelete) return
        mutableState.update { it.copy(isDeleting = true, error = null) }
        viewModelScope.launch {
            when (val result = todoService.delete(todoId)) {
                is ServiceResult.Success -> {
                    mutableState.update {
                        it.copy(isDeleting = false, showDeleteConfirmation = false)
                    }
                    effectChannel.send(TodoEditEffect.Deleted)
                }
                is ServiceResult.Failure -> mutableState.update {
                    it.copy(
                        isDeleting = false,
                        showDeleteConfirmation = false,
                        error = result.error.toEditError(),
                    )
                }
            }
        }
    }

    private fun ServiceError.validationErrors(): Set<TodoEditValidationError> =
        if (this is ServiceError.InvalidTodo) {
            errors.mapNotNull { error ->
                when (error) {
                    TodoValidationError.BLANK_TITLE -> TodoEditValidationError.TITLE_REQUIRED
                    TodoValidationError.INVALID_CATEGORY_ID -> TodoEditValidationError.CATEGORY_REQUIRED
                }
            }.toSet()
        } else {
            emptySet()
        }

    private fun ServiceError.toEditError(): TodoEditError = when (this) {
        ServiceError.TodoNotFound -> TodoEditError.TODO_NOT_FOUND
        ServiceError.CategoryNotFound -> TodoEditError.CATEGORY_NOT_FOUND
        is ServiceError.InvalidTodo -> TodoEditError.INVALID_TODO
        ServiceError.PersistenceFailure -> TodoEditError.PERSISTENCE_FAILURE
        else -> TodoEditError.OPERATION_FAILED
    }
}
