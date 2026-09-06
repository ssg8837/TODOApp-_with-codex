package com.example.todoapplication.application.service

import com.example.todoapplication.domain.validation.CategoryValidationError
import com.example.todoapplication.domain.validation.ReminderValidationError
import com.example.todoapplication.domain.validation.TodoValidationError

sealed interface ServiceResult<out T> {
    data class Success<T>(val value: T) : ServiceResult<T>

    data class Failure(val error: ServiceError) : ServiceResult<Nothing>
}

sealed interface ServiceError {
    data class InvalidTodo(
        val errors: Set<TodoValidationError>,
    ) : ServiceError

    data class InvalidCategory(
        val errors: Set<CategoryValidationError>,
    ) : ServiceError

    data class InvalidReminder(
        val errors: Set<ReminderValidationError>,
    ) : ServiceError

    data object TodoNotFound : ServiceError
    data object CategoryNotFound : ServiceError
    data object ReminderNotFound : ServiceError
    data object SystemCategoryOperationProhibited : ServiceError
    data object CategoryNameDuplicated : ServiceError
    data object InvalidCategoryOrder : ServiceError
    data object ReminderRequiresTodoTime : ServiceError
    data object PersistenceFailure : ServiceError
}
