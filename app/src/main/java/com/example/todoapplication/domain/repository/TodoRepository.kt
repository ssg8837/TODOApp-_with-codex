package com.example.todoapplication.domain.repository

import com.example.todoapplication.domain.model.Todo
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

/**
 * TODO의 Domain 기반 저장·조회 계약.
 *
 * Room Entity나 DAO를 노출하지 않으며 Flow 역시 [Todo] 목록만 방출한다. 비즈니스 검증은
 * Application Service 책임이고 이 계층은 영속화 결과와 데이터 오류를 전달한다.
 */
interface TodoRepository {
    suspend fun create(todo: Todo): Todo

    suspend fun update(todo: Todo): Boolean

    suspend fun delete(todo: Todo): Boolean

    suspend fun getById(id: Long): Todo?

    fun observeByDate(date: LocalDate): Flow<List<Todo>>

    /** `null` Category는 전체를, [incompleteOnly]는 미완료만 조회함을 뜻한다. */
    fun observeFiltered(
        date: LocalDate,
        categoryId: Long?,
        incompleteOnly: Boolean,
    ): Flow<List<Todo>>
}
