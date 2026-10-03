package com.example.todoapplication.data.repository

import com.example.todoapplication.data.local.dao.CategoryDao
import com.example.todoapplication.data.mapper.CategoryMapper
import com.example.todoapplication.domain.model.Category
import com.example.todoapplication.domain.repository.CategoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * [CategoryRepository]의 Room 구현.
 *
 * Entity↔Domain 변환과 DAO 호출을 담당한다. 삭제 재지정과 reorder의 transaction 절차는
 * 직접 재구현하지 않고 [CategoryDao]의 원자적 API에 위임한다.
 */
internal class RoomCategoryRepository(
    private val categoryDao: CategoryDao,
) : CategoryRepository {
    override suspend fun create(category: Category): Category = dataAccess {
        val id = categoryDao.insert(CategoryMapper.toEntity(category))
        category.copy(id = id)
    }

    override suspend fun update(category: Category): Boolean = dataAccess {
        categoryDao.updateUserCategory(
            id = category.id,
            name = category.name,
            colorValue = category.color.storageValue,
            sortOrder = category.sortOrder,
        ) == 1
    }

    override suspend fun deleteCategory(categoryId: Long): Boolean = dataAccess {
        categoryDao.deleteUserCategoryAndReassignTodos(categoryId)
    }

    override fun observeAll(): Flow<List<Category>> = categoryDao.observeAll()
        .map { entities -> entities.map(CategoryMapper::toDomain) }
        .mapDataAccessErrors()

    override suspend fun getById(id: Long): Category? = dataAccess {
        categoryDao.getById(id)?.let(CategoryMapper::toDomain)
    }

    override suspend fun getByName(name: String): Category? = dataAccess {
        categoryDao.getByName(name)?.let(CategoryMapper::toDomain)
    }

    override suspend fun getSystemCategory(): Category? = dataAccess {
        categoryDao.getSystemCategory()?.let(CategoryMapper::toDomain)
    }

    override suspend fun isNameTaken(name: String): Boolean = dataAccess {
        categoryDao.countByName(name) > 0
    }

    override suspend fun getNextUserSortOrder(): Int = dataAccess {
        categoryDao.getNextUserSortOrder()
    }

    override suspend fun saveUserCategoryOrder(categoryIds: List<Long>) = dataAccess {
        categoryDao.saveUserCategoryOrder(categoryIds)
    }
}
