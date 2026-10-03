package com.example.todoapplication.data.repository

import com.example.todoapplication.domain.repository.DataAccessException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch

/** Coroutine 취소는 보존하고 나머지 persistence 예외를 데이터 접근 오류로 변환한다. */
internal suspend inline fun <T> dataAccess(crossinline block: suspend () -> T): T = try {
    block()
} catch (exception: CancellationException) {
    throw exception
} catch (exception: DataAccessException) {
    throw exception
} catch (exception: Exception) {
    throw DataAccessException(exception)
}

/** Flow 수집 중 발생한 persistence 예외에도 동일한 데이터 오류 경계를 적용한다. */
internal fun <T> Flow<T>.mapDataAccessErrors(): Flow<T> = catch { exception ->
    when (exception) {
        is CancellationException -> throw exception
        is DataAccessException -> throw exception
        else -> throw DataAccessException(exception)
    }
}
