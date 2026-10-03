package com.example.todoapplication.app

import androidx.compose.runtime.Composable
import androidx.navigation.compose.rememberNavController
import com.example.todoapplication.navigation.TodoNavHost

/** 앱 NavController를 만들고 Notification 진입 TODO ID를 navigation host에 전달한다. */
@Composable
fun TodoApp(initialTodoId: Long? = null) {
    val navController = rememberNavController()
    TodoNavHost(navController = navController, initialTodoId = initialTodoId)
}
