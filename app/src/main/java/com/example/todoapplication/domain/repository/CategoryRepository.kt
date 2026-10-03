package com.example.todoapplication.domain.repository

import com.example.todoapplication.domain.model.Category
import kotlinx.coroutines.flow.Flow

/**
 * Category를 Domain 모델로 저장·관찰하는 Repository 계약.
 *
 * Category 삭제와 순서 저장의 원자적 세부 절차는 구현 아래 DAO에 캡슐화된다.
 */
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

    /** 사용자 Category 전체 순서를 하나의 영속화 동작으로 저장한다. */
    suspend fun saveUserCategoryOrder(categoryIds: List<Long>)
}
