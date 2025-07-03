package com.eddyslarez.lectornfc.data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "scan_results")
data class ScanResult(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val sessionId: String,
    val uid: String,
    val cardType: String,
    val sectorCount: Int,
    val crackedSectors: Int,
    val totalBlocks: Int,
    val readableBlocks: Int,
    val timestamp: Date,
    val duration: Long, // milliseconds
    val attackMethod: String,
    val success: Boolean,
    val notes: String = ""
)