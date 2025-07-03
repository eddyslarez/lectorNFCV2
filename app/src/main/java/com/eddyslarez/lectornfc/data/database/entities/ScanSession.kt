package com.eddyslarez.lectornfc.data.database.entities


import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "scan_sessions")
data class ScanSession(
    @PrimaryKey
    val sessionId: String,
    val cardUid: String,
    val cardType: String,
    val startTime: Date,
    val endTime: Date?,
    val totalSectors: Int,
    val crackedSectors: Int,
    val attackMethod: String,
    val rawData: String, // JSON serialized block data
    val exported: Boolean = false,
    val notes: String = ""
)