package com.example.todoapplication.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import com.example.todoapplication.data.local.entity.CategoryEntity
import kotlinx.coroutines.flow.Flow

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
