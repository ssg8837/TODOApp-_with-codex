package com.example.todoapplication.feature.todo.detail

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.todoapplication.app.TodoApplication
import com.example.todoapplication.presenterfactory.TodoDetailPresenterFactory

/**
 * AppContainer의 Service들로 확인 Presenter를 구성하고 Screen과 연결한다.
 * Navigation은 Route callback으로 전달하며 Composable이나 Presenter에서 NavController를 직접 참조하지 않는다.
 */
@Composable
fun TodoDetailRoute(
    todoId: Long,
    onBack: () -> Unit,
    onEdit: (Long) -> Unit,
    onConfirm: () -> Unit,
) {
    val application = LocalContext.current.applicationContext as TodoApplication
    val factory = remember(application.container, todoId) {
        TodoDetailPresenterFactory(
            todoService = application.container.todoService,
            categoryService = application.container.categoryService,
            reminderService = application.container.reminderService,
            todoId = todoId,
        )
    }
    val presenter: TodoDetailPresenter = viewModel(factory = factory)
    val state by presenter.state.collectAsStateWithLifecycle()

    TodoDetailScreen(
        state = state,
        onEvent = presenter::onEvent,
        onBack = onBack,
        onEdit = { onEdit(todoId) },
        onConfirm = onConfirm,
    )
}

