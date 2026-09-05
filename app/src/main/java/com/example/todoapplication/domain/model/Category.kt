package com.example.todoapplication.domain.model

import java.time.Instant

data class Category(
    val id: Long,
    val name: String,
    val color: CategoryColor,
    val sortOrder: Int,
    val isSystem: Boolean,
    val createdAt: Instant,
) {
    companion object {
        const val SYSTEM_DEFAULT_NAME = "일반"
        const val SYSTEM_DEFAULT_SORT_ORDER = 0
        val SYSTEM_DEFAULT_COLOR = CategoryColor.NEUTRAL
    }
}
