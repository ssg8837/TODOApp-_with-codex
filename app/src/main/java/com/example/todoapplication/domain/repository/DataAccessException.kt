package com.example.todoapplication.domain.repository

/** Room/SQLite 구현 예외가 상위 계층으로 직접 새지 않도록 감싸는 데이터 접근 오류. */
class DataAccessException internal constructor(
    cause: Throwable,
) : RuntimeException("Data operation failed", cause)
