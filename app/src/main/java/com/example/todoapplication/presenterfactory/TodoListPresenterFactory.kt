package com.example.todoapplication.presenterfactory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.todoapplication.application.service.CategoryService
import com.example.todoapplication.application.service.TodoService
import com.example.todoapplication.feature.todo.list.TodoListPresenter
import java.time.Clock

class TodoListPresenterFactory(
    private val todoService: TodoService,
    private val categoryService: CategoryService,
    private val clock: Clock = Clock.systemDefaultZone(),
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(TodoListPresenter::class.java)) {
            "Unsupported ViewModel class: ${modelClass.name}"
        }
        @Suppress("UNCHECKED_CAST")
        return TodoListPresenter(todoService, categoryService, clock) as T
    }
}
