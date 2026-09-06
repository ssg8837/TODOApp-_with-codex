package com.example.todoapplication.application.service

import com.example.todoapplication.domain.model.Category
import kotlinx.coroutines.flow.Flow

interface CategoryService {
    suspend fun create(category: Category): ServiceResult<Category>

    suspend fun update(category: Category): ServiceResult<Category>

    suspend fun delete(categoryId: Long): ServiceResult<Unit>

    fun observeAll(): Flow<ServiceResult<List<Category>>>

    suspend fun getById(categoryId: Long): ServiceResult<Category>

    suspend fun reorder(categoryIds: List<Long>): ServiceResult<Unit>
}
