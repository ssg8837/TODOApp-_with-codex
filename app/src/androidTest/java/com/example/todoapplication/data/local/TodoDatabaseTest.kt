package com.example.todoapplication.data.local

import android.database.sqlite.SQLiteDatabase
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.todoapplication.data.local.entity.ReminderEntity
import com.example.todoapplication.domain.model.Category
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TodoDatabaseTest {
    private val databaseName = "todo-database-test.db"
    private lateinit var database: TodoDatabase
    private lateinit var context: android.content.Context

    @Before
    fun setUp() {
        val created = createTestDatabase(databaseName)
        context = created.first
        database = created.second
    }

    @After
    fun tearDown() {
        database.close()
        context.deleteDatabase(databaseName)
    }

    @Test
    fun firstCreationSeedsExactlyOneSystemCategoryWithoutAssumingItsId() = runBlocking {
        val systemCategory = database.categoryDao().getSystemCategory()

        assertNotNull(systemCategory)
        assertNotEquals(0L, systemCategory!!.id)
        assertEquals(Category.SYSTEM_DEFAULT_NAME, systemCategory.name)
        assertEquals(Category.SYSTEM_DEFAULT_COLOR.storageValue, systemCategory.colorValue)
        assertEquals(Category.SYSTEM_DEFAULT_SORT_ORDER, systemCategory.sortOrder)
        assertEquals(1, database.categoryDao().countSystemCategories())
        assertEquals(1, database.categoryDao().countByName(Category.SYSTEM_DEFAULT_NAME))

        database.close()
        database = TodoDatabase.create(context, databaseName)
        database.openHelper.writableDatabase

        assertEquals(1, database.categoryDao().countSystemCategories())
        assertEquals(1, database.categoryDao().countByName(Category.SYSTEM_DEFAULT_NAME))
    }

    @Test
    fun migrationFrom1To2PreservesExistingTodosAndRemindersAndAddsUniqueIndex() = runBlocking {
        val migrationDbName = "todo-migration-1-to-2-test.db"
        context.deleteDatabase(migrationDbName)
        val dbFile = context.getDatabasePath(migrationDbName)
        dbFile.parentFile?.mkdirs()

        // 1. Version 1 DB 생성 및 기존 1440 / 15 Reminder 데이터 삽입
        val v1Db = SQLiteDatabase.openOrCreateDatabase(dbFile, null)
        try {
            v1Db.execSQL("PRAGMA foreign_keys = ON")
            v1Db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `categories` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `name` TEXT NOT NULL,
                    `color` TEXT NOT NULL,
                    `sort_order` INTEGER NOT NULL,
                    `is_system` INTEGER NOT NULL,
                    `created_at_epoch_millis` INTEGER NOT NULL
                )
                """.trimIndent(),
            )
            v1Db.execSQL(
                "CREATE UNIQUE INDEX IF NOT EXISTS `index_categories_name` ON `categories` (`name`)",
            )
            v1Db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_categories_sort_order` ON `categories` (`sort_order`)",
            )
            v1Db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `todos` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `title` TEXT NOT NULL,
                    `date_epoch_day` INTEGER NOT NULL,
                    `time_minute_of_day` INTEGER,
                    `category_id` INTEGER NOT NULL,
                    `is_completed` INTEGER NOT NULL,
                    `created_at_epoch_millis` INTEGER NOT NULL,
                    `updated_at_epoch_millis` INTEGER NOT NULL,
                    FOREIGN KEY(`category_id`) REFERENCES `categories`(`id`) ON UPDATE NO ACTION ON DELETE NO ACTION
                )
                """.trimIndent(),
            )
            v1Db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_todos_category_id` ON `todos` (`category_id`)",
            )
            v1Db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `reminders` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `todo_id` INTEGER NOT NULL,
                    `minutes_before` INTEGER NOT NULL,
                    FOREIGN KEY(`todo_id`) REFERENCES `todos`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                )
                """.trimIndent(),
            )
            v1Db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_reminders_todo_id` ON `reminders` (`todo_id`)",
            )
            v1Db.execSQL(
                "CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)",
            )
            v1Db.execSQL(
                "INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, 'f1ea9aba64d4f01c7f9bcbae10d5d455')",
            )
            v1Db.execSQL(
                """
                INSERT INTO `categories` (`id`, `name`, `color`, `sort_order`, `is_system`, `created_at_epoch_millis`)
                VALUES (1, '일반', 'NEUTRAL', 0, 1, 1000)
                """.trimIndent(),
            )
            v1Db.execSQL(
                """
                INSERT INTO `todos` (`id`, `title`, `date_epoch_day`, `time_minute_of_day`, `category_id`, `is_completed`, `created_at_epoch_millis`, `updated_at_epoch_millis`)
                VALUES (10, '기존 일정', 20500, 720, 1, 0, 1000, 1000)
                """.trimIndent(),
            )
            v1Db.execSQL(
                "INSERT INTO `reminders` (`id`, `todo_id`, `minutes_before`) VALUES (101, 10, 1440)",
            )
            v1Db.execSQL(
                "INSERT INTO `reminders` (`id`, `todo_id`, `minutes_before`) VALUES (102, 10, 15)",
            )
            v1Db.version = 1
        } finally {
            v1Db.close()
        }

        // 2. Version 2 Room Database로 열기 (MIGRATION_1_2 수행 및 스키마 검증)
        val migratedDb = TodoDatabase.create(context, migrationDbName)
        try {
            val migratedTodo = migratedDb.todoDao().getById(10L)
            assertNotNull(migratedTodo)
            assertEquals("기존 일정", migratedTodo!!.title)
            assertEquals(720, migratedTodo.timeMinuteOfDay)

            val reminders = migratedDb.reminderDao().observeByTodoId(10L).first()
            assertEquals(
                listOf(
                    ReminderEntity(id = 102L, todoId = 10L, minutesBefore = 15),
                    ReminderEntity(id = 101L, todoId = 10L, minutesBefore = 1_440),
                ),
                reminders,
            )

            // 3. 사용자 지정 offset(150분) 추가 가능 확인
            val customId = migratedDb.reminderDao().insert(
                ReminderEntity(todoId = 10L, minutesBefore = 150),
            )
            assertTrue(customId > 0L)
            assertEquals(
                listOf(15, 150, 1_440),
                migratedDb.reminderDao().observeByTodoId(10L).first().map { it.minutesBefore },
            )

            // 4. 동일 TODO 내 중복 offset(15분) 삽입 시 고유 인덱스에 의해 차단되는지 확인
            try {
                migratedDb.reminderDao().insert(
                    ReminderEntity(todoId = 10L, minutesBefore = 15),
                )
                fail("Duplicate (todo_id, minutes_before) must be rejected by unique index")
            } catch (_: Exception) {
                // Expected unique constraint violation
            }

            // 5. FK Cascade 유지 확인
            migratedDb.todoDao().delete(migratedTodo)
            assertTrue(migratedDb.reminderDao().observeByTodoId(10L).first().isEmpty())
        } finally {
            migratedDb.close()
            context.deleteDatabase(migrationDbName)
        }
    }
}
