package com.example.todoapplication.feature.todo.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.todoapplication.application.service.CategoryService
import com.example.todoapplication.application.service.ReminderService
import com.example.todoapplication.application.service.ServiceError
import com.example.todoapplication.application.service.ServiceResult
import com.example.todoapplication.application.service.TodoService
import com.example.todoapplication.domain.model.CategoryColor
import com.example.todoapplication.domain.model.Reminder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * TODO 확인(상세) 화면의 읽기 전용 상태를 관리하는 Presenter.
 *
 * Route의 `todoId`를 기반으로 TODO, Category, Reminder의 최신 상태를 반응형 Flow로 결합하여
 * 화면 모델을 구성한다. 편집 및 화면 닫기는 Route callback으로 위임하며
 * Repository나 Room, NavController에 직접 접근하지 않는다.
 */
class TodoDetailPresenter(
    private val todoService: TodoService,
    private val categoryService: CategoryService,
    private val reminderService: ReminderService,
    private val todoId: Long,
) : ViewModel() {

    private val mutableState = MutableStateFlow(
        TodoDetailUiState(
            todoId = todoId,
            isLoading = true,
        ),
    )
    val state: StateFlow<TodoDetailUiState> = mutableState.asStateFlow()

    init {
        if (todoId <= 0L) {
            mutableState.update {
                it.copy(
                    isLoading = false,
                    error = TodoDetailError.TODO_NOT_FOUND,
                )
            }
        } else {
            observeDetail()
        }
    }

    fun onEvent(event: TodoDetailEvent) {
        when (event) {
            TodoDetailEvent.Refresh -> observeDetail()
        }
    }

    private fun observeDetail() {
        viewModelScope.launch {
            combine(
                todoService.observeById(todoId),
                categoryService.observeAll(),
                reminderService.observeByTodoId(todoId),
            ) { todoResult, categoryResult, reminderResult ->
                Triple(todoResult, categoryResult, reminderResult)
            }.collect { (todoResult, categoryResult, reminderResult) ->
                when (todoResult) {
                    is ServiceResult.Success -> {
                        val todo = todoResult.value
                        val categories = (categoryResult as? ServiceResult.Success)?.value.orEmpty()
                        val category = categories.firstOrNull { it.id == todo.categoryId }
                        val reminders = (reminderResult as? ServiceResult.Success)?.value
                            ?.map(Reminder::minutesBefore)
                            ?.sorted()
                            .orEmpty()

                        mutableState.update {
                            it.copy(
                                todoId = todo.id,
                                title = todo.title,
                                date = todo.date,
                                time = todo.time,
                                categoryName = category?.name.orEmpty(),
                                categoryColor = category?.color ?: CategoryColor.NEUTRAL,
                                isCompleted = todo.isCompleted,
                                reminders = reminders,
                                isLoading = false,
                                error = null,
                            )
                        }
                    }
                    is ServiceResult.Failure -> {
                        val error = when (todoResult.error) {
                            ServiceError.TodoNotFound -> TodoDetailError.TODO_NOT_FOUND
                            else -> TodoDetailError.PERSISTENCE_FAILURE
                        }
                        mutableState.update {
                            it.copy(
                                isLoading = false,
                                error = error,
                            )
                        }
                    }
                }
            }
        }
    }
}

