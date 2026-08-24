package com.example.ddulo.data.local.converter

import androidx.room.TypeConverter
import com.example.ddulo.data.local.entity.FavoriteType
import com.google.gson.Gson

class JsonTypeConverters {

    val gson = Gson()

    @TypeConverter
    fun fromFavoriteType(type: FavoriteType): String = type.name

    @TypeConverter
    fun toFavoriteType(value: String): FavoriteType =
        FavoriteType.valueOf(value)
}

