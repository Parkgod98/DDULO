package com.example.ddulo.data.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.ddulo.data.local.converter.JsonTypeConverters
import com.example.ddulo.data.local.dao.FavoriteDao
import com.example.ddulo.data.local.entity.FavoriteEntity

@Database(
    entities = [
        FavoriteEntity::class
    ],
    version = 1
)
@TypeConverters(JsonTypeConverters::class)
abstract class FavoriteDatabase : RoomDatabase() {

    abstract fun favoriteDao(): FavoriteDao

    companion object {
        @Volatile private var INSTANCE: FavoriteDatabase? = null

        fun get(context: Context): FavoriteDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    FavoriteDatabase::class.java,
                    "favorite.db"
                ).build().also { INSTANCE = it }
            }
    }
}