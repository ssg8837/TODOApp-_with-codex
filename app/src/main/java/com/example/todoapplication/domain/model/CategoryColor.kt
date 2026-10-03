package com.example.todoapplication.domain.model

/**
 * Category가 사용할 수 있는 사전 정의 색상의 의미 값.
 *
 * Compose `Color`와 분리되어 있으며 [storageValue]를 통해 Room에 안정적으로 저장된다.
 */
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
        /** 저장 문자열을 대응하는 색상으로 복원하며 알 수 없는 값은 허용하지 않는다. */
        fun fromStorageValue(value: String): CategoryColor =
            entries.firstOrNull { it.storageValue == value }
                ?: throw IllegalArgumentException("Unknown CategoryColor value: $value")
    }
}
