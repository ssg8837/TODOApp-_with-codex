package com.example.todoapplication.domain.validation

import com.example.todoapplication.domain.model.Todo

enum class TodoValidationError {
    BLANK_TITLE,
    INVALID_CATEGORY_ID,
}

object TodoValidator {
    fun validate(todo: Todo): Set<TodoValidationError> = buildSet {
        if (todo.title.isBlank()) add(TodoValidationError.BLANK_TITLE)
        if (todo.categoryId <= 0) add(TodoValidationError.INVALID_CATEGORY_ID)
    }
}
