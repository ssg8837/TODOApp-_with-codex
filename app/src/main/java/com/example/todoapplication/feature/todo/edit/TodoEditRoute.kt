package com.example.todoapplication.feature.todo.edit

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.todoapplication.app.TodoApplication
import com.example.todoapplication.presenterfactory.TodoEditPresenterFactory
import java.time.LocalDate

@Composable
fun TodoEditRoute(
    mode: TodoEditMode,
    initialDate: LocalDate,
    todoId: Long? = null,
    onBack: () -> Unit,
    onSaved: () -> Unit,
) {
    val application = LocalContext.current.applicationContext as TodoApplication
    val factory = remember(application.container, mode, initialDate, todoId) {
        TodoEditPresenterFactory(
            todoService = application.container.todoService,
            categoryService = application.container.categoryService,
            mode = mode,
            initialDate = initialDate,
            todoId = todoId,
        )
    }
    val presenter: TodoEditPresenter = viewModel(factory = factory)
    val state by presenter.state.collectAsStateWithLifecycle()

    LaunchedEffect(presenter) {
        presenter.effects.collect { effect ->
            when (effect) {
                TodoEditEffect.Saved -> onSaved()
            }
        }
    }

    TodoEditScreen(
        state = state,
        onEvent = presenter::onEvent,
        onBack = onBack,
    )
}
