package com.example.todoapplication.notification

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test

class NotificationChannelsTest {
    @Test
    fun channelIdIsMigratedToV2() {
        assertEquals("todo_reminders_v2", NotificationChannels.TODO_REMINDERS)
        assertEquals("todo_reminders", NotificationChannels.LEGACY_TODO_REMINDERS)
    }

    @Test
    fun defaultVibrationPatternMatchesSpecification() {
        val expected = longArrayOf(0L, 250L, 150L, 250L)
        assertArrayEquals(expected, NotificationChannels.DEFAULT_VIBRATION_PATTERN)
    }
}

