package com.example.todoapplication.domain.validation

import com.example.todoapplication.domain.model.Category
import com.example.todoapplication.domain.model.CategoryColor
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CategoryValidatorTest {
    @Test
    fun validSystemCategoryHasNoErrors() {
        assertTrue(CategoryValidator.validate(systemCategory()).isEmpty())
    }

    @Test
    fun systemCategoryMustKeepReservedValues() {
        val errors = CategoryValidator.validate(
            systemCategory(
                name = "기타",
                color = CategoryColor.RED,
                sortOrder = 2,
            ),
        )

        assertEquals(
            setOf(
                CategoryValidationError.INVALID_SYSTEM_NAME,
                CategoryValidationError.INVALID_SYSTEM_COLOR,
                CategoryValidationError.INVALID_SYSTEM_SORT_ORDER,
            ),
            errors,
        )
    }

    @Test
    fun userCategoryRejectsBlankReservedDuplicateNameAndInvalidOrder() {
        val blankErrors = CategoryValidator.validate(userCategory(name = " ", sortOrder = 0))
        val reservedErrors = CategoryValidator.validate(userCategory(name = "일반"))
        val duplicateErrors = CategoryValidator.validate(
            userCategory(name = " 업무 "),
            existingNames = setOf("업무"),
        )

        assertEquals(
            setOf(
                CategoryValidationError.BLANK_NAME,
                CategoryValidationError.INVALID_USER_SORT_ORDER,
            ),
            blankErrors,
        )
        assertEquals(setOf(CategoryValidationError.RESERVED_SYSTEM_NAME), reservedErrors)
        assertEquals(setOf(CategoryValidationError.DUPLICATE_NAME), duplicateErrors)
    }

    private fun systemCategory(
        name: String = Category.SYSTEM_DEFAULT_NAME,
        color: CategoryColor = Category.SYSTEM_DEFAULT_COLOR,
        sortOrder: Int = Category.SYSTEM_DEFAULT_SORT_ORDER,
    ) = Category(
        id = 1,
        name = name,
        color = color,
        sortOrder = sortOrder,
        isSystem = true,
        createdAt = Instant.EPOCH,
    )

    private fun userCategory(
        name: String = "업무",
        sortOrder: Int = 1,
    ) = Category(
        id = 0,
        name = name,
        color = CategoryColor.BLUE,
        sortOrder = sortOrder,
        isSystem = false,
        createdAt = Instant.EPOCH,
    )
}
