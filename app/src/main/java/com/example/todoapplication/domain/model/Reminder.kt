package com.example.todoapplication.domain.model

/**
 * TODO 시각보다 [minutesBefore]분 먼저 알릴 설정을 나타내는 도메인 모델.
 *
 * [todoId]는 유효한 TODO를 가리키며 [minutesBefore]는 0보다 큰 양의 정수 분 단위 offset이다.
 * 기본 프리셋 간격은 [PRESET_MINUTES_BEFORE]로 제공되며 사용자 지정 간격도 동일하게 표현한다.
 * 실제 발화 시각 계산과 OS Alarm 예약은 이 모델의 책임이 아니다.
 */
data class Reminder(
    val id: Long,
    val todoId: Long,
    val minutesBefore: Int,
) {
    companion object {
        const val FIFTEEN_MINUTES_BEFORE = 15
        const val ONE_HOUR_BEFORE = 60
        const val ONE_DAY_BEFORE = 1_440

        /** 초 단위 변환(`minutesBefore * 60`) 및 분해 계산에서 산술 오버플로를 방지하는 기술적 상한. */
        const val MAX_MINUTES_BEFORE = Int.MAX_VALUE / 60

        val PRESET_MINUTES_BEFORE = setOf(
            FIFTEEN_MINUTES_BEFORE,
            ONE_HOUR_BEFORE,
            ONE_DAY_BEFORE,
        )

        val SUPPORTED_MINUTES_BEFORE = PRESET_MINUTES_BEFORE
    }
}
