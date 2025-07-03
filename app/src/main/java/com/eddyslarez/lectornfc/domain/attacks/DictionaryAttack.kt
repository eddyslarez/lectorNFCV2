package com.eddyslarez.lectornfc.domain.attacks


import android.nfc.tech.MifareClassic
import com.eddyslarez.lectornfc.data.models.KeyPair
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

class DictionaryAttack(
    private val keyDictionaries: KeyDictionaries
) {

    data class AttackResult(
        val success: Boolean,
        val keyPair: KeyPair?,
        val keysAttempted: Int,
        val timeElapsed: Long,
        val foundKeyIndex: Int = -1,
        val keySource: String = ""
    )

    data class ProgressCallback(
        val sector: Int,
        val currentKeyIndex: Int,
        val totalKeys: Int,
        val currentKey: ByteArray,
        val timeElapsed: Long
    )

    suspend fun attackSector(
        mifare: MifareClassic,
        sector: Int,
        progressCallback: ((ProgressCallback) -> Unit)? = null
    ): AttackResult = withContext(Dispatchers.IO) {

        val startTime = System.currentTimeMillis()
        val allKeys = keyDictionaries.getAllKeys()
        var keysAttempted = 0

        try {
            for ((index, key) in allKeys.withIndex()) {
                keysAttempted++

                // Callback de progreso
                progressCallback?.invoke(
                    ProgressCallback(
                        sector = sector,
                        currentKeyIndex = index,
                        totalKeys = allKeys.size,
                        currentKey = key.clone(),
                        timeElapsed = System.currentTimeMillis() - startTime
                    )
                )

                var keyA: ByteArray? = null
                var keyB: ByteArray? = null

                // Probar Key A
                try {
                    if (mifare.authenticateSectorWithKeyA(sector, key)) {
                        keyA = key.clone()
                    }
                } catch (e: Exception) {
                    // Continuar con el siguiente intento
                }

                // Probar Key B
                try {
                    if (mifare.authenticateSectorWithKeyB(sector, key)) {
                        keyB = key.clone()
                    }
                } catch (e: Exception) {
                    // Continuar con el siguiente intento
                }

                // Si encontramos alguna clave
                if (keyA != null || keyB != null) {
                    val keySource = identifyKeySource(key)
                    val timeElapsed = System.currentTimeMillis() - startTime

                    return@withContext AttackResult(
                        success = true,
                        keyPair = KeyPair(keyA, keyB),
                        keysAttempted = keysAttempted,
                        timeElapsed = timeElapsed,
                        foundKeyIndex = index,
                        keySource = keySource
                    )
                }

                // Pequeña pausa para no saturar el sistema
                if (index % 50 == 0) {
                    delay(10)
                }
            }

            // No se encontró ninguna clave
            val timeElapsed = System.currentTimeMillis() - startTime
            AttackResult(
                success = false,
                keyPair = null,
                keysAttempted = keysAttempted,
                timeElapsed = timeElapsed
            )

        } catch (e: Exception) {
            val timeElapsed = System.currentTimeMillis() - startTime
            AttackResult(
                success = false,
                keyPair = null,
                keysAttempted = keysAttempted,
                timeElapsed = timeElapsed
            )
        }
    }

    suspend fun attackCard(
        mifare: MifareClassic,
        progressCallback: ((sector: Int, progress: ProgressCallback) -> Unit)? = null
    ): Map<Int, AttackResult> = withContext(Dispatchers.IO) {

        val results = mutableMapOf<Int, AttackResult>()

        try {
            mifare.connect()

            for (sector in 0 until mifare.sectorCount) {
                val result = attackSector(mifare, sector) { progress ->
                    progressCallback?.invoke(sector, progress)
                }

                results[sector] = result

                // Pequeña pausa entre sectores
                delay(100)
            }

        } catch (e: Exception) {
            // Error general en la conexión
        } finally {
            try {
                mifare.close()
            } catch (e: Exception) {
                // Ignorar errores al cerrar
            }
        }

        results
    }

    suspend fun quickScan(
        mifare: MifareClassic,
        maxKeysPerSector: Int = 20
    ): Map<Int, AttackResult> = withContext(Dispatchers.IO) {

        val results = mutableMapOf<Int, AttackResult>()
        val basicKeys = keyDictionaries.getBasicKeys() + keyDictionaries.getRussianKeys()
        val limitedKeys = basicKeys.take(maxKeysPerSector)

        try {
            mifare.connect()

            for (sector in 0 until mifare.sectorCount) {
                val startTime = System.currentTimeMillis()
                var keysAttempted = 0
                var found = false

                for (key in limitedKeys) {
                    keysAttempted++

                    var keyA: ByteArray? = null
                    var keyB: ByteArray? = null

                    try {
                        if (mifare.authenticateSectorWithKeyA(sector, key)) {
                            keyA = key.clone()
                            found = true
                        }

                        if (mifare.authenticateSectorWithKeyB(sector, key)) {
                            keyB = key.clone()
                            found = true
                        }

                        if (found) {
                            results[sector] = AttackResult(
                                success = true,
                                keyPair = KeyPair(keyA, keyB),
                                keysAttempted = keysAttempted,
                                timeElapsed = System.currentTimeMillis() - startTime,
                                keySource = identifyKeySource(key)
                            )
                            break
                        }
                    } catch (e: Exception) {
                        continue
                    }
                }

                if (!found) {
                    results[sector] = AttackResult(
                        success = false,
                        keyPair = null,
                        keysAttempted = keysAttempted,
                        timeElapsed = System.currentTimeMillis() - startTime
                    )
                }
            }

        } finally {
            try {
                mifare.close()
            } catch (e: Exception) {
                // Ignorar
            }
        }

        results
    }

    suspend fun attackWithUIDKeys(
        mifare: MifareClassic,
        uid: ByteArray
    ): Map<Int, AttackResult> = withContext(Dispatchers.IO) {

        val results = mutableMapOf<Int, AttackResult>()
        val uidKeys = keyDictionaries.generateUIDBasedKeys(uid)

        try {
            mifare.connect()

            for (sector in 0 until mifare.sectorCount) {
                val startTime = System.currentTimeMillis()
                var keysAttempted = 0
                var found = false

                for (key in uidKeys) {
                    keysAttempted++

                    var keyA: ByteArray? = null
                    var keyB: ByteArray? = null

                    try {
                        if (mifare.authenticateSectorWithKeyA(sector, key)) {
                            keyA = key.clone()
                            found = true
                        }

                        if (mifare.authenticateSectorWithKeyB(sector, key)) {
                            keyB = key.clone()
                            found = true
                        }

                        if (found) {
                            results[sector] = AttackResult(
                                success = true,
                                keyPair = KeyPair(keyA, keyB),
                                keysAttempted = keysAttempted,
                                timeElapsed = System.currentTimeMillis() - startTime,
                                keySource = "UID-based"
                            )
                            break
                        }
                    } catch (e: Exception) {
                        continue
                    }
                }

                if (!found) {
                    results[sector] = AttackResult(
                        success = false,
                        keyPair = null,
                        keysAttempted = keysAttempted,
                        timeElapsed = System.currentTimeMillis() - startTime
                    )
                }
            }

        } finally {
            try {
                mifare.close()
            } catch (e: Exception) {
                // Ignorar
            }
        }

        results
    }

    private fun identifyKeySource(key: ByteArray): String {
        val (isKnown, category) = keyDictionaries.isKnownKey(key)
        return if (isKnown && category != null) {
            category
        } else {
            "Unknown"
        }
    }

    fun getAttackStatistics(): DictionaryStatistics {
        val stats = keyDictionaries.getDictionaryStats()
        return DictionaryStatistics(
            totalKeys = stats["total_keys"] ?: 0,
            defaultKeys = stats["default_keys"] ?: 0,
            transportKeys = stats["transport_keys"] ?: 0,
            accessKeys = stats["access_keys"] ?: 0,
            russianKeys = stats["russian_keys"] ?: 0,
            breachedKeys = stats["breached_keys"] ?: 0,
            weakPatterns = stats["weak_patterns"] ?: 0,
            vendorKeys = stats["vendor_keys"] ?: 0
        )
    }

    fun estimateAttackTime(sectorCount: Int, fastMode: Boolean = false): AttackTimeEstimate {
        val stats = getAttackStatistics()
        val keysToTest = if (fastMode) minOf(stats.totalKeys, 100) else stats.totalKeys

        // Estimaciones basadas en testing real (aproximadamente)
        val timePerKeyMs = 50L // 50ms por clave en promedio
        val totalTimeMs = keysToTest * sectorCount * timePerKeyMs

        return AttackTimeEstimate(
            minimumSeconds = totalTimeMs / 1000,
            maximumSeconds = (totalTimeMs * 1.5).toLong() / 1000,
            averageSeconds = (totalTimeMs * 1.2).toLong() / 1000,
            keysToTest = keysToTest,
            sectorCount = sectorCount
        )
    }

    data class DictionaryStatistics(
        val totalKeys: Int,
        val defaultKeys: Int,
        val transportKeys: Int,
        val accessKeys: Int,
        val russianKeys: Int,
        val breachedKeys: Int,
        val weakPatterns: Int,
        val vendorKeys: Int
    )

    data class AttackTimeEstimate(
        val minimumSeconds: Long,
        val maximumSeconds: Long,
        val averageSeconds: Long,
        val keysToTest: Int,
        val sectorCount: Int
    ) {
        fun getFormattedTime(): String {
            return when {
                averageSeconds < 60 -> "${averageSeconds}s"
                averageSeconds < 3600 -> "${averageSeconds / 60}m ${averageSeconds % 60}s"
                else -> "${averageSeconds / 3600}h ${(averageSeconds % 3600) / 60}m"
            }
        }
    }
}