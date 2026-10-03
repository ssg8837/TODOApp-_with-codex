package com.example.todoapplication.feature.todo.edit

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.todoapplication.app.TodoApplication
import com.example.todoapplication.presenterfactory.TodoEditPresenterFactory
import java.time.LocalDate

/**
 * 편집 Presenter의 상태·one-shot Effect와 TODO 편집 Screen을 연결한다.
 * Saved/Deleted 효과를 Navigation callback으로 전달하며 데이터 계층에는 접근하지 않는다.
 */
@Composable
fun TodoEditRoute(
    mode: TodoEditMode,
    initialDate: LocalDate,
    todoId: Long? = null,
    onBack: () -> Unit,
    onSaved: () -> Unit,
    onDeleted: () -> Unit,
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
    var pendingReminderEvent by remember { mutableStateOf<TodoEditEvent?>(null) }
    var notificationPermissionRequested by rememberSaveable { mutableStateOf(false) }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        presenter.onEvent(TodoEditEvent.NotificationPermissionResult(granted))
        pendingReminderEvent?.let(presenter::onEvent)
        pendingReminderEvent = null
    }

    LaunchedEffect(presenter) {
        presenter.effects.collect { effect ->
            when (effect) {
                TodoEditEffect.Saved -> onSaved()
                TodoEditEffect.Deleted -> onDeleted()
            }
        }
    }

    TodoEditScreen(
        state = state,
        onEvent = { event ->
            val enablesReminder = when (event) {
                is TodoEditEvent.SetOneDayReminder -> event.enabled
                is TodoEditEvent.SetFifteenMinuteReminder -> event.enabled
                else -> false
            }
            val requiresPermission = enablesReminder &&
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(
                    application,
                    Manifest.permission.POST_NOTIFICATIONS,
                ) != PackageManager.PERMISSION_GRANTED
            if (requiresPermission) {
                if (notificationPermissionRequested) {
                    presenter.onEvent(TodoEditEvent.NotificationPermissionResult(false))
                    presenter.onEvent(event)
                } else {
                    notificationPermissionRequested = true
                    pendingReminderEvent = event
                    permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            } else {
                presenter.onEvent(event)
            }
        },
        onBack = onBack,
    )
}
