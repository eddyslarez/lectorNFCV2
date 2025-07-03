package com.eddyslarez.lectornfc.data.database.dao

import androidx.room.*
import com.eddyslarez.lectornfc.data.database.entities.ScanResult
import kotlinx.coroutines.flow.Flow

@Dao
interface ScanResultDao {
    @Query("SELECT * FROM scan_results ORDER BY timestamp DESC")
    fun getAllScanResults(): Flow<List<ScanResult>>

    @Query("SELECT * FROM scan_results WHERE sessionId = :sessionId")
    suspend fun getScanResultsBySession(sessionId: String): List<ScanResult>

    @Query("SELECT * FROM scan_results WHERE success = 1 ORDER BY timestamp DESC")
    fun getSuccessfulScans(): Flow<List<ScanResult>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScanResult(scanResult: ScanResult): Long

    @Update
    suspend fun updateScanResult(scanResult: ScanResult)

    @Delete
    suspend fun deleteScanResult(scanResult: ScanResult)

    @Query("DELETE FROM scan_results WHERE timestamp < :cutoffDate")
    suspend fun deleteOldResults(cutoffDate: Long)

    @Query("SELECT COUNT(*) FROM scan_results")
    suspend fun getTotalScansCount(): Int

    @Query("SELECT COUNT(*) FROM scan_results WHERE success = 1")
    suspend fun getSuccessfulScansCount(): Int
}