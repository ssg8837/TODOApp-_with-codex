package com.example.todoapplication.feature.todo.list

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.todoapplication.app.TodoApplication
import com.example.todoapplication.presenterfactory.TodoListPresenterFactory

@Composable
fun TodoListRoute() {
    val application = LocalContext.current.applicationContext as TodoApplication
    val factory = remember(application.container) {
        TodoListPresenterFactory(
            todoService = application.container.todoService,
            categoryService = application.container.categoryService,
        )
    }
    val presenter: TodoListPresenter = viewModel(factory = factory)
    val state by presenter.state.collectAsStateWithLifecycle()

    TodoListScreen(
        state = state,
        onEvent = presenter::onEvent,
    )
}
