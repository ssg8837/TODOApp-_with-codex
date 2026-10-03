package com.example.todoapplication.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import com.example.todoapplication.data.local.entity.CategoryEntity
import kotlinx.coroutines.flow.Flow

/**
 * Category 저장과 정렬, 삭제 시 참조 재지정을 담당하는 Room DAO.
 *
 * 전체 목록은 시스템 Category를 먼저, 사용자 Category를 저장된 순서로 방출한다.
 */
@Dao
abstract class CategoryDao {
    @Insert
    abstract suspend fun insert(category: CategoryEntity): Long

    @Query(
        """
        UPDATE categories
        SET name = :name, color = :colorValue, sort_order = :sortOrder
        WHERE id = :id AND is_system = 0
        """,
    )
    abstract suspend fun updateUserCategory(
        id: Long,
        name: String,
        colorValue: String,
        sortOrder: Int,
    ): Int

    @Query("DELETE FROM categories WHERE id = :id AND is_system = 0")
    protected abstract suspend fun deleteUserCategoryById(id: Long): Int

    @Query(
        """
        SELECT * FROM categories
        ORDER BY is_system DESC, sort_order ASC, created_at_epoch_millis ASC, id ASC
        """,
    )
    abstract fun observeAll(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories WHERE id = :id")
    abstract suspend fun getById(id: Long): CategoryEntity?

    @Query("SELECT * FROM categories WHERE name = :name")
    abstract suspend fun getByName(name: String): CategoryEntity?

    @Query("SELECT * FROM categories WHERE is_system = 1 LIMIT 1")
    abstract suspend fun getSystemCategory(): CategoryEntity?

    @Query("SELECT COALESCE(MAX(sort_order), 0) + 1 FROM categories WHERE is_system = 0")
    abstract suspend fun getNextUserSortOrder(): Int

    @Query("UPDATE categories SET sort_order = :sortOrder WHERE id = :id AND is_system = 0")
    protected abstract suspend fun updateUserSortOrder(id: Long, sortOrder: Int): Int

    @Query("UPDATE todos SET category_id = :newCategoryId WHERE category_id = :oldCategoryId")
    protected abstract suspend fun reassignTodos(oldCategoryId: Long, newCategoryId: Long): Int

    @Query("SELECT COUNT(*) FROM categories WHERE name = :name")
    abstract suspend fun countByName(name: String): Int

    @Query("SELECT COUNT(*) FROM categories WHERE is_system = 1")
    abstract suspend fun countSystemCategories(): Int

    @Query("SELECT COUNT(*) FROM categories WHERE is_system = 0")
    protected abstract suspend fun countUserCategories(): Int

    /**
     * 사용자 Category를 참조하는 모든 TODO를 시스템 `일반`로 재지정한 뒤 삭제한다.
     *
     * 조회·재지정·삭제는 하나의 transaction이므로 실패 시 전체가 rollback된다. 시스템
     * Category 삭제는 거부하며 TODO 자체는 삭제하지 않는다.
     */
    @Transaction
    open suspend fun deleteUserCategoryAndReassignTodos(categoryId: Long): Boolean {
        val target = getById(categoryId) ?: return false
        require(!target.isSystem) { "System category cannot be deleted" }

        val systemCategory = checkNotNull(getSystemCategory()) {
            "System category is missing"
        }
        reassignTodos(oldCategoryId = categoryId, newCategoryId = systemCategory.id)
        check(deleteUserCategoryById(categoryId) == 1) {
            "Category deletion failed"
        }
        return true
    }

    /**
     * 모든 사용자 Category ID를 중복·누락 없이 받아 1부터 연속된 순서로 저장한다.
     * 검증과 각 행 갱신이 하나의 transaction에 포함되어 부분 순서 변경을 남기지 않는다.
     */
    @Transaction
    open suspend fun saveUserCategoryOrder(categoryIds: List<Long>) {
        require(categoryIds.distinct().size == categoryIds.size) {
            "Category order contains duplicate IDs"
        }
        require(categoryIds.size == countUserCategories()) {
            "Category order must contain every user category"
        }
        categoryIds.forEachIndexed { index, categoryId ->
            check(updateUserSortOrder(categoryId, index + 1) == 1) {
                "Only existing user categories can be reordered"
            }
        }
    }
}
