package com.example.todoapplication.feature.todo.edit

import java.time.LocalDate
import java.time.LocalTime

sealed interface TodoEditEvent {
    data class TitleChanged(val title: String) : TodoEditEvent

    data class DateChanged(val date: LocalDate) : TodoEditEvent

    data class TimeChanged(val time: LocalTime) : TodoEditEvent

    data object TimeCleared : TodoEditEvent

    data class CategoryChanged(val categoryId: Long) : TodoEditEvent

    data object Save : TodoEditEvent
}

sealed interface TodoEditEffect {
    data object Saved : TodoEditEffect
}
