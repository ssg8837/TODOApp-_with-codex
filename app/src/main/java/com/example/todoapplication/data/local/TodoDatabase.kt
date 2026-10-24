package com.example.todoapplication.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.todoapplication.data.local.dao.CategoryDao
import com.example.todoapplication.data.local.dao.ReminderDao
import com.example.todoapplication.data.local.dao.TodoDao
import com.example.todoapplication.data.local.entity.CategoryEntity
import com.example.todoapplication.data.local.entity.ReminderEntity
import com.example.todoapplication.data.local.entity.TodoEntity
import com.example.todoapplication.domain.model.Category

/**
 * TODO, Category, Reminder를 보관하는 version 2 Room database.
 *
 * 최초 생성 callback에서 시스템 Category `일반`을 중복 없이 seed한다. DAO와 schema만
 * 노출하며 destructive migration 정책은 사용하지 않는다.
 */
@Database(
    entities = [TodoEntity::class, CategoryEntity::class, ReminderEntity::class],
    version = 2,
    exportSchema = true,
)
@TypeConverters(RoomConverters::class)
abstract class TodoDatabase : RoomDatabase() {
    abstract fun todoDao(): TodoDao

    abstract fun categoryDao(): CategoryDao

    abstract fun reminderDao(): ReminderDao

    companion object {
        const val DATABASE_NAME = "todo.db"

        /**
         * Version 1 -> 2 Migration:
         * 기존 Reminder 데이터(id, todo_id, minutes_before)를 모두 보존하면서
         * 동일 TODO 내 중복 offset 방지를 위한 `(todo_id, minutes_before)` 고유 인덱스를 추가한다.
         */
        val MIGRATION_1_2: Migration = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    DELETE FROM `reminders`
                    WHERE `id` NOT IN (
                        SELECT MIN(`id`)
                        FROM `reminders`
                        GROUP BY `todo_id`, `minutes_before`
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    """
                    CREATE UNIQUE INDEX IF NOT EXISTS `index_reminders_todo_id_minutes_before`
                    ON `reminders` (`todo_id`, `minutes_before`)
                    """.trimIndent(),
                )
            }
        }

        /** 시스템 Category 생성 callback과 비파괴 Migration을 포함한 영속 database를 만든다. */
        fun create(context: Context, name: String = DATABASE_NAME): TodoDatabase =
            Room.databaseBuilder(context, TodoDatabase::class.java, name)
                .addMigrations(MIGRATION_1_2)
                .addCallback(systemCategoryCallback)
                .build()

        private val systemCategoryCallback = object : Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                db.execSQL(
                    """
                    INSERT OR IGNORE INTO categories (
                        name, color, sort_order, is_system, created_at_epoch_millis
                    ) VALUES (?, ?, ?, ?, ?)
                    """.trimIndent(),
                    arrayOf<Any>(
                        Category.SYSTEM_DEFAULT_NAME,
                        Category.SYSTEM_DEFAULT_COLOR.storageValue,
                        Category.SYSTEM_DEFAULT_SORT_ORDER,
                        1,
                        System.currentTimeMillis(),
                    ),
                )
            }
        }
    }
}
