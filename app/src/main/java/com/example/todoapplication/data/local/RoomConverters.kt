package com.example.todoapplication.data.local

import androidx.room.TypeConverter
import com.example.todoapplication.domain.model.CategoryColor
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

class RoomConverters {
    @TypeConverter
    fun localDateToEpochDay(value: LocalDate): Long = value.toEpochDay()

    @TypeConverter
    fun epochDayToLocalDate(value: Long): LocalDate = LocalDate.ofEpochDay(value)

    @TypeConverter
    fun localTimeToMinuteOfDay(value: LocalTime?): Int? = value?.let {
        it.hour * MINUTES_PER_HOUR + it.minute
    }

    @TypeConverter
    fun minuteOfDayToLocalTime(value: Int?): LocalTime? = value?.let {
        LocalTime.ofSecondOfDay(it.toLong() * SECONDS_PER_MINUTE)
    }

    @TypeConverter
    fun instantToEpochMillis(value: Instant): Long = value.toEpochMilli()

    @TypeConverter
    fun epochMillisToInstant(value: Long): Instant = Instant.ofEpochMilli(value)

    @TypeConverter
    fun categoryColorToStorageValue(value: CategoryColor): String = value.storageValue

    @TypeConverter
    fun storageValueToCategoryColor(value: String): CategoryColor =
        CategoryColor.fromStorageValue(value)

    private companion object {
        const val MINUTES_PER_HOUR = 60
        const val SECONDS_PER_MINUTE = 60L
    }
}
