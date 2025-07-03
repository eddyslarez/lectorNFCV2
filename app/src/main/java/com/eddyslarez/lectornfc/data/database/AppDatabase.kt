package com.eddyslarez.lectornfc.data.database

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import android.content.Context
import com.eddyslarez.lectornfc.data.database.dao.*
import com.eddyslarez.lectornfc.data.database.entities.*
import androidx.room.*
import com.eddyslarez.lectornfc.data.database.dao.*
import com.eddyslarez.lectornfc.data.database.entities.*
import com.eddyslarez.lectornfc.utils.Converters

import com.eddyslarez.lectornfc.data.database.dao.*
import com.eddyslarez.lectornfc.data.database.entities.*
import androidx.room.*
import com.eddyslarez.lectornfc.data.database.dao.*
import com.eddyslarez.lectornfc.data.database.entities.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        ScanResult::class,
        FoundKey::class,
        ScanSession::class,
        ScanHistoryEntity::class
    ],
    version = 2,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun scanResultDao(): ScanResultDao
    abstract fun foundKeyDao(): FoundKeyDao
    abstract fun scanSessionDao(): ScanSessionDao
    abstract fun scanHistoryDao(): ScanHistoryDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        // Migración manual de versión 1 a 2
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Crear la tabla scan_history si no existe
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS scan_history (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        uid TEXT NOT NULL,
                        timestamp INTEGER NOT NULL,
                        cardType TEXT NOT NULL,
                        operationMode TEXT NOT NULL,
                        attackMethod TEXT NOT NULL,
                        totalSectors INTEGER NOT NULL,
                        crackedSectors INTEGER NOT NULL,
                        totalBlocks INTEGER NOT NULL,
                        readableBlocks INTEGER NOT NULL,
                        foundKeys INTEGER NOT NULL,
                        scanDuration INTEGER NOT NULL,
                        successRate REAL NOT NULL,
                        rawData TEXT NOT NULL,
                        notes TEXT NOT NULL,
                        exported INTEGER NOT NULL
                    )
                """.trimIndent())
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "mifare_database"
                )
                    .addMigrations(MIGRATION_1_2) // Usar migración manual
                    // .fallbackToDestructiveMigration() // Comentar esto por ahora
                    .build()
                INSTANCE = instance
                instance
            }
        }

        fun clearInstance() {
            INSTANCE = null
        }
    }
}