package com.example.todoapplication.presenterfactory

import com.example.todoapplication.feature.todo.list.FakeCategoryService
import com.example.todoapplication.feature.todo.list.FakeTodoService
import com.example.todoapplication.feature.todo.list.TodoListPresenter
import com.example.todoapplication.test.MainDispatcherRule
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class TodoListPresenterFactoryTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun factoryCreatesPresenterFromProvidedService() {
        val factory = TodoListPresenterFactory(
            todoService = FakeTodoService(),
            categoryService = FakeCategoryService(),
            clock = Clock.fixed(Instant.parse("2026-09-06T00:00:00Z"), ZoneOffset.UTC),
        )

        val presenter = factory.create(TodoListPresenter::class.java)

        assertEquals(TodoListPresenter::class.java, presenter.javaClass)
    }
}
