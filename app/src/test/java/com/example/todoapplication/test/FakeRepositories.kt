package com.example.todoapplication.test

import com.example.todoapplication.domain.model.Category
import com.example.todoapplication.domain.model.Reminder
import com.example.todoapplication.domain.model.Todo
import com.example.todoapplication.domain.repository.CategoryRepository
import com.example.todoapplication.domain.repository.DataAccessException
import com.example.todoapplication.domain.repository.ReminderRepository
import com.example.todoapplication.domain.repository.TodoRepository
import java.time.LocalDate
import java.time.LocalTime
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class FakeTodoRepository(
    initialTodos: List<Todo> = emptyList(),
) : TodoRepository {
    private val todos = MutableStateFlow(initialTodos)
    var writeCount = 0
        private set
    var failWithPersistenceError = false
    val calls = mutableListOf<String>()

    override suspend fun create(todo: Todo): Todo {
        beforeWrite("create")
        val saved = todo.copy(id = nextId())
        todos.value += saved
        return saved
    }

    override suspend fun update(todo: Todo): Boolean {
        beforeWrite("update")
        val index = todos.value.indexOfFirst { it.id == todo.id }
        if (index < 0) return false
        todos.value = todos.value.toMutableList().apply { set(index, todo) }
        return true
    }

    override suspend fun delete(todo: Todo): Boolean {
        beforeWrite("delete")
        val removed = todos.value.any { it.id == todo.id }
        todos.value = todos.value.filterNot { it.id == todo.id }
        return removed
    }

    override suspend fun getById(id: Long): Todo? {
        calls += "getById"
        return todos.value.firstOrNull { it.id == id }
    }

    override fun observeByDate(date: LocalDate): Flow<List<Todo>> {
        calls += "observeByDate"
        return todos.map { values -> values.filter { it.date == date } }
    }

    override fun observeFiltered(
        date: LocalDate,
        categoryId: Long?,
        incompleteOnly: Boolean,
    ): Flow<List<Todo>> = todos.map { values ->
        values.filter { todo ->
            todo.date == date &&
                (categoryId == null || todo.categoryId == categoryId) &&
                (!incompleteOnly || !todo.isCompleted)
        }
    }

    private fun beforeWrite(call: String) {
        calls += call
        writeCount += 1
        if (failWithPersistenceError) throw DataAccessException(IllegalStateException("fake"))
    }

    private fun nextId(): Long = (todos.value.maxOfOrNull(Todo::id) ?: 0L) + 1L
}

class FakeCategoryRepository(
    initialCategories: List<Category> = emptyList(),
) : CategoryRepository {
    private val categories = MutableStateFlow(initialCategories)
    var writeCount = 0
        private set
    var savedOrder: List<Long>? = null
        private set
    val calls = mutableListOf<String>()

    override suspend fun create(category: Category): Category {
        calls += "create"
        writeCount += 1
        val saved = category.copy(id = nextId())
        categories.value += saved
        return saved
    }

    override suspend fun update(category: Category): Boolean {
        calls += "update"
        writeCount += 1
        val index = categories.value.indexOfFirst { it.id == category.id }
        if (index < 0) return false
        categories.value = categories.value.toMutableList().apply { set(index, category) }
        return true
    }

    override suspend fun deleteCategory(categoryId: Long): Boolean {
        calls += "deleteCategory"
        writeCount += 1
        val removed = categories.value.any { it.id == categoryId }
        categories.value = categories.value.filterNot { it.id == categoryId }
        return removed
    }

    override fun observeAll(): Flow<List<Category>> {
        calls += "observeAll"
        return categories
    }

    override suspend fun getById(id: Long): Category? {
        calls += "getById"
        return categories.value.firstOrNull { it.id == id }
    }

    override suspend fun getByName(name: String): Category? =
        categories.value.firstOrNull { it.name == name }

    override suspend fun getSystemCategory(): Category? =
        categories.value.firstOrNull(Category::isSystem)

    override suspend fun isNameTaken(name: String): Boolean {
        calls += "isNameTaken"
        return categories.value.any { it.name == name }
    }

    override suspend fun getNextUserSortOrder(): Int {
        calls += "getNextUserSortOrder"
        return (categories.value.filterNot(Category::isSystem).maxOfOrNull(Category::sortOrder) ?: 0) + 1
    }

    override suspend fun saveUserCategoryOrder(categoryIds: List<Long>) {
        calls += "saveUserCategoryOrder"
        writeCount += 1
        savedOrder = categoryIds
        val orders = categoryIds.withIndex().associate { (index, id) -> id to index + 1 }
        categories.value = categories.value.map { category ->
            orders[category.id]?.let { category.copy(sortOrder = it) } ?: category
        }
    }

    private fun nextId(): Long = (categories.value.maxOfOrNull(Category::id) ?: 0L) + 1L
}

class FakeReminderRepository(
    initialReminders: List<Reminder> = emptyList(),
) : ReminderRepository {
    private val reminders = MutableStateFlow(initialReminders)
    var writeCount = 0
        private set
    val calls = mutableListOf<String>()
    var recoveryCandidates: List<Reminder> = emptyList()
    var replaceCallCount = 0
        private set
    var failReplaceWithPersistenceError = false

    override suspend fun create(reminder: Reminder): Reminder {
        calls += "create"
        writeCount += 1
        val saved = reminder.copy(id = nextId())
        reminders.value += saved
        return saved
    }

    override fun observeByTodoId(todoId: Long): Flow<List<Reminder>> =
        reminders.map { values -> values.filter { it.todoId == todoId } }

    override suspend fun deleteById(id: Long): Boolean {
        calls += "deleteById"
        writeCount += 1
        val removed = reminders.value.any { it.id == id }
        reminders.value = reminders.value.filterNot { it.id == id }
        return removed
    }

    override suspend fun deleteByTodoId(todoId: Long): Int {
        calls += "deleteByTodoId"
        writeCount += 1
        val count = reminders.value.count { it.todoId == todoId }
        reminders.value = reminders.value.filterNot { it.todoId == todoId }
        return count
    }

    override suspend fun replaceReminders(
        todoId: Long,
        reminders: List<Reminder>,
    ): List<Reminder> {
        calls += "replaceReminders"
        writeCount += 1
        replaceCallCount += 1
        if (failReplaceWithPersistenceError) {
            throw DataAccessException(IllegalStateException("fake"))
        }
        this.reminders.value = this.reminders.value.filterNot { it.todoId == todoId }
        return reminders.map { reminder ->
            reminder.copy(id = nextId()).also { saved ->
                this.reminders.value += saved
            }
        }
    }

    override suspend fun getFutureAlarmRecoveryCandidates(
        currentDate: LocalDate,
        currentTime: LocalTime,
    ): List<Reminder> {
        calls += "getFutureAlarmRecoveryCandidates"
        return recoveryCandidates
    }

    private fun nextId(): Long = (reminders.value.maxOfOrNull(Reminder::id) ?: 0L) + 1L
}
