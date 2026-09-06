package com.example.todoapplication.application.service

import com.example.todoapplication.domain.validation.CategoryValidationError
import com.example.todoapplication.test.FakeCategoryRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class DefaultCategoryServiceTest {
    @Test
    fun createUserCategoryAppliesNextSortOrder() = runBlocking {
        val repository = FakeCategoryRepository(
            listOf(systemCategory(), userCategory(sortOrder = 1)),
        )
        val service = DefaultCategoryService(repository)

        val result = service.create(userCategory(id = 0, name = " 개인 ", sortOrder = 99))

        val saved = (result as ServiceResult.Success).value
        assertEquals("개인", saved.name)
        assertEquals(2, saved.sortOrder)
        assertEquals(1, repository.writeCount)
    }

    @Test
    fun reservedSystemNameIsRejectedWithoutWrite() = runBlocking {
        val repository = FakeCategoryRepository(listOf(systemCategory()))
        val service = DefaultCategoryService(repository)

        val result = service.create(userCategory(id = 0, name = " 일반 "))

        assertEquals(
            ServiceResult.Failure(
                ServiceError.InvalidCategory(
                    setOf(CategoryValidationError.RESERVED_SYSTEM_NAME),
                ),
            ),
            result,
        )
        assertEquals(0, repository.writeCount)
    }

    @Test
    fun duplicateNameIsRejectedWithoutWrite() = runBlocking {
        val repository = FakeCategoryRepository(listOf(systemCategory(), userCategory()))
        val service = DefaultCategoryService(repository)

        assertEquals(
            ServiceResult.Failure(ServiceError.CategoryNameDuplicated),
            service.create(userCategory(id = 0, name = "업무")),
        )
        assertEquals(0, repository.writeCount)
    }

    @Test
    fun blankNameValidationDoesNotWrite() = runBlocking {
        val repository = FakeCategoryRepository(listOf(systemCategory()))
        val service = DefaultCategoryService(repository)

        val result = service.create(userCategory(id = 0, name = "  "))

        assertEquals(
            ServiceResult.Failure(
                ServiceError.InvalidCategory(setOf(CategoryValidationError.BLANK_NAME)),
            ),
            result,
        )
        assertEquals(0, repository.writeCount)
    }

    @Test
    fun systemCategoryUpdateAndDeleteAreRejected() = runBlocking {
        val system = systemCategory()
        val repository = FakeCategoryRepository(listOf(system))
        val service = DefaultCategoryService(repository)

        val failure = ServiceResult.Failure(ServiceError.SystemCategoryOperationProhibited)
        assertEquals(failure, service.update(system.copy(name = "변경")))
        assertEquals(failure, service.delete(system.id))
        assertEquals(0, repository.writeCount)
    }

    @Test
    fun userCategoryCanBeUpdatedAndDeleted() = runBlocking {
        val user = userCategory()
        val repository = FakeCategoryRepository(listOf(systemCategory(), user))
        val service = DefaultCategoryService(repository)
        val changed = user.copy(name = " 개인 ")

        assertEquals(
            ServiceResult.Success(user.copy(name = "개인")),
            service.update(changed),
        )
        assertEquals(ServiceResult.Success(Unit), service.delete(user.id))
        assertEquals(listOf("getById", "update", "getById", "deleteCategory"), repository.calls)
    }

    @Test
    fun reorderAcceptsAllUserIdsAndDelegatesSingleRepositoryOperation() = runBlocking {
        val first = userCategory(id = 2, sortOrder = 1)
        val second = userCategory(id = 3, name = "개인", sortOrder = 2)
        val repository = FakeCategoryRepository(listOf(systemCategory(), first, second))
        val service = DefaultCategoryService(repository)

        assertEquals(ServiceResult.Success(Unit), service.reorder(listOf(second.id, first.id)))
        assertEquals(listOf(second.id, first.id), repository.savedOrder)
        assertEquals(1, repository.writeCount)
    }

    @Test
    fun reorderRejectsSystemDuplicateMissingAndUnknownIdsWithoutWrite() = runBlocking {
        val first = userCategory(id = 2)
        val second = userCategory(id = 3, name = "개인", sortOrder = 2)
        val invalidOrders = listOf(
            listOf(1L, 2L, 3L),
            listOf(2L, 2L),
            listOf(2L),
            listOf(2L, 99L),
        )

        invalidOrders.forEach { invalidOrder ->
            val repository = FakeCategoryRepository(listOf(systemCategory(), first, second))
            val service = DefaultCategoryService(repository)

            assertEquals(
                ServiceResult.Failure(ServiceError.InvalidCategoryOrder),
                service.reorder(invalidOrder),
            )
            assertEquals(0, repository.writeCount)
        }
    }
}
