package com.example.todoapplication.domain.reminder

import com.example.todoapplication.domain.model.Reminder

/**
 * 분 단위 알림 간격(`offsetMinutes`)을 일/시간/분 단위로 분해한 순수 모델.
 */
data class ReminderOffsetParts(
    val days: Int,
    val hours: Int,
    val minutes: Int,
)

/**
 * `offsetMinutes`와 사용자 입력(일/시간/분) 및 표시용 문자열 간의 순수 변환을 담당한다.
 *
 * Android Context나 리소스에 의존하지 않으며 UI, Detail, Notification에서 일관된 규칙으로 사용한다.
 */
object ReminderDurationFormatter {
    private const val MINUTES_PER_HOUR = 60
    private const val MINUTES_PER_DAY = 1_440

    /** 양의 분 단위 offset을 일, 시간, 분 단위로 분해한다. */
    fun decompose(offsetMinutes: Int): ReminderOffsetParts {
        require(offsetMinutes > 0) { "offsetMinutes must be positive: $offsetMinutes" }
        val days = offsetMinutes / MINUTES_PER_DAY
        val hours = (offsetMinutes % MINUTES_PER_DAY) / MINUTES_PER_HOUR
        val minutes = offsetMinutes % MINUTES_PER_HOUR
        return ReminderOffsetParts(days = days, hours = hours, minutes = minutes)
    }

    /**
     * 일/시간/분 입력을 분 단위 offset으로 합산한다.
     *
     * 음수 입력, 0분 합계, 산술 오버플로 또는 [Reminder.MAX_MINUTES_BEFORE] 초과는 `null`을 반환한다.
     */
    fun toOffsetMinutes(days: Int, hours: Int, minutes: Int): Int? {
        if (days < 0 || hours < 0 || minutes < 0) return null
        val total = days.toLong() * MINUTES_PER_DAY.toLong() +
            hours.toLong() * MINUTES_PER_HOUR.toLong() +
            minutes.toLong()
        if (total <= 0L || total > Reminder.MAX_MINUTES_BEFORE.toLong()) return null
        return total.toInt()
    }

    /**
     * 문자열 입력(일/시간/분)을 검증하여 유효한 분 단위 offset으로 변환한다.
     *
     * 빈 문자열은 0으로 취급하되 세 입력이 모두 비어 있거나 숫자가 아닌 값, 음수, 0분 합계,
     * 오버플로 입력은 `null`을 반환한다.
     */
    fun parseCustomOffsetMinutes(
        daysInput: String,
        hoursInput: String,
        minutesInput: String,
    ): Int? {
        val trimmedDays = daysInput.trim()
        val trimmedHours = hoursInput.trim()
        val trimmedMinutes = minutesInput.trim()
        if (trimmedDays.isEmpty() && trimmedHours.isEmpty() && trimmedMinutes.isEmpty()) {
            return null
        }
        val days = if (trimmedDays.isEmpty()) 0 else trimmedDays.toIntOrNull() ?: return null
        val hours = if (trimmedHours.isEmpty()) 0 else trimmedHours.toIntOrNull() ?: return null
        val minutes = if (trimmedMinutes.isEmpty()) 0 else trimmedMinutes.toIntOrNull() ?: return null
        return toOffsetMinutes(days = days, hours = hours, minutes = minutes)
    }

    /**
     * 0인 단위를 생략한 순수 기간 문자열을 반환한다.
     *
     * 예: `15 -> "15분"`, `60 -> "1시간"`, `90 -> "1시간 30분"`, `1440 -> "1일"`, `1500 -> "1일 1시간"`.
     */
    fun formatDuration(offsetMinutes: Int): String {
        val parts = decompose(offsetMinutes)
        return buildList {
            if (parts.days > 0) add("${parts.days}일")
            if (parts.hours > 0) add("${parts.hours}시간")
            if (parts.minutes > 0) add("${parts.minutes}분")
        }.joinToString(separator = " ")
    }

    /**
     * 상대 알림 시점 문자열을 반환한다.
     *
     * 예: `15 -> "15분 전"`, `60 -> "1시간 전"`, `150 -> "2시간 30분 전"`, `1440 -> "1일 전"`.
     */
    fun formatRelativeText(offsetMinutes: Int): String = "${formatDuration(offsetMinutes)} 전"
}

