package com.example.todoapplication.domain.reminder

import com.example.todoapplication.domain.model.Reminder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ReminderDurationFormatterTest {
    @Test
    fun decomposesOffsetsIntoDaysHoursAndMinutes() {
        assertEquals(ReminderOffsetParts(0, 0, 15), ReminderDurationFormatter.decompose(15))
        assertEquals(ReminderOffsetParts(0, 1, 0), ReminderDurationFormatter.decompose(60))
        assertEquals(ReminderOffsetParts(0, 2, 30), ReminderDurationFormatter.decompose(150))
        assertEquals(ReminderOffsetParts(1, 0, 0), ReminderDurationFormatter.decompose(1_440))
        assertEquals(ReminderOffsetParts(1, 1, 30), ReminderDurationFormatter.decompose(1_530))
        assertEquals(ReminderOffsetParts(3, 4, 0), ReminderDurationFormatter.decompose(4_560))
    }

    @Test
    fun formatsDurationAndRelativeTextOmittingZeroUnits() {
        assertEquals("15분", ReminderDurationFormatter.formatDuration(15))
        assertEquals("15분 전", ReminderDurationFormatter.formatRelativeText(15))

        assertEquals("1시간", ReminderDurationFormatter.formatDuration(60))
        assertEquals("1시간 전", ReminderDurationFormatter.formatRelativeText(60))

        assertEquals("1시간 30분", ReminderDurationFormatter.formatDuration(90))
        assertEquals("1시간 30분 전", ReminderDurationFormatter.formatRelativeText(90))

        assertEquals("2시간 30분", ReminderDurationFormatter.formatDuration(150))
        assertEquals("2시간 30분 전", ReminderDurationFormatter.formatRelativeText(150))

        assertEquals("1일", ReminderDurationFormatter.formatDuration(1_440))
        assertEquals("1일 전", ReminderDurationFormatter.formatRelativeText(1_440))

        assertEquals("1일 1시간", ReminderDurationFormatter.formatDuration(1_500))
        assertEquals("1일 1시간 전", ReminderDurationFormatter.formatRelativeText(1_500))

        assertEquals("1일 1시간 30분", ReminderDurationFormatter.formatDuration(1_530))
        assertEquals("1일 1시간 30분 전", ReminderDurationFormatter.formatRelativeText(1_530))

        assertEquals("3일", ReminderDurationFormatter.formatDuration(4_320))
        assertEquals("3일 전", ReminderDurationFormatter.formatRelativeText(4_320))

        assertEquals("3일 4시간", ReminderDurationFormatter.formatDuration(4_560))
        assertEquals("3일 4시간 전", ReminderDurationFormatter.formatRelativeText(4_560))
    }

    @Test
    fun parsesValidCustomInputsIncludingMultiWeekOffsets() {
        assertEquals(30, ReminderDurationFormatter.parseCustomOffsetMinutes("", "", "30"))
        assertEquals(150, ReminderDurationFormatter.parseCustomOffsetMinutes("0", "2", "30"))
        assertEquals(150, ReminderDurationFormatter.parseCustomOffsetMinutes("", "2", "30"))
        assertEquals(4_320, ReminderDurationFormatter.parseCustomOffsetMinutes("3", "0", "0"))
        assertEquals(10_080, ReminderDurationFormatter.parseCustomOffsetMinutes("7", "", ""))
        assertEquals(43_200, ReminderDurationFormatter.parseCustomOffsetMinutes("30", "", ""))
    }

    @Test
    fun rejectsZeroNegativeNonNumericAndOverflowInputs() {
        assertNull(ReminderDurationFormatter.parseCustomOffsetMinutes("", "", ""))
        assertNull(ReminderDurationFormatter.parseCustomOffsetMinutes("0", "0", "0"))
        assertNull(ReminderDurationFormatter.parseCustomOffsetMinutes("-1", "0", "0"))
        assertNull(ReminderDurationFormatter.parseCustomOffsetMinutes("0", "-2", "30"))
        assertNull(ReminderDurationFormatter.parseCustomOffsetMinutes("0", "0", "-5"))
        assertNull(ReminderDurationFormatter.parseCustomOffsetMinutes("abc", "1", "0"))
        assertNull(ReminderDurationFormatter.parseCustomOffsetMinutes("0", "1.5", "0"))
        assertNull(ReminderDurationFormatter.toOffsetMinutes(Int.MAX_VALUE, 0, 0))
        assertNull(ReminderDurationFormatter.toOffsetMinutes(0, 0, Reminder.MAX_MINUTES_BEFORE + 1))
    }
}

