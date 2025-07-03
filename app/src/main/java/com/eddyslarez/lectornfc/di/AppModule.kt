package com.eddyslarez.lectornfc.di

import androidx.room.Room
import com.eddyslarez.lectornfc.data.database.AppDatabase
import com.eddyslarez.lectornfc.data.repository.MifareRepository
import com.eddyslarez.lectornfc.domain.attacks.*
import com.eddyslarez.lectornfc.domain.usecases.*
import com.eddyslarez.lectornfc.presentation.viewmodel.*
import com.eddyslarez.lectornfc.utils.*
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val appModule = module {

    // Database - Configuración mejorada
    single {
        Room.databaseBuilder(
            androidContext(),
            AppDatabase::class.java,
            "mifare_database"
        )
            .fallbackToDestructiveMigration() // Recrear DB si hay problemas de migración
            .enableMultiInstanceInvalidation() // Sincronización entre instancias
            .build()
    }

    // DAOs
    single { get<AppDatabase>().scanResultDao() }
    single { get<AppDatabase>().foundKeyDao() }
    single { get<AppDatabase>().scanSessionDao() }
    single { get<AppDatabase>().scanHistoryDao() }

    // Repository
    single { MifareRepository(get(), get(), get(), get()) }

    // Utils
    single { ExportManager(androidContext()) }
    single { ShareManager(androidContext()) }
    single { NFCHelper(androidContext()) }
    single { CryptoUtils() }

    // Attack engines
    single { KeyDictionaries() }
    single { NonceAnalyzer() }
    single { HardnestedAttacker() }
    single { MKFKey32() }
    single { DictionaryAttack(get()) }
    single { BruteForceAttack() }
    single { AdvancedMifareManager(get(), get(), get(), get(), get(), get()) }

    // Use cases
    single { ReadCardUseCase(get()) }
    single { WriteCardUseCase(get()) }
    single { CrackCardUseCase(get()) }
    single { ExportDataUseCase(get()) }

    // ViewModels
    viewModel { MainViewModel(get(), get(), get(), get(), get()) }
    viewModel { HistoryViewModel(get()) }
    viewModel { ToolsViewModel(get()) }
}