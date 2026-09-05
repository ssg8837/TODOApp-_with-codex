package com.example.todoapplication.app

import com.example.todoapplication.data.local.TodoDatabase
import com.example.todoapplication.domain.repository.CategoryRepository
import com.example.todoapplication.domain.repository.ReminderRepository
import com.example.todoapplication.domain.repository.TodoRepository

interface AppContainer {
    val database: TodoDatabase
    val todoRepository: TodoRepository
    val categoryRepository: CategoryRepository
    val reminderRepository: ReminderRepository
}
