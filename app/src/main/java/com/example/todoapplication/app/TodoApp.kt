package com.example.todoapplication.app

import androidx.compose.runtime.Composable
import androidx.navigation.compose.rememberNavController
import com.example.todoapplication.navigation.TodoNavHost

@Composable
fun TodoApp(initialTodoId: Long? = null) {
    val navController = rememberNavController()
    TodoNavHost(navController = navController, initialTodoId = initialTodoId)
}
