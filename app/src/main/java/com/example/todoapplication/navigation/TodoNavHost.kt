package com.example.todoapplication.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.todoapplication.feature.todo.edit.TodoEditMode
import com.example.todoapplication.feature.todo.edit.TodoEditRoute
import com.example.todoapplication.feature.todo.list.TodoListRoute
import java.time.LocalDate

@Composable
fun TodoNavHost(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = TodoDestination.LIST,
    ) {
        composable(TodoDestination.LIST) {
            TodoListRoute(
                onAddTodo = { date -> navController.navigate(TodoDestination.newTodo(date)) },
                onEditTodo = { todoId -> navController.navigate(TodoDestination.editTodo(todoId)) },
            )
        }
        composable(
            route = TodoDestination.NEW_PATTERN,
            arguments = listOf(
                navArgument(TodoDestination.NEW_DATE_ARGUMENT) { type = NavType.StringType },
            ),
        ) { entry ->
            val initialDate = entry.arguments
                ?.getString(TodoDestination.NEW_DATE_ARGUMENT)
                ?.let(LocalDate::parse)
                ?: return@composable
            TodoEditRoute(
                mode = TodoEditMode.CREATE,
                initialDate = initialDate,
                onBack = navController::popBackStack,
                onSaved = navController::popBackStack,
            )
        }
        composable(
            route = TodoDestination.EDIT_PATTERN,
            arguments = listOf(
                navArgument(TodoDestination.TODO_ID_ARGUMENT) { type = NavType.LongType },
            ),
        ) { entry ->
            val todoId = entry.arguments?.getLong(TodoDestination.TODO_ID_ARGUMENT)
                ?: return@composable
            TodoEditRoute(
                mode = TodoEditMode.EDIT,
                initialDate = LocalDate.now(),
                todoId = todoId,
                onBack = navController::popBackStack,
                onSaved = navController::popBackStack,
            )
        }
    }
}
