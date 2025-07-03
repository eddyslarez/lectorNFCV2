package com.eddyslarez.lectornfc.data.database.entities


import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "scan_history")
data class ScanHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val uid: String,
    val cardType: String,
    val timestamp: Long,
    val totalSectors: Int,
    val crackedSectors: Int,
    val totalBlocks: Int,
    val readableBlocks: Int,
    val foundKeys: Int,
    val attackMethod: String,
    val operationMode: String,
    val scanDuration: Long, // en millisegundos
    val successRate: Float,
    val rawData: String, // JSON con todos los datos
    val notes: String = "",
    val exported: Boolean = false
)