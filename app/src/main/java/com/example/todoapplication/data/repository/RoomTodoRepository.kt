package com.example.todoapplication.data.repository

import com.example.todoapplication.data.local.dao.TodoDao
import com.example.todoapplication.data.mapper.TodoMapper
import com.example.todoapplication.domain.model.Todo
import com.example.todoapplication.domain.repository.TodoRepository
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * [TodoRepository]의 Room 구현.
 *
 * DAO의 Entity를 [TodoMapper]로 Domain 모델에 변환하고 Room/SQLite 오류를
 * [com.example.todoapplication.domain.repository.DataAccessException]으로 경계화한다.
 * Android UI와 비즈니스 검증에는 의존하지 않는다.
 */
internal class RoomTodoRepository(
    private val todoDao: TodoDao,
) : TodoRepository {
    override suspend fun create(todo: Todo): Todo = dataAccess {
        val id = todoDao.insert(TodoMapper.toEntity(todo))
        todo.copy(id = id)
    }

    override suspend fun update(todo: Todo): Boolean = dataAccess {
        todoDao.update(TodoMapper.toEntity(todo)) == 1
    }

    override suspend fun delete(todo: Todo): Boolean = dataAccess {
        todoDao.delete(TodoMapper.toEntity(todo)) == 1
    }

    override suspend fun getById(id: Long): Todo? = dataAccess {
        todoDao.getById(id)?.let(TodoMapper::toDomain)
    }

    override fun observeByDate(date: LocalDate): Flow<List<Todo>> =
        todoDao.observeByDate(date.toEpochDay())
            .map { entities -> entities.map(TodoMapper::toDomain) }
            .mapDataAccessErrors()

    override fun observeFiltered(
        date: LocalDate,
        categoryId: Long?,
        incompleteOnly: Boolean,
    ): Flow<List<Todo>> = todoDao.observeFiltered(
        dateEpochDay = date.toEpochDay(),
        categoryId = categoryId,
        incompleteOnly = incompleteOnly,
    ).map { entities -> entities.map(TodoMapper::toDomain) }
        .mapDataAccessErrors()
}
