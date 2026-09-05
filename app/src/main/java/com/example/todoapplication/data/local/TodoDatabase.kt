package com.example.todoapplication.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.todoapplication.data.local.dao.CategoryDao
import com.example.todoapplication.data.local.dao.ReminderDao
import com.example.todoapplication.data.local.dao.TodoDao
import com.example.todoapplication.data.local.entity.CategoryEntity
import com.example.todoapplication.data.local.entity.ReminderEntity
import com.example.todoapplication.data.local.entity.TodoEntity
import com.example.todoapplication.domain.model.Category

@Database(
    entities = [TodoEntity::class, CategoryEntity::class, ReminderEntity::class],
    version = 1,
    exportSchema = true,
)
@TypeConverters(RoomConverters::class)
abstract class TodoDatabase : RoomDatabase() {
    abstract fun todoDao(): TodoDao

    abstract fun categoryDao(): CategoryDao

    abstract fun reminderDao(): ReminderDao

    companion object {
        const val DATABASE_NAME = "todo.db"

        fun create(context: Context, name: String = DATABASE_NAME): TodoDatabase =
            Room.databaseBuilder(context, TodoDatabase::class.java, name)
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
