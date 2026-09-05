package com.example.todoapplication.domain.validation

import com.example.todoapplication.domain.model.Todo
import java.time.Instant
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TodoValidatorTest {
    @Test
    fun validTodoHasNoErrors() {
        assertTrue(TodoValidator.validate(todo()).isEmpty())
    }

    @Test
    fun blankTitleAndMissingCategoryAreRejected() {
        val errors = TodoValidator.validate(todo(title = "   ", categoryId = 0))

        assertEquals(
            setOf(
                TodoValidationError.BLANK_TITLE,
                TodoValidationError.INVALID_CATEGORY_ID,
            ),
            errors,
        )
    }

    private fun todo(
        title: String = "장보기",
        categoryId: Long = 1,
    ) = Todo(
        id = 0,
        title = title,
        date = LocalDate.of(2026, 9, 5),
        time = null,
        categoryId = categoryId,
        isCompleted = false,
        createdAt = Instant.EPOCH,
        updatedAt = Instant.EPOCH,
    )
}
