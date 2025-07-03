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
    version = 2, // Incrementamos la versión de 1 a 2
    exportSchema = true, // Cambiamos a true para generar esquemas
    autoMigrations = [
        AutoMigration(from = 1, to = 2)
    ]
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
                    .fallbackToDestructiveMigration() // Esto recreará la DB si hay problemas
                    .build()
                INSTANCE = instance
                instance
            }
        }
        
        // Método para limpiar la instancia (útil para testing)
        fun clearInstance() {
            INSTANCE = null
        }
    }
}