package com.example.todoapplication.domain.repository

import com.example.todoapplication.domain.model.Category
import kotlinx.coroutines.flow.Flow

interface CategoryRepository {
    suspend fun create(category: Category): Category

    suspend fun update(category: Category): Boolean

    suspend fun deleteCategory(categoryId: Long): Boolean

    fun observeAll(): Flow<List<Category>>

    suspend fun getById(id: Long): Category?

    suspend fun getByName(name: String): Category?

    suspend fun getSystemCategory(): Category?

    suspend fun isNameTaken(name: String): Boolean

    suspend fun getNextUserSortOrder(): Int

    suspend fun saveUserCategoryOrder(categoryIds: List<Long>)
}
