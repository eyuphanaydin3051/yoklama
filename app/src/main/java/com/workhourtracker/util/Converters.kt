package com.workhourtracker.util

import androidx.room.TypeConverter
import com.workhourtracker.domain.model.RecordType
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

class Converters {
    @TypeConverter
    fun fromRecordType(value: RecordType?): String? = value?.name

    @TypeConverter
    fun toRecordType(value: String?): RecordType? = value?.let { RecordType.valueOf(it) }

    @TypeConverter
    fun fromLocalDateTime(value: LocalDateTime?): Long? =
        value?.atZone(ZoneId.systemDefault())?.toInstant()?.toEpochMilli()

    @TypeConverter
    fun toLocalDateTime(value: Long?): LocalDateTime? =
        value?.let { LocalDateTime.ofInstant(Instant.ofEpochMilli(it), ZoneId.systemDefault()) }

    @TypeConverter
    fun fromLocalDate(value: LocalDate?): Long? =
        value?.atStartOfDay(ZoneId.systemDefault())?.toInstant()?.toEpochMilli()

    @TypeConverter
    fun toLocalDate(value: Long?): LocalDate? =
        value?.let { LocalDateTime.ofInstant(Instant.ofEpochMilli(it), ZoneId.systemDefault()).toLocalDate() }
}
