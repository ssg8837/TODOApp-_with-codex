package com.example.todoapplication.domain.model

/**
 * TODO 시각보다 [minutesBefore]분 먼저 알릴 설정을 나타내는 도메인 모델.
 *
 * [todoId]는 유효한 TODO를 가리키며 MVP에서 지원하는 간격은 [SUPPORTED_MINUTES_BEFORE]로
 * 제한된다. 실제 발화 시각 계산과 OS Alarm 예약은 이 모델의 책임이 아니다.
 */
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
