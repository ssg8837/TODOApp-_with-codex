package com.example.todoapplication.feature.todo.list

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.todoapplication.app.TodoApplication
import com.example.todoapplication.presenterfactory.TodoListPresenterFactory

/**
 * AppContainer의 Service로 목록 Presenter를 만들고 lifecycle-aware 상태를 Screen에 연결한다.
 * 화면 Navigation은 callback으로 전달하며 Repository나 DAO를 직접 조회하지 않는다.
 */
@Composable
fun TodoListRoute(
    onAddTodo: (java.time.LocalDate) -> Unit,
    onEditTodo: (Long) -> Unit,
    onManageCategories: () -> Unit,
) {
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
        onAddTodo = { onAddTodo(state.selectedDate) },
        onEditTodo = onEditTodo,
        onManageCategories = onManageCategories,
    )
}
