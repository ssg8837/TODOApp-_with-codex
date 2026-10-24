package com.example.todoapplication.presenterfactory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.todoapplication.application.service.CategoryService
import com.example.todoapplication.application.service.ReminderService
import com.example.todoapplication.application.service.TodoService
import com.example.todoapplication.feature.todo.detail.TodoDetailPresenter

/** AppContainer의 Service들로 TODO 확인 Presenter를 생성하는 수동 DI 경계. */
class TodoDetailPresenterFactory(
    private val todoService: TodoService,
    private val categoryService: CategoryService,
    private val reminderService: ReminderService,
    private val todoId: Long,
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(TodoDetailPresenter::class.java)) {
            "Unsupported ViewModel class: ${modelClass.name}"
        }
        @Suppress("UNCHECKED_CAST")
        return TodoDetailPresenter(
            todoService = todoService,
            categoryService = categoryService,
            reminderService = reminderService,
            todoId = todoId,
        ) as T
    }
}

