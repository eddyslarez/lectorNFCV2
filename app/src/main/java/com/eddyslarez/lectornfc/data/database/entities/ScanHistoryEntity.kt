package com.eddyslarez.lectornfc.data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey


@Entity(tableName = "scan_history")
data class ScanHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val uid: String,
    val timestamp: Long,
    val cardType: String,
    val operationMode: String,
    val attackMethod: String,
    val totalSectors: Int,
    val crackedSectors: Int,
    val totalBlocks: Int,
    val readableBlocks: Int,
    val foundKeys: Int,
    val scanDuration: Long,
    val successRate: Float,
    val rawData: String,
    val notes: String,
    val exported: Boolean
)