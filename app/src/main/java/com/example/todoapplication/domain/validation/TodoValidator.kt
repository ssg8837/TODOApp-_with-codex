package com.example.todoapplication.domain.validation

import com.example.todoapplication.domain.model.Todo

/** TODO가 지켜야 할 순수 도메인 검증 실패 사유. */
enum class TodoValidationError {
    BLANK_TITLE,
    INVALID_CATEGORY_ID,
}

/** 저장소나 Android API에 의존하지 않고 TODO의 필수 불변식을 검증한다. */
object TodoValidator {
    fun validate(todo: Todo): Set<TodoValidationError> = buildSet {
        if (todo.title.isBlank()) add(TodoValidationError.BLANK_TITLE)
        if (todo.categoryId <= 0) add(TodoValidationError.INVALID_CATEGORY_ID)
    }
}
