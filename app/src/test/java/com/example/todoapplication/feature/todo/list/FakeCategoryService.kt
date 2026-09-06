package com.example.todoapplication.feature.todo.list

import com.example.todoapplication.application.service.CategoryService
import com.example.todoapplication.application.service.ServiceResult
import com.example.todoapplication.domain.model.Category
import com.example.todoapplication.domain.model.CategoryColor
import java.time.Instant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow

internal class FakeCategoryService(
    initialCategories: List<Category> = listOf(defaultCategory()),
) : CategoryService {
    val categories = MutableSharedFlow<ServiceResult<List<Category>>>(replay = 1)
    var observationCount = 0
        private set

    init {
        categories.tryEmit(ServiceResult.Success(initialCategories))
    }

    override fun observeAll(): Flow<ServiceResult<List<Category>>> {
        observationCount += 1
        return categories
    }

    override suspend fun create(category: Category): ServiceResult<Category> = unused()

    override suspend fun update(category: Category): ServiceResult<Category> = unused()

    override suspend fun delete(categoryId: Long): ServiceResult<Unit> = unused()

    override suspend fun getById(categoryId: Long): ServiceResult<Category> = unused()

    override suspend fun reorder(categoryIds: List<Long>): ServiceResult<Unit> = unused()

    private fun <T> unused(): ServiceResult<T> =
        error("Not used by TodoListPresenter in Phase 5")

    companion object {
        fun defaultCategory(
            id: Long = 1,
            name: String = "Category",
            color: CategoryColor = CategoryColor.NEUTRAL,
        ): Category = Category(
            id = id,
            name = name,
            color = color,
            sortOrder = 0,
            isSystem = true,
            createdAt = Instant.parse("2026-09-06T00:00:00Z"),
        )
    }
}
