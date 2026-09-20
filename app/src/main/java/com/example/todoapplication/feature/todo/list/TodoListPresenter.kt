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
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class TodoListPresenter(
    private val todoService: TodoService,
    private val categoryService: CategoryService,
    clock: Clock = Clock.systemDefaultZone(),
) : ViewModel() {
    private val query = MutableStateFlow(ListQuery(LocalDate.now(clock)))
    private val categoryResults = MutableStateFlow<ServiceResult<List<Category>>?>(null)
    private val mutableState = MutableStateFlow(TodoListUiState(selectedDate = query.value.date))
    val state: StateFlow<TodoListUiState> = mutableState.asStateFlow()

    init {
        viewModelScope.launch {
            categoryService.observeAll().collect { result ->
                if (result is ServiceResult.Success) {
                    val choices = result.value.map { CategoryFilterUiModel(it.id, it.name, it.color) }
                    val selected = query.value.categoryId
                    if (selected != null && choices.none { it.id == selected }) {
                        // Remove the deleted option and its selection in the same state update.
                        changeQuery(query.value.copy(categoryId = null), choices)
                    } else {
                        mutableState.update { it.copy(categories = choices) }
                    }
                }
                categoryResults.value = result
            }
        }
        viewModelScope.launch {
            query.flatMapLatest { current ->
                combine(
                    observeTodos(current),
                    categoryResults.filterNotNull(),
                ) { todos, categories -> ListResult(current, todos, categories) }
            }.collect(::applyResult)
        }
    }

    fun onEvent(event: TodoListEvent) {
        val current = query.value
        when (event) {
            TodoListEvent.PreviousDate -> changeQuery(current.copy(date = current.date.minusDays(1)))
            TodoListEvent.NextDate -> changeQuery(current.copy(date = current.date.plusDays(1)))
            is TodoListEvent.SelectDate -> changeQuery(current.copy(date = event.date))
            is TodoListEvent.SelectCategory -> {
                val id = event.categoryId?.takeIf { id -> state.value.categories.any { it.id == id } }
                changeQuery(current.copy(categoryId = id))
            }
            is TodoListEvent.SetIncompleteOnly -> changeQuery(current.copy(incompleteOnly = event.enabled))
            is TodoListEvent.SetCompleted -> setCompleted(event.todoId, event.completed)
        }
    }

    private fun changeQuery(
        next: ListQuery,
        categories: List<CategoryFilterUiModel> = state.value.categories,
    ) {
        if (next == query.value) return
        query.value = next
        mutableState.update {
            it.copy(
                selectedDate = next.date,
                selectedCategoryId = next.categoryId,
                incompleteOnly = next.incompleteOnly,
                categories = categories,
                todos = emptyList(),
                isLoading = true,
                error = null,
                emptyState = TodoListEmptyState.NO_TODOS,
            )
        }
    }

    private fun observeTodos(current: ListQuery): Flow<TodosResult> =
        todoService.observeFiltered(current.date, current.categoryId, current.incompleteOnly)
            .distinctUntilChanged()
            .flatMapLatest { result ->
                if (result is ServiceResult.Success && result.value.isEmpty() &&
                    (current.categoryId != null || current.incompleteOnly)
                ) {
                    // Only empty filtered results need an additional date-level observation.
                    todoService.observeByDate(current.date).map { dateResult ->
                        when (dateResult) {
                            is ServiceResult.Success -> TodosResult(
                                result,
                                if (dateResult.value.isEmpty()) TodoListEmptyState.NO_TODOS
                                else TodoListEmptyState.NO_MATCHES,
                            )
                            is ServiceResult.Failure -> TodosResult(dateResult)
                        }
                    }
                } else {
                    flowOf(TodosResult(result))
                }
            }

    private fun applyResult(result: ListResult) {
        if (result.query != query.value) return
        val mapped = combineTodoAndCategoryResults(result.todos.result, result.categories)
        mutableState.update {
            when (mapped) {
                is ServiceResult.Success -> it.copy(
                    todos = mapped.value,
                    isLoading = false,
                    error = null,
                    emptyState = result.todos.emptyState,
                )
                is ServiceResult.Failure -> it.copy(
                    todos = emptyList(),
                    isLoading = false,
                    error = mapped.error.toPresentationError(),
                )
            }
        }
    }

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

    private data class ListQuery(
        val date: LocalDate,
        val categoryId: Long? = null,
        val incompleteOnly: Boolean = false,
    )

    private data class TodosResult(
        val result: ServiceResult<List<Todo>>,
        val emptyState: TodoListEmptyState = TodoListEmptyState.NO_TODOS,
    )

    private data class ListResult(
        val query: ListQuery,
        val todos: TodosResult,
        val categories: ServiceResult<List<Category>>,
    )
}
