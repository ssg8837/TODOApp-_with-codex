package com.example.todoapplication.app

import com.example.todoapplication.application.service.CategoryService
import com.example.todoapplication.application.service.ReminderService
import com.example.todoapplication.application.service.ReminderRecoveryService
import com.example.todoapplication.application.service.TodoService
import com.example.todoapplication.data.local.TodoDatabase
import com.example.todoapplication.domain.repository.CategoryRepository
import com.example.todoapplication.domain.repository.ReminderRepository
import com.example.todoapplication.domain.repository.TodoRepository
import com.example.todoapplication.notification.AlarmScheduler

/**
 * Database, Repository, Application Service와 Android 시스템 adapter를 노출하는 수동 DI 계약.
 * Activity와 Receiver는 이 경계에서 이미 조립된 의존성을 가져온다.
 */
interface AppContainer {
    val database: TodoDatabase
    val todoRepository: TodoRepository
    val categoryRepository: CategoryRepository
    val reminderRepository: ReminderRepository
    val alarmScheduler: AlarmScheduler
    val todoService: TodoService
    val categoryService: CategoryService
    val reminderService: ReminderService
    val reminderRecoveryService: ReminderRecoveryService
}
