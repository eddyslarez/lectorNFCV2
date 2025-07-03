package com.eddyslarez.lectornfc.utils


object Constants {
    // NFC Constants
    const val NFC_TIMEOUT = 5000L
    const val MAX_RETRY_ATTEMPTS = 3
    const val RETRY_DELAY_MS = 500L

    // Database Constants
    const val DATABASE_NAME = "mifare_database"
    const val DATABASE_VERSION = 2

    // Export Constants
    const val EXPORT_DIRECTORY = "MifarePro"
    const val MAX_EXPORT_SIZE = 50 * 1024 * 1024 // 50MB

    // Attack Constants
    const val MAX_ATTACK_TIME_MS = 300000L // 5 minutes
    const val DICTIONARY_TIMEOUT_MS = 60000L // 1 minute
    const val HARDNESTED_TIMEOUT_MS = 180000L // 3 minutes

    // UI Constants
    const val ANIMATION_DURATION_MS = 300
    const val PROGRESS_UPDATE_INTERVAL_MS = 100L

    // Security Constants
    const val MIN_KEY_ENTROPY = 0.5f
    const val MAX_FAILED_ATTEMPTS = 5

    // File Provider
    const val FILE_PROVIDER_AUTHORITY = "com.eddyslarez.lectornfc.fileprovider"
}