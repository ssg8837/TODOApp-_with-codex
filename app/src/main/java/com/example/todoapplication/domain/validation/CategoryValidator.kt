package com.example.todoapplication.domain.validation

import com.example.todoapplication.domain.model.Category

enum class CategoryValidationError {
    BLANK_NAME,
    DUPLICATE_NAME,
    INVALID_SYSTEM_NAME,
    INVALID_SYSTEM_COLOR,
    INVALID_SYSTEM_SORT_ORDER,
    RESERVED_SYSTEM_NAME,
    INVALID_USER_SORT_ORDER,
}

object CategoryValidator {
    fun validate(
        category: Category,
        existingNames: Set<String> = emptySet(),
    ): Set<CategoryValidationError> = buildSet {
        val normalizedName = category.name.trim()

        if (normalizedName.isEmpty()) add(CategoryValidationError.BLANK_NAME)
        if (existingNames.any { it.trim() == normalizedName }) {
            add(CategoryValidationError.DUPLICATE_NAME)
        }

        if (category.isSystem) {
            if (category.name != Category.SYSTEM_DEFAULT_NAME) {
                add(CategoryValidationError.INVALID_SYSTEM_NAME)
            }
            if (category.color != Category.SYSTEM_DEFAULT_COLOR) {
                add(CategoryValidationError.INVALID_SYSTEM_COLOR)
            }
            if (category.sortOrder != Category.SYSTEM_DEFAULT_SORT_ORDER) {
                add(CategoryValidationError.INVALID_SYSTEM_SORT_ORDER)
            }
        } else {
            if (normalizedName == Category.SYSTEM_DEFAULT_NAME) {
                add(CategoryValidationError.RESERVED_SYSTEM_NAME)
            }
            if (category.sortOrder < FIRST_USER_SORT_ORDER) {
                add(CategoryValidationError.INVALID_USER_SORT_ORDER)
            }
        }
    }

    private const val FIRST_USER_SORT_ORDER = 1
}
