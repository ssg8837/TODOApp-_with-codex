package com.example.todoapplication.application.service

import com.example.todoapplication.domain.model.Category
import com.example.todoapplication.domain.repository.CategoryRepository
import com.example.todoapplication.domain.validation.CategoryValidator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class DefaultCategoryService(
    private val categoryRepository: CategoryRepository,
) : CategoryService {
    override suspend fun create(category: Category): ServiceResult<Category> = serviceCall {
        if (category.isSystem) {
            return@serviceCall ServiceResult.Failure(
                ServiceError.SystemCategoryOperationProhibited,
            )
        }
        val normalizedName = category.name.trim()
        val validationCandidate = category.copy(
            name = normalizedName,
            sortOrder = FIRST_USER_SORT_ORDER,
            isSystem = false,
        )
        val initialErrors = CategoryValidator.validate(validationCandidate)
        if (initialErrors.isNotEmpty()) {
            return@serviceCall ServiceResult.Failure(
                ServiceError.InvalidCategory(initialErrors),
            )
        }
        if (categoryRepository.isNameTaken(normalizedName)) {
            return@serviceCall ServiceResult.Failure(ServiceError.CategoryNameDuplicated)
        }
        val candidate = category.copy(
            name = normalizedName,
            sortOrder = categoryRepository.getNextUserSortOrder(),
            isSystem = false,
        )
        val errors = CategoryValidator.validate(candidate)
        if (errors.isNotEmpty()) {
            return@serviceCall ServiceResult.Failure(ServiceError.InvalidCategory(errors))
        }
        ServiceResult.Success(categoryRepository.create(candidate))
    }

    override suspend fun update(category: Category): ServiceResult<Category> = serviceCall {
        val existing = categoryRepository.getById(category.id)
            ?: return@serviceCall ServiceResult.Failure(ServiceError.CategoryNotFound)
        if (existing.isSystem) {
            return@serviceCall ServiceResult.Failure(
                ServiceError.SystemCategoryOperationProhibited,
            )
        }
        val normalizedName = category.name.trim()
        val categoryWithName = categoryRepository.getByName(normalizedName)
        if (categoryWithName != null && categoryWithName.id != existing.id) {
            return@serviceCall ServiceResult.Failure(ServiceError.CategoryNameDuplicated)
        }
        val candidate = existing.copy(
            name = normalizedName,
            color = category.color,
        )
        val errors = CategoryValidator.validate(candidate)
        if (errors.isNotEmpty()) {
            return@serviceCall ServiceResult.Failure(ServiceError.InvalidCategory(errors))
        }
        if (!categoryRepository.update(candidate)) {
            return@serviceCall ServiceResult.Failure(ServiceError.CategoryNotFound)
        }
        ServiceResult.Success(candidate)
    }

    override suspend fun delete(categoryId: Long): ServiceResult<Unit> = serviceCall {
        val category = categoryRepository.getById(categoryId)
            ?: return@serviceCall ServiceResult.Failure(ServiceError.CategoryNotFound)
        if (category.isSystem) {
            return@serviceCall ServiceResult.Failure(
                ServiceError.SystemCategoryOperationProhibited,
            )
        }
        if (!categoryRepository.deleteCategory(categoryId)) {
            return@serviceCall ServiceResult.Failure(ServiceError.CategoryNotFound)
        }
        ServiceResult.Success(Unit)
    }

    override fun observeAll(): Flow<ServiceResult<List<Category>>> =
        categoryRepository.observeAll().asServiceResult()

    override suspend fun getById(categoryId: Long): ServiceResult<Category> = serviceCall {
        val category = categoryRepository.getById(categoryId)
            ?: return@serviceCall ServiceResult.Failure(ServiceError.CategoryNotFound)
        ServiceResult.Success(category)
    }

    override suspend fun reorder(categoryIds: List<Long>): ServiceResult<Unit> = serviceCall {
        val categories = categoryRepository.observeAll().first()
        val userCategoryIds = categories.filterNot(Category::isSystem).map(Category::id)
        val isValidOrder = categoryIds.size == userCategoryIds.size &&
            categoryIds.distinct().size == categoryIds.size &&
            categoryIds.toSet() == userCategoryIds.toSet()
        if (!isValidOrder) {
            return@serviceCall ServiceResult.Failure(ServiceError.InvalidCategoryOrder)
        }
        categoryRepository.saveUserCategoryOrder(categoryIds)
        ServiceResult.Success(Unit)
    }

    private companion object {
        const val FIRST_USER_SORT_ORDER = 1
    }
}
