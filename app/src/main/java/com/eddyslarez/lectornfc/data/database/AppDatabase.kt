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

@Database(
    entities = [
        ScanResult::class,
        FoundKey::class,
        ScanSession::class,
        ScanHistoryEntity::class
    ],
    version = 1,
    exportSchema = false,

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

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "mifare_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}