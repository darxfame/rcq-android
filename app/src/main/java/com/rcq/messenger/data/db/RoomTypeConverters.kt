package com.rcq.messenger.data.db

import androidx.room.TypeConverter
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class RoomTypeConverters {
    private val json = Json { ignoreUnknownKeys = true }

    @TypeConverter
    fun fromMapLongString(value: Map<Long, String>): String = json.encodeToString(value)

    @TypeConverter
    fun toMapLongString(value: String): Map<Long, String> =
        json.decodeFromString(value)

    @TypeConverter
    fun fromListString(value: List<String>): String = json.encodeToString(value)

    @TypeConverter
    fun toListString(value: String): List<String> =
        json.decodeFromString(value)

    @TypeConverter
    fun fromListLong(value: List<Long>): String = json.encodeToString(value)

    @TypeConverter
    fun toListLong(value: String): List<Long> =
        json.decodeFromString(value)
}