package com.example.todoapplication.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Category를 `categories` 테이블에 저장하는 Room Entity.
 *
 * 이름의 unique index가 중복을 최종 방지하고 색상은 Compose 타입이 아닌 안정적인 문자열로
 * 저장된다. 시스템 여부와 정렬 순서는 도메인 정책을 영속화한다.
 */
@Entity(
    tableName = "categories",
    indices = [
        Index(value = ["name"], unique = true),
        Index(value = ["sort_order"]),
    ],
)
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    @ColumnInfo(name = "color")
    val colorValue: String,
    @ColumnInfo(name = "sort_order")
    val sortOrder: Int,
    @ColumnInfo(name = "is_system")
    val isSystem: Boolean,
    @ColumnInfo(name = "created_at_epoch_millis")
    val createdAtEpochMillis: Long,
)
