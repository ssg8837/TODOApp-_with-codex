package com.example.todoapplication.application.service

import com.example.todoapplication.domain.repository.DataAccessException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

/** 데이터 계층 오류를 Service의 persistence 실패로 변환하고 Coroutine 취소는 전파한다. */
internal suspend inline fun <T> serviceCall(
    crossinline block: suspend () -> ServiceResult<T>,
): ServiceResult<T> = try {
    block()
} catch (exception: CancellationException) {
    throw exception
} catch (_: DataAccessException) {
    ServiceResult.Failure(ServiceError.PersistenceFailure)
}

/** Repository Flow 값을 성공 결과로 감싸고 데이터 오류를 Service 오류로 변환한다. */
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
