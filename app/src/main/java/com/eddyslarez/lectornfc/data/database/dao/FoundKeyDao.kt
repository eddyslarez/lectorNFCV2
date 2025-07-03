package com.eddyslarez.lectornfc.data.database.dao

import androidx.room.*
import com.eddyslarez.lectornfc.data.database.entities.FoundKey
import kotlinx.coroutines.flow.Flow


@Dao
interface FoundKeyDao {
    @Query("SELECT * FROM found_keys ORDER BY timestamp DESC")
    fun getAllFoundKeys(): Flow<List<FoundKey>>

    @Query("SELECT * FROM found_keys WHERE sessionId = :sessionId")
    suspend fun getFoundKeysBySession(sessionId: String): List<FoundKey>

    @Query("SELECT * FROM found_keys WHERE sector = :sector")
    suspend fun getFoundKeysBySector(sector: Int): List<FoundKey>

    @Query("SELECT * FROM found_keys WHERE discoveryMethod = :method")
    suspend fun getFoundKeysByMethod(method: String): List<FoundKey>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFoundKey(foundKey: FoundKey): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFoundKeys(foundKeys: List<FoundKey>)

    @Update
    suspend fun updateFoundKey(foundKey: FoundKey)

    @Delete
    suspend fun deleteFoundKey(foundKey: FoundKey)

    @Query("DELETE FROM found_keys WHERE sessionId = :sessionId")
    suspend fun deleteFoundKeysBySession(sessionId: String)

    @Query("DELETE FROM found_keys WHERE timestamp < :cutoffDate")
    suspend fun deleteOldFoundKeys(cutoffDate: Long)

    @Query("SELECT COUNT(*) FROM found_keys")
    suspend fun getTotalFoundKeysCount(): Int

    @Query("SELECT COUNT(DISTINCT sector) FROM found_keys")
    suspend fun getUniqueSectorsCount(): Int

    @Query("SELECT * FROM found_keys WHERE confidence > :minConfidence ORDER BY confidence DESC")
    suspend fun getHighConfidenceKeys(minConfidence: Float): List<FoundKey>
}