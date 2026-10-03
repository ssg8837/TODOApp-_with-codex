package com.example.todoapplication.domain.reminder

import com.example.todoapplication.domain.model.Reminder
import com.example.todoapplication.domain.model.Todo
import java.time.Instant
import java.time.ZoneId

/**
 * TODO의 로컬 날짜·시간과 Reminder 간격을 실제 Alarm 발화 [Instant]로 변환한다.
 *
 * [ZoneId]를 명시적으로 받아 로컬 시간을 절대 시각으로 해석한다. 시간이 없는 TODO에는
 * `null`을 반환하며, 계산 결과가 과거인지 판단하는 일은 호출자의 책임이다.
 */
object ReminderCalculator {
    /** [todo]의 시각에서 [reminder] 간격을 뺀 발화 시각을 계산한다. */
    fun scheduledAt(
        todo: Todo,
        reminder: Reminder,
        zoneId: ZoneId,
    ): Instant? = todo.time
        ?.let { time -> todo.date.atTime(time).atZone(zoneId).toInstant() }
        ?.minusSeconds(reminder.minutesBefore * SECONDS_PER_MINUTE)

    private const val SECONDS_PER_MINUTE = 60L
}
