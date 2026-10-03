package com.example.todoapplication.domain.model

import java.time.Instant

/**
 * TODO를 분류하고 이름과 표시 색상, 사용자 지정 순서를 보존하는 도메인 모델.
 *
 * 시스템 Category인 `일반`은 [isSystem]이 `true`이고 이름·색상·정렬 순서가 companion의
 * 기본값으로 고정된다. 사용자 Category는 1 이상의 [sortOrder]를 가진다.
 */
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
