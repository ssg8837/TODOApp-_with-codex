package com.example.todoapplication.feature.todo.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.todoapplication.application.service.CategoryService
import com.example.todoapplication.application.service.ServiceError
import com.example.todoapplication.application.service.ServiceResult
import com.example.todoapplication.application.service.TodoService
import com.example.todoapplication.domain.model.Category
import com.example.todoapplication.domain.model.Todo
import java.time.Clock
import java.time.LocalDate
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class TodoListPresenter(
    private val todoService: TodoService,
    private val categoryService: CategoryService,
    clock: Clock = Clock.systemDefaultZone(),
) : ViewModel() {
    private val selectedDate = MutableStateFlow(LocalDate.now(clock))
    private val mutableState = MutableStateFlow(
        TodoListUiState(selectedDate = selectedDate.value),
    )

    val state: StateFlow<TodoListUiState> = mutableState.asStateFlow()

    init {
        viewModelScope.launch {
            selectedDate
                .flatMapLatest(::observeDate)
                .collect(::applyLoadState)
        }
    }

    fun onEvent(event: TodoListEvent) {
        when (event) {
            TodoListEvent.PreviousDate -> selectDate(selectedDate.value.minusDays(1))
            TodoListEvent.NextDate -> selectDate(selectedDate.value.plusDays(1))
            is TodoListEvent.SelectDate -> selectDate(event.date)
            is TodoListEvent.SetCompleted -> setCompleted(event.todoId, event.completed)
        }
    }

    private fun selectDate(date: LocalDate) {
        if (selectedDate.value == date) return
        mutableState.update {
            it.copy(
                selectedDate = date,
                todos = emptyList(),
                isLoading = true,
                error = null,
            )
        }
        selectedDate.value = date
    }

    private fun observeDate(date: LocalDate): Flow<ListLoadState> =
        combine(
            todoService.observeByDate(date),
            categoryService.observeAll(),
            ::combineTodoAndCategoryResults,
        )
            .map<ServiceResult<List<TodoListItemUiModel>>, ListLoadState> { result ->
                ListLoadState.Result(date, result)
            }
            .onStart { emit(ListLoadState.Loading(date)) }

    private fun combineTodoAndCategoryResults(
        todoResult: ServiceResult<List<Todo>>,
        categoryResult: ServiceResult<List<Category>>,
    ): ServiceResult<List<TodoListItemUiModel>> {
        if (todoResult is ServiceResult.Failure) return todoResult
        if (categoryResult is ServiceResult.Failure) return categoryResult

        val todos = (todoResult as ServiceResult.Success).value
        val categoriesById = (categoryResult as ServiceResult.Success).value.associateBy { it.id }
        val items = todos.map { todo ->
            val category = categoriesById[todo.categoryId]
                ?: return ServiceResult.Failure(ServiceError.CategoryNotFound)
            TodoListItemUiModel(
                id = todo.id,
                title = todo.title,
                time = todo.time,
                isCompleted = todo.isCompleted,
                categoryName = category.name,
                categoryColor = category.color,
            )
        }
        return ServiceResult.Success(items)
    }

    private fun applyLoadState(loadState: ListLoadState) {
        if (loadState.date != selectedDate.value) return
        when (loadState) {
            is ListLoadState.Loading -> mutableState.update {
                it.copy(
                    selectedDate = loadState.date,
                    todos = emptyList(),
                    isLoading = true,
                    error = null,
                )
            }
            is ListLoadState.Result -> applyListResult(loadState.date, loadState.result)
        }
    }

    private fun applyListResult(
        date: LocalDate,
        result: ServiceResult<List<TodoListItemUiModel>>,
    ) {
        mutableState.update { current ->
            when (result) {
                is ServiceResult.Success -> current.copy(
                    selectedDate = date,
                    todos = result.value,
                    isLoading = false,
                    error = null,
                )
                is ServiceResult.Failure -> current.copy(
                    selectedDate = date,
                    todos = emptyList(),
                    isLoading = false,
                    error = result.error.toPresentationError(),
                )
            }
        }
    }

    private fun setCompleted(todoId: Long, completed: Boolean) {
        viewModelScope.launch {
            when (val result = todoService.setCompleted(todoId, completed)) {
                is ServiceResult.Success -> mutableState.update { it.copy(error = null) }
                is ServiceResult.Failure -> mutableState.update {
                    it.copy(error = result.error.toPresentationError())
                }
            }
        }
    }

    private fun ServiceError.toPresentationError(): TodoListError = when (this) {
        ServiceError.TodoNotFound -> TodoListError.TODO_NOT_FOUND
        ServiceError.CategoryNotFound -> TodoListError.CATEGORY_NOT_FOUND
        is ServiceError.InvalidTodo -> TodoListError.INVALID_TODO
        ServiceError.PersistenceFailure -> TodoListError.PERSISTENCE_FAILURE
        else -> TodoListError.OPERATION_FAILED
    }

    private sealed interface ListLoadState {
        val date: LocalDate

        data class Loading(override val date: LocalDate) : ListLoadState

        data class Result(
            override val date: LocalDate,
            val result: ServiceResult<List<TodoListItemUiModel>>,
        ) : ListLoadState
    }
}
