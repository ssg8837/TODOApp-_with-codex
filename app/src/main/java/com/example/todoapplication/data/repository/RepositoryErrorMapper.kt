package com.example.todoapplication.data.repository

import com.example.todoapplication.domain.repository.DataAccessException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch

internal suspend inline fun <T> dataAccess(crossinline block: suspend () -> T): T = try {
    block()
} catch (exception: CancellationException) {
    throw exception
} catch (exception: DataAccessException) {
    throw exception
} catch (exception: Exception) {
    throw DataAccessException(exception)
}

internal fun <T> Flow<T>.mapDataAccessErrors(): Flow<T> = catch { exception ->
    when (exception) {
        is CancellationException -> throw exception
        is DataAccessException -> throw exception
        else -> throw DataAccessException(exception)
    }
}
