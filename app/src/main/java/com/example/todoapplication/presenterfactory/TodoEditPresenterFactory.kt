package com.example.todoapplication.presenterfactory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.todoapplication.application.service.CategoryService
import com.example.todoapplication.application.service.TodoService
import com.example.todoapplication.feature.todo.edit.TodoEditMode
import com.example.todoapplication.feature.todo.edit.TodoEditPresenter
import java.time.Clock
import java.time.LocalDate

class TodoEditPresenterFactory(
    private val todoService: TodoService,
    private val categoryService: CategoryService,
    private val mode: TodoEditMode,
    private val initialDate: LocalDate,
    private val todoId: Long? = null,
    private val clock: Clock = Clock.systemDefaultZone(),
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(TodoEditPresenter::class.java)) {
            "Unsupported ViewModel class: ${modelClass.name}"
        }
        @Suppress("UNCHECKED_CAST")
        return TodoEditPresenter(
            todoService = todoService,
            categoryService = categoryService,
            mode = mode,
            initialDate = initialDate,
            todoId = todoId,
            clock = clock,
        ) as T
    }
}
