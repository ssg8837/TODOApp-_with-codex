package com.example.todoapplication

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.mutableStateOf
import com.example.todoapplication.app.TodoApp
import com.example.todoapplication.ui.theme.ToDOApplicationTheme

/** Compose 앱을 호스팅하고 Notification Intent의 TODO ID를 시작 navigation에 전달한다. */
class MainActivity : ComponentActivity() {
    private val initialTodoId = mutableStateOf<Long?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initialTodoId.value = intent.getLongExtra(EXTRA_TODO_ID, 0L).takeIf { it > 0L }
        enableEdgeToEdge()
        setContent {
            ToDOApplicationTheme {
                TodoApp(initialTodoId = initialTodoId.value)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        initialTodoId.value = intent.getLongExtra(EXTRA_TODO_ID, 0L).takeIf { it > 0L }
    }

    companion object {
        const val EXTRA_TODO_ID = "todo_id"
    }
}
