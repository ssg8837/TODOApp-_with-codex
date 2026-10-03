package com.example.todoapplication.application.service

import com.example.todoapplication.domain.validation.CategoryValidationError
import com.example.todoapplication.domain.validation.ReminderValidationError
import com.example.todoapplication.domain.validation.TodoValidationError

/** Application Service가 성공 값 또는 UI 문자열과 분리된 의미 오류를 반환하는 계약. */
sealed interface ServiceResult<out T> {
    data class Success<T>(val value: T) : ServiceResult<T>

    data class Failure(val error: ServiceError) : ServiceResult<Nothing>
}

/** Presenter가 화면 오류로 변환할 수 있는 최소한의 비즈니스/Application 오류 집합. */
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
