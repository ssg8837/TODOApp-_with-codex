package com.example.todoapplication.feature.todo.detail

/** TODO 확인 화면에서 발생하는 사용자 이벤트. */
sealed interface TodoDetailEvent {
    data object Refresh : TodoDetailEvent
}

