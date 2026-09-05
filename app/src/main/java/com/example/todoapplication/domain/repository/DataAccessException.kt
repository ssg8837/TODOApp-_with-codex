package com.example.todoapplication.domain.repository

class DataAccessException internal constructor(
    cause: Throwable,
) : RuntimeException("Data operation failed", cause)
