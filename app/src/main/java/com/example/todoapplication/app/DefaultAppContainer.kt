package com.example.todoapplication.app

import android.content.Context
import com.example.todoapplication.application.service.CategoryService
import com.example.todoapplication.application.service.DefaultCategoryService
import com.example.todoapplication.application.service.DefaultReminderService
import com.example.todoapplication.application.service.DefaultTodoService
import com.example.todoapplication.application.service.ReminderService
import com.example.todoapplication.application.service.TodoService
import com.example.todoapplication.data.local.TodoDatabase
import com.example.todoapplication.data.repository.RoomCategoryRepository
import com.example.todoapplication.data.repository.RoomReminderRepository
import com.example.todoapplication.data.repository.RoomTodoRepository
import com.example.todoapplication.domain.repository.CategoryRepository
import com.example.todoapplication.domain.repository.ReminderRepository
import com.example.todoapplication.domain.repository.TodoRepository

class DefaultAppContainer(
    context: Context,
) : AppContainer {
    override val database: TodoDatabase = TodoDatabase.create(context.applicationContext)

    override val todoRepository: TodoRepository = RoomTodoRepository(database.todoDao())

    override val categoryRepository: CategoryRepository =
        RoomCategoryRepository(database.categoryDao())

    override val reminderRepository: ReminderRepository =
        RoomReminderRepository(database.reminderDao())

    override val todoService: TodoService = DefaultTodoService(
        todoRepository = todoRepository,
        categoryRepository = categoryRepository,
    )

    override val categoryService: CategoryService = DefaultCategoryService(categoryRepository)

    override val reminderService: ReminderService = DefaultReminderService(
        reminderRepository = reminderRepository,
        todoRepository = todoRepository,
    )
}
