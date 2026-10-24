package com.example.todoapplication.presenterfactory

import com.example.todoapplication.application.service.ReminderService
import com.example.todoapplication.application.service.ServiceResult
import com.example.todoapplication.domain.model.Reminder
import com.example.todoapplication.feature.todo.detail.TodoDetailPresenter
import com.example.todoapplication.feature.todo.list.FakeCategoryService
import com.example.todoapplication.feature.todo.list.FakeTodoService
import com.example.todoapplication.test.MainDispatcherRule
import java.time.LocalDate
import java.time.LocalTime
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class TodoDetailPresenterFactoryTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun factoryCreatesPresenterFromProvidedServices() {
        val factory = TodoDetailPresenterFactory(
            todoService = FakeTodoService(),
            categoryService = FakeCategoryService(),
            reminderService = FakeReminderService(),
            todoId = 1L,
        )

        val presenter = factory.create(TodoDetailPresenter::class.java)

        assertEquals(TodoDetailPresenter::class.java, presenter.javaClass)
    }

    private class FakeReminderService : ReminderService {
        override fun observeByTodoId(todoId: Long): Flow<ServiceResult<List<Reminder>>> = emptyFlow()
        override suspend fun create(reminder: Reminder): ServiceResult<Reminder> = error("unused")
        override suspend fun getById(reminderId: Long): ServiceResult<Reminder> = error("unused")
        override suspend fun replaceReminders(todoId: Long, reminders: List<Reminder>): ServiceResult<List<Reminder>> = error("unused")
        override suspend fun delete(reminderId: Long): ServiceResult<Unit> = error("unused")
        override suspend fun deleteByTodoId(todoId: Long): ServiceResult<Int> = error("unused")
        override suspend fun getFutureAlarmRecoveryCandidates(currentDate: LocalDate, currentTime: LocalTime): ServiceResult<List<Reminder>> = error("unused")
    }
}

