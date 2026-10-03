package com.example.todoapplication.feature.todo.edit

import java.time.LocalDate
import java.time.LocalTime

/** TODO 편집 화면에서 Presenter가 처리하는 사용자 의도. */
sealed interface TodoEditEvent {
    data class SetOneDayReminder(val enabled: Boolean) : TodoEditEvent

    data class SetFifteenMinuteReminder(val enabled: Boolean) : TodoEditEvent

    data class NotificationPermissionResult(val granted: Boolean) : TodoEditEvent
    data class TitleChanged(val title: String) : TodoEditEvent

    data class DateChanged(val date: LocalDate) : TodoEditEvent

    data class TimeChanged(val time: LocalTime) : TodoEditEvent

    data object TimeCleared : TodoEditEvent

    data class CategoryChanged(val categoryId: Long) : TodoEditEvent

    data object Save : TodoEditEvent

    data object RequestDelete : TodoEditEvent

    data object ConfirmDelete : TodoEditEvent

    data object CancelDelete : TodoEditEvent
}

/** 저장·삭제 성공 후 Route가 Navigation에 사용하는 one-shot 효과. */
sealed interface TodoEditEffect {
    data object Saved : TodoEditEffect

    data object Deleted : TodoEditEffect
}
