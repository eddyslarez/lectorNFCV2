package com.eddyslarez.lectornfc.data.database.dao

import androidx.room.*
import com.eddyslarez.lectornfc.data.database.entities.ScanSession
import kotlinx.coroutines.flow.Flow

@Dao
interface ScanSessionDao {
    @Query("SELECT * FROM scan_sessions ORDER BY startTime DESC")
    fun getAllSessions(): Flow<List<ScanSession>>

    @Query("SELECT * FROM scan_sessions WHERE sessionId = :sessionId")
    suspend fun getSessionById(sessionId: String): ScanSession?

    @Query("SELECT * FROM scan_sessions WHERE cardUid = :uid ORDER BY startTime DESC")
    suspend fun getSessionsByUid(uid: String): List<ScanSession>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: ScanSession)

    @Update
    suspend fun updateSession(session: ScanSession)

    @Delete
    suspend fun deleteSession(session: ScanSession)

    @Query("DELETE FROM scan_sessions WHERE startTime < :cutoffDate")
    suspend fun deleteOldSessions(cutoffDate: Long)

    @Query("SELECT COUNT(*) FROM scan_sessions")
    suspend fun getTotalSessionsCount(): Int

    @Query("SELECT COUNT(DISTINCT cardUid) FROM scan_sessions")
    suspend fun getUniqueCardsCount(): Int
}
