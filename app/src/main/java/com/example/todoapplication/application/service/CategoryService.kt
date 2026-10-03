package com.example.todoapplication.application.service

import com.example.todoapplication.domain.model.Category
import kotlinx.coroutines.flow.Flow

/**
 * Category 생성·수정·삭제·정렬 정책을 수행하는 Application Service 계약.
 *
 * 시스템 `일반` 보호와 Domain validation을 적용하고 원자적 영속 절차는 Repository에 위임한다.
 */
interface CategoryService {
    suspend fun create(category: Category): ServiceResult<Category>

    suspend fun update(category: Category): ServiceResult<Category>

    suspend fun delete(categoryId: Long): ServiceResult<Unit>

    fun observeAll(): Flow<ServiceResult<List<Category>>>

    suspend fun getById(categoryId: Long): ServiceResult<Category>

    /** 모든 사용자 Category가 정확히 한 번 포함된 순서를 검증해 저장한다. */
    suspend fun reorder(categoryIds: List<Long>): ServiceResult<Unit>
}
