package com.example.todoapplication.feature.todo.edit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.todoapplication.application.service.CategoryService
import com.example.todoapplication.application.service.ServiceError
import com.example.todoapplication.application.service.ServiceResult
import com.example.todoapplication.application.service.TodoService
import com.example.todoapplication.domain.model.Category
import com.example.todoapplication.domain.model.Todo
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
            is TodoEditEvent.TitleChanged -> mutableState.update {
                it.copy(
                    title = event.title,
                    validationErrors = it.validationErrors - TodoEditValidationError.TITLE_REQUIRED,
                    error = null,
                )
            }
            is TodoEditEvent.DateChanged -> mutableState.update { it.copy(date = event.date) }
            is TodoEditEvent.TimeChanged -> mutableState.update { it.copy(time = event.time) }
            TodoEditEvent.TimeCleared -> mutableState.update { it.copy(time = null) }
            is TodoEditEvent.CategoryChanged -> mutableState.update {
                it.copy(
                    selectedCategoryId = event.categoryId,
                    validationErrors = it.validationErrors - TodoEditValidationError.CATEGORY_REQUIRED,
                    error = null,
                )
            }
            TodoEditEvent.Save -> save()
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
                isLoading = !(categoriesLoaded && todoLoaded),
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
                            isLoading = !(categoriesLoaded && todoLoaded),
                            error = null,
                        )
                    }
                }
                is ServiceResult.Failure -> mutableState.update {
                    it.copy(isLoading = false, error = result.error.toEditError())
                }
            }
        }
    }

    private fun save() {
        val current = mutableState.value
        if (current.isLoading || current.isSaving) return
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
            val result = if (current.mode == TodoEditMode.CREATE) {
                todoService.create(todo)
            } else {
                todoService.update(todo)
            }
            applySaveResult(result)
        }
    }

    private suspend fun applySaveResult(result: ServiceResult<Todo>) {
        when (result) {
            is ServiceResult.Success -> {
                mutableState.update { it.copy(isSaving = false) }
                effectChannel.send(TodoEditEffect.Saved)
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
