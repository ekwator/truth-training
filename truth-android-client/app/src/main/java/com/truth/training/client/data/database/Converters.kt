package com.truth.training.client.data.database

import androidx.room.TypeConverter
import java.time.Instant
import java.time.format.DateTimeFormatter

/**
 * Type converters for Room database.
 * Currently, all date fields are stored as ISO 8601 strings.
 */
class Converters {
    private val formatter = DateTimeFormatter.ISO_INSTANT
    
    @TypeConverter
    fun fromTimestamp(value: String?): String? = value
    
    @TypeConverter
    fun dateToTimestamp(date: String?): String? = date
}

