package com.example.ddulo.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "favorites")
data class FavoriteEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,

    val type: FavoriteType,

    /** 사용자 지정 이름 */
    val displayName: String,

    /** 실제 데이터 (JSON) */
    val payload: String,

    val createdAt: Long = System.currentTimeMillis()
)
