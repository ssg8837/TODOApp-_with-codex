package com.example.todoapplication.domain.model

import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

/**
 * 사용자가 관리하는 TODO의 도메인 모델.
 *
 * [categoryId]는 유효한 Category를 가리키는 non-null 식별자다. [time]이 `null`이면
 * 특정 실행 시각이 없는 일정이며 Reminder를 설정할 수 없다. 시간 값은 분 단위로 다룬다.
 */
data class Todo(
    val id: Long,
    val title: String,
    val date: LocalDate,
    val time: LocalTime?,
    val categoryId: Long,
    val isCompleted: Boolean,
    val createdAt: Instant,
    val updatedAt: Instant,
)
