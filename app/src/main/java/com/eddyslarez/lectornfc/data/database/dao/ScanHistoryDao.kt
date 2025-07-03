package com.eddyslarez.lectornfc.data.database.dao

import androidx.room.*
import com.eddyslarez.lectornfc.data.database.entities.ScanHistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ScanHistoryDao {
    @Query("SELECT * FROM scan_history ORDER BY timestamp DESC")
    fun getAllScanHistory(): Flow<List<ScanHistoryEntity>>

    @Query("SELECT * FROM scan_history WHERE id = :id")
    suspend fun getScanHistoryById(id: Long): ScanHistoryEntity?

    @Query("SELECT * FROM scan_history WHERE uid = :uid ORDER BY timestamp DESC")
    suspend fun getScanHistoryByUid(uid: String): List<ScanHistoryEntity>

    @Query("SELECT * FROM scan_history WHERE attackMethod = :method ORDER BY timestamp DESC")
    suspend fun getScanHistoryByMethod(method: String): List<ScanHistoryEntity>

    @Query("SELECT * FROM scan_history WHERE timestamp BETWEEN :startDate AND :endDate ORDER BY timestamp DESC")
    suspend fun getScanHistoryByDateRange(startDate: Long, endDate: Long): List<ScanHistoryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScanHistory(scanHistory: ScanHistoryEntity): Long

    @Update
    suspend fun updateScanHistory(scanHistory: ScanHistoryEntity)

    @Delete
    suspend fun deleteScanHistory(scanHistory: ScanHistoryEntity)

    @Query("DELETE FROM scan_history")
    suspend fun deleteAllScanHistory()

    @Query("DELETE FROM scan_history WHERE timestamp < :cutoffDate")
    suspend fun deleteOldScanHistory(cutoffDate: Long)

    @Query("SELECT COUNT(*) FROM scan_history")
    suspend fun getTotalScansCount(): Int

    @Query("SELECT COUNT(*) FROM scan_history WHERE successRate > 0")
    suspend fun getSuccessfulScansCount(): Int

    @Query("SELECT COUNT(DISTINCT uid) FROM scan_history")
    suspend fun getUniqueCardsCount(): Int

    @Query("SELECT * FROM scan_history ORDER BY timestamp DESC")
    suspend fun getAllScanHistoryOnce(): List<ScanHistoryEntity>

    @Query("UPDATE scan_history SET exported = 1 WHERE id = :id")
    suspend fun markAsExported(id: Int)
}