package com.example.todoapplication.application.service

import com.example.todoapplication.domain.repository.DataAccessException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

internal suspend inline fun <T> serviceCall(
    crossinline block: suspend () -> ServiceResult<T>,
): ServiceResult<T> = try {
    block()
} catch (exception: CancellationException) {
    throw exception
} catch (_: DataAccessException) {
    ServiceResult.Failure(ServiceError.PersistenceFailure)
}

internal fun <T> Flow<T>.asServiceResult(): Flow<ServiceResult<T>> =
    map<T, ServiceResult<T>> { ServiceResult.Success(it) }
        .catch { exception ->
            when (exception) {
                is CancellationException -> throw exception
                is DataAccessException -> emit(
                    ServiceResult.Failure(ServiceError.PersistenceFailure),
                )
                else -> throw exception
            }
        }
