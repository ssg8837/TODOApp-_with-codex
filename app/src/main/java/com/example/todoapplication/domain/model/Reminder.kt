package com.example.todoapplication.domain.model

data class Reminder(
    val id: Long,
    val todoId: Long,
    val minutesBefore: Int,
) {
    companion object {
        const val FIFTEEN_MINUTES_BEFORE = 15
        const val ONE_DAY_BEFORE = 1_440
        val SUPPORTED_MINUTES_BEFORE = setOf(
            FIFTEEN_MINUTES_BEFORE,
            ONE_DAY_BEFORE,
        )
    }
}
