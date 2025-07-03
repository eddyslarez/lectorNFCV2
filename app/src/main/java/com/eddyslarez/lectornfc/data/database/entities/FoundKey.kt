package com.eddyslarez.lectornfc.data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "found_keys")
data class FoundKey(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val sessionId: String,
    val sector: Int,
    val keyA: String?,
    val keyB: String?,
    val keyType: String,
    val discoveryMethod: String,
    val confidence: Float,
    val timestamp: Long
)
