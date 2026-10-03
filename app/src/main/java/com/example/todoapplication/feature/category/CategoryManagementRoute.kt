package com.example.todoapplication.feature.category

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.todoapplication.app.TodoApplication
import com.example.todoapplication.presenterfactory.CategoryManagementPresenterFactory

/** Category Service로 Presenter를 만들고 lifecycle-aware 상태와 뒤로 가기 callback을 연결한다. */
@Composable
fun CategoryManagementRoute(onBack: () -> Unit) {
    val application = LocalContext.current.applicationContext as TodoApplication
    val factory = remember(application.container) {
        CategoryManagementPresenterFactory(application.container.categoryService)
    }
    val presenter: CategoryManagementPresenter = viewModel(factory = factory)
    val state by presenter.state.collectAsStateWithLifecycle()

    CategoryManagementScreen(
        state = state,
        onEvent = presenter::onEvent,
        onBack = onBack,
    )
}
