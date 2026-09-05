package com.example.todoapplication.data.local

import com.example.todoapplication.domain.model.CategoryColor
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RoomConvertersTest {
    private val converters = RoomConverters()

    @Test
    fun instantConvertsToEpochMillisAndBack() {
        val instant = Instant.parse("2026-09-05T12:34:56.789Z")

        val epochMillis = converters.instantToEpochMillis(instant)

        assertEquals(instant.toEpochMilli(), epochMillis)
        assertEquals(instant, converters.epochMillisToInstant(epochMillis))
    }

    @Test
    fun localDateConvertsToEpochDayAndBack() {
        val date = LocalDate.of(2026, 9, 5)

        val epochDay = converters.localDateToEpochDay(date)

        assertEquals(date, converters.epochDayToLocalDate(epochDay))
    }

    @Test
    fun nullableTimeConvertsToNullStorageValueAndBack() {
        assertNull(converters.localTimeToMinuteOfDay(null))
        assertNull(converters.minuteOfDayToLocalTime(null))
    }

    @Test
    fun localTimeConvertsToMinuteOfDayAndBack() {
        val time = LocalTime.of(14, 35)

        val minuteOfDay = converters.localTimeToMinuteOfDay(time)

        assertEquals(875, minuteOfDay)
        assertEquals(time, converters.minuteOfDayToLocalTime(minuteOfDay))
    }

    @Test
    fun everyCategoryColorConvertsToStableValueAndBack() {
        CategoryColor.entries.forEach { color ->
            val stored = converters.categoryColorToStorageValue(color)

            assertEquals(color.storageValue, stored)
            assertEquals(color, converters.storageValueToCategoryColor(stored))
        }
    }

    @Test(expected = IllegalArgumentException::class)
    fun unknownCategoryColorStorageValueIsRejected() {
        converters.storageValueToCategoryColor("UNKNOWN")
    }
}
