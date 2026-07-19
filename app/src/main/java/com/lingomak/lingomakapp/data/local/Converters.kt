package com.lingomak.lingomakapp.data.local

import androidx.room.TypeConverter

class Converters {
    @TypeConverter
    fun fromString(value: String?): List<String> {
        if (value == null || value.isEmpty()) return emptyList()
        return value.split("|")
    }

    @TypeConverter
    fun fromList(list: List<String>?): String {
        if (list == null || list.isEmpty()) return ""
        return list.joinToString("|")
    }
}
