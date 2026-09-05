package com.example.todoapplication.domain.model

enum class CategoryColor(val storageValue: String) {
    NEUTRAL("NEUTRAL"),
    RED("RED"),
    ORANGE("ORANGE"),
    YELLOW("YELLOW"),
    GREEN("GREEN"),
    BLUE("BLUE"),
    PURPLE("PURPLE"),
    PINK("PINK");

    companion object {
        fun fromStorageValue(value: String): CategoryColor =
            entries.firstOrNull { it.storageValue == value }
                ?: throw IllegalArgumentException("Unknown CategoryColor value: $value")
    }
}
