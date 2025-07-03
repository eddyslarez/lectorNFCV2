package com.eddyslarez.lectornfc.domain.attacks


import android.nfc.tech.MifareClassic
import com.eddyslarez.lectornfc.data.models.KeyPair
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.security.SecureRandom
import kotlin.random.Random

class BruteForceAttack {

    companion object {
        private const val KEY_LENGTH = 6
        private const val MAX_ATTEMPTS_PER_CYCLE = 1000
        private const val DELAY_BETWEEN_CYCLES_MS = 100L
        private const val MAX_TIME_PER_SECTOR_MS = 300000L // 5 minutos máximo por sector
    }

    data class BruteForceResult(
        val success: Boolean,
        val keyPair: KeyPair?,
        val attemptsCount: Long,
        val timeElapsed: Long,
        val strategy: String,
        val foundKey: ByteArray? = null,
        val foundKeyType: String? = null
    )

    data class BruteForceProgress(
        val sector: Int,
        val currentAttempt: Long,
        val estimatedTotalAttempts: Long,
        val timeElapsed: Long,
        val currentStrategy: String,
        val currentKey: ByteArray
    )

    enum class BruteForceStrategy {
        SEQUENTIAL,     // 000000, 000001, 000002...
        RANDOM,         // Claves completamente aleatorias
        PATTERN_BASED,  // Basado en patrones comunes
        SMART_RANDOM,   // Aleatorio inteligente con heurísticas
        NIBBLE_BASED,   // Por nibbles (4 bits)
        HYBRID          // Combinación de estrategias
    }

    private val secureRandom = SecureRandom()

    suspend fun attackSector(
        mifare: MifareClassic,
        sector: Int,
        strategy: BruteForceStrategy = BruteForceStrategy.SMART_RANDOM,
        maxTimeMs: Long = MAX_TIME_PER_SECTOR_MS,
        progressCallback: ((BruteForceProgress) -> Unit)? = null
    ): BruteForceResult = withContext(Dispatchers.IO) {

        val startTime = System.currentTimeMillis()
        var attemptsCount = 0L

        try {
            when (strategy) {
                BruteForceStrategy.SEQUENTIAL -> executeSequentialAttack(
                    mifare, sector, maxTimeMs, startTime, progressCallback
                )
                BruteForceStrategy.RANDOM -> executeRandomAttack(
                    mifare, sector, maxTimeMs, startTime, progressCallback
                )
                BruteForceStrategy.PATTERN_BASED -> executePatternBasedAttack(
                    mifare, sector, maxTimeMs, startTime, progressCallback
                )
                BruteForceStrategy.SMART_RANDOM -> executeSmartRandomAttack(
                    mifare, sector, maxTimeMs, startTime, progressCallback
                )
                BruteForceStrategy.NIBBLE_BASED -> executeNibbleBasedAttack(
                    mifare, sector, maxTimeMs, startTime, progressCallback
                )
                BruteForceStrategy.HYBRID -> executeHybridAttack(
                    mifare, sector, maxTimeMs, startTime, progressCallback
                )
            }
        } catch (e: Exception) {
            val timeElapsed = System.currentTimeMillis() - startTime
            BruteForceResult(
                success = false,
                keyPair = null,
                attemptsCount = attemptsCount,
                timeElapsed = timeElapsed,
                strategy = strategy.name
            )
        }
    }

    private suspend fun executeSequentialAttack(
        mifare: MifareClassic,
        sector: Int,
        maxTimeMs: Long,
        startTime: Long,
        progressCallback: ((BruteForceProgress) -> Unit)?
    ): BruteForceResult {

        var attemptsCount = 0L
        val totalPossibleKeys = 256L.pow(KEY_LENGTH) // 2^48

        // Comenzar desde 000000 y avanzar secuencialmente
        val currentKey = ByteArray(KEY_LENGTH)

        while (System.currentTimeMillis() - startTime < maxTimeMs) {
            attemptsCount++

            // Progreso
            if (attemptsCount % 100 == 0L) {
                progressCallback?.invoke(
                    BruteForceProgress(
                        sector = sector,
                        currentAttempt = attemptsCount,
                        estimatedTotalAttempts = totalPossibleKeys,
                        timeElapsed = System.currentTimeMillis() - startTime,
                        currentStrategy = "SEQUENTIAL",
                        currentKey = currentKey.clone()
                    )
                )
            }

            // Probar la clave actual
            val result = testKey(mifare, sector, currentKey)
            if (result != null) {
                return BruteForceResult(
                    success = true,
                    keyPair = result.first,
                    attemptsCount = attemptsCount,
                    timeElapsed = System.currentTimeMillis() - startTime,
                    strategy = "SEQUENTIAL",
                    foundKey = currentKey.clone(),
                    foundKeyType = result.second
                )
            }

            // Incrementar la clave
            incrementKey(currentKey)

            // Pausa cada ciclo
            if (attemptsCount % MAX_ATTEMPTS_PER_CYCLE == 0L) {
                delay(DELAY_BETWEEN_CYCLES_MS)
            }
        }

        return BruteForceResult(
            success = false,
            keyPair = null,
            attemptsCount = attemptsCount,
            timeElapsed = System.currentTimeMillis() - startTime,
            strategy = "SEQUENTIAL"
        )
    }

    private suspend fun executeRandomAttack(
        mifare: MifareClassic,
        sector: Int,
        maxTimeMs: Long,
        startTime: Long,
        progressCallback: ((BruteForceProgress) -> Unit)?
    ): BruteForceResult {

        var attemptsCount = 0L
        val estimatedAttempts = 2L.pow(24) // Estimación optimista

        while (System.currentTimeMillis() - startTime < maxTimeMs) {
            attemptsCount++

            // Generar clave aleatoria
            val randomKey = ByteArray(KEY_LENGTH)
            secureRandom.nextBytes(randomKey)

            // Progreso
            if (attemptsCount % 100 == 0L) {
                progressCallback?.invoke(
                    BruteForceProgress(
                        sector = sector,
                        currentAttempt = attemptsCount,
                        estimatedTotalAttempts = estimatedAttempts,
                        timeElapsed = System.currentTimeMillis() - startTime,
                        currentStrategy = "RANDOM",
                        currentKey = randomKey.clone()
                    )
                )
            }

            // Probar la clave
            val result = testKey(mifare, sector, randomKey)
            if (result != null) {
                return BruteForceResult(
                    success = true,
                    keyPair = result.first,
                    attemptsCount = attemptsCount,
                    timeElapsed = System.currentTimeMillis() - startTime,
                    strategy = "RANDOM",
                    foundKey = randomKey.clone(),
                    foundKeyType = result.second
                )
            }

            // Pausa cada ciclo
            if (attemptsCount % MAX_ATTEMPTS_PER_CYCLE == 0L) {
                delay(DELAY_BETWEEN_CYCLES_MS)
            }
        }

        return BruteForceResult(
            success = false,
            keyPair = null,
            attemptsCount = attemptsCount,
            timeElapsed = System.currentTimeMillis() - startTime,
            strategy = "RANDOM"
        )
    }

    private suspend fun executePatternBasedAttack(
        mifare: MifareClassic,
        sector: Int,
        maxTimeMs: Long,
        startTime: Long,
        progressCallback: ((BruteForceProgress) -> Unit)?
    ): BruteForceResult {

        var attemptsCount = 0L
        val patterns = generateCommonPatterns()

        for (pattern in patterns) {
            if (System.currentTimeMillis() - startTime >= maxTimeMs) break

            for (variation in generatePatternVariations(pattern)) {
                attemptsCount++

                // Progreso
                if (attemptsCount % 50 == 0L) {
                    progressCallback?.invoke(
                        BruteForceProgress(
                            sector = sector,
                            currentAttempt = attemptsCount,
                            estimatedTotalAttempts = patterns.size * 100L,
                            timeElapsed = System.currentTimeMillis() - startTime,
                            currentStrategy = "PATTERN_BASED",
                            currentKey = variation.clone()
                        )
                    )
                }

                // Probar la variación
                val result = testKey(mifare, sector, variation)
                if (result != null) {
                    return BruteForceResult(
                        success = true,
                        keyPair = result.first,
                        attemptsCount = attemptsCount,
                        timeElapsed = System.currentTimeMillis() - startTime,
                        strategy = "PATTERN_BASED",
                        foundKey = variation.clone(),
                        foundKeyType = result.second
                    )
                }

                if (System.currentTimeMillis() - startTime >= maxTimeMs) break
            }
        }

        return BruteForceResult(
            success = false,
            keyPair = null,
            attemptsCount = attemptsCount,
            timeElapsed = System.currentTimeMillis() - startTime,
            strategy = "PATTERN_BASED"
        )
    }

    private suspend fun executeSmartRandomAttack(
        mifare: MifareClassic,
        sector: Int,
        maxTimeMs: Long,
        startTime: Long,
        progressCallback: ((BruteForceProgress) -> Unit)?
    ): BruteForceResult {

        var attemptsCount = 0L
        val estimatedAttempts = 1000000L // Estimación más realista
        val uid = mifare.tag.id

        // Generar semillas basadas en información de la tarjeta
        val seeds = generateSmartSeeds(uid, sector)

        while (System.currentTimeMillis() - startTime < maxTimeMs) {
            attemptsCount++

            // Alternar entre diferentes heurísticas
            val key = when (attemptsCount % 4) {
                0L -> generateUIDBasedKey(uid, sector, attemptsCount)
                1L -> generateTimeBasedKey(startTime, attemptsCount)
                2L -> generateSectorBasedKey(sector, attemptsCount)
                else -> generateWeightedRandomKey()
            }

            // Progreso
            if (attemptsCount % 100 == 0L) {
                progressCallback?.invoke(
                    BruteForceProgress(
                        sector = sector,
                        currentAttempt = attemptsCount,
                        estimatedTotalAttempts = estimatedAttempts,
                        timeElapsed = System.currentTimeMillis() - startTime,
                        currentStrategy = "SMART_RANDOM",
                        currentKey = key.clone()
                    )
                )
            }

            // Probar la clave
            val result = testKey(mifare, sector, key)
            if (result != null) {
                return BruteForceResult(
                    success = true,
                    keyPair = result.first,
                    attemptsCount = attemptsCount,
                    timeElapsed = System.currentTimeMillis() - startTime,
                    strategy = "SMART_RANDOM",
                    foundKey = key.clone(),
                    foundKeyType = result.second
                )
            }

            // Pausa cada ciclo
            if (attemptsCount % MAX_ATTEMPTS_PER_CYCLE == 0L) {
                delay(DELAY_BETWEEN_CYCLES_MS)
            }
        }

        return BruteForceResult(
            success = false,
            keyPair = null,
            attemptsCount = attemptsCount,
            timeElapsed = System.currentTimeMillis() - startTime,
            strategy = "SMART_RANDOM"
        )
    }

    private suspend fun executeNibbleBasedAttack(
        mifare: MifareClassic,
        sector: Int,
        maxTimeMs: Long,
        startTime: Long,
        progressCallback: ((BruteForceProgress) -> Unit)?
    ): BruteForceResult {

        var attemptsCount = 0L
        val totalCombinations = 16L.pow(KEY_LENGTH * 2) // 16^12 nibbles

        // Trabajar con nibbles (4 bits cada uno)
        val currentNibbles = IntArray(KEY_LENGTH * 2) // 12 nibbles para 6 bytes

        while (System.currentTimeMillis() - startTime < maxTimeMs) {
            attemptsCount++

            // Convertir nibbles a bytes
            val key = nibblesToBytes(currentNibbles)

            // Progreso
            if (attemptsCount % 100 == 0L) {
                progressCallback?.invoke(
                    BruteForceProgress(
                        sector = sector,
                        currentAttempt = attemptsCount,
                        estimatedTotalAttempts = totalCombinations,
                        timeElapsed = System.currentTimeMillis() - startTime,
                        currentStrategy = "NIBBLE_BASED",
                        currentKey = key.clone()
                    )
                )
            }

            // Probar la clave
            val result = testKey(mifare, sector, key)
            if (result != null) {
                return BruteForceResult(
                    success = true,
                    keyPair = result.first,
                    attemptsCount = attemptsCount,
                    timeElapsed = System.currentTimeMillis() - startTime,
                    strategy = "NIBBLE_BASED",
                    foundKey = key.clone(),
                    foundKeyType = result.second
                )
            }

            // Incrementar nibbles
            incrementNibbles(currentNibbles)

            // Pausa cada ciclo
            if (attemptsCount % MAX_ATTEMPTS_PER_CYCLE == 0L) {
                delay(DELAY_BETWEEN_CYCLES_MS)
            }
        }

        return BruteForceResult(
            success = false,
            keyPair = null,
            attemptsCount = attemptsCount,
            timeElapsed = System.currentTimeMillis() - startTime,
            strategy = "NIBBLE_BASED"
        )
    }

    private suspend fun executeHybridAttack(
        mifare: MifareClassic,
        sector: Int,
        maxTimeMs: Long,
        startTime: Long,
        progressCallback: ((BruteForceProgress) -> Unit)?
    ): BruteForceResult {

        val timePerStrategy = maxTimeMs / 3
        val strategies = listOf(
            BruteForceStrategy.SMART_RANDOM,
            BruteForceStrategy.PATTERN_BASED,
            BruteForceStrategy.RANDOM
        )

        for ((index, strategy) in strategies.withIndex()) {
            val strategyStartTime = System.currentTimeMillis()
            val remainingTime = maxTimeMs - (strategyStartTime - startTime)
            val strategyMaxTime = minOf(timePerStrategy, remainingTime)

            if (strategyMaxTime <= 0) break

            val result = when (strategy) {
                BruteForceStrategy.SMART_RANDOM -> executeSmartRandomAttack(
                    mifare, sector, strategyMaxTime, strategyStartTime, progressCallback
                )
                BruteForceStrategy.PATTERN_BASED -> executePatternBasedAttack(
                    mifare, sector, strategyMaxTime, strategyStartTime, progressCallback
                )
                BruteForceStrategy.RANDOM -> executeRandomAttack(
                    mifare, sector, strategyMaxTime, strategyStartTime, progressCallback
                )
                else -> continue
            }

            if (result.success) {
                return result.copy(strategy = "HYBRID_${strategy.name}")
            }
        }

        return BruteForceResult(
            success = false,
            keyPair = null,
            attemptsCount = 0,
            timeElapsed = System.currentTimeMillis() - startTime,
            strategy = "HYBRID"
        )
    }

    private suspend fun testKey(mifare: MifareClassic, sector: Int, key: ByteArray): Pair<KeyPair, String>? {
        return withContext(Dispatchers.IO) {
            try {
                var keyA: ByteArray? = null
                var keyB: ByteArray? = null
                var keyType = ""

                // Probar Key A
                try {
                    if (mifare.authenticateSectorWithKeyA(sector, key)) {
                        keyA = key.clone()
                        keyType = "A"
                    }
                } catch (e: Exception) {
                    // Continuar con Key B
                }

                // Probar Key B si Key A no funcionó
                if (keyA == null) {
                    try {
                        if (mifare.authenticateSectorWithKeyB(sector, key)) {
                            keyB = key.clone()
                            keyType = "B"
                        }
                    } catch (e: Exception) {
                        // No funciona ninguna
                    }
                }

                if (keyA != null || keyB != null) {
                    Pair(KeyPair(keyA, keyB), keyType)
                } else {
                    null
                }
            } catch (e: Exception) {
                null
            }
        }
    }

    // Funciones auxiliares
    private fun incrementKey(key: ByteArray) {
        for (i in key.indices.reversed()) {
            if (key[i].toInt() and 0xFF < 255) {
                key[i] = (key[i].toInt() + 1).toByte()
                break
            } else {
                key[i] = 0
            }
        }
    }

    private fun incrementNibbles(nibbles: IntArray) {
        for (i in nibbles.indices.reversed()) {
            if (nibbles[i] < 15) {
                nibbles[i]++
                break
            } else {
                nibbles[i] = 0
            }
        }
    }

    private fun nibblesToBytes(nibbles: IntArray): ByteArray {
        val bytes = ByteArray(nibbles.size / 2)
        for (i in bytes.indices) {
            bytes[i] = ((nibbles[i * 2] shl 4) or nibbles[i * 2 + 1]).toByte()
        }
        return bytes
    }

    private fun generateCommonPatterns(): List<ByteArray> {
        val patterns = mutableListOf<ByteArray>()

        // Patrones de repetición
        for (byte in 0x00..0xFF) {
            patterns.add(ByteArray(KEY_LENGTH) { byte.toByte() })
        }

        // Patrones incrementales
        for (start in 0x00..0xFA) {
            val pattern = ByteArray(KEY_LENGTH)
            for (i in 0 until KEY_LENGTH) {
                pattern[i] = (start + i).toByte()
            }
            patterns.add(pattern)
        }

        // Patrones de fecha/hora
        patterns.add(byteArrayOf(0x20, 0x24, 0x01, 0x01, 0x00, 0x00)) // 2024-01-01
        patterns.add(byteArrayOf(0x12, 0x34, 0x56, 0x78, 0x9A.toByte(), 0xBC.toByte()))

        return patterns
    }

    private fun generatePatternVariations(basePattern: ByteArray): List<ByteArray> {
        val variations = mutableListOf<ByteArray>()

        // Patrón original
        variations.add(basePattern.clone())

        // Patrón invertido
        variations.add(basePattern.reversedArray())

        // Patrones con XOR
        for (xorValue in listOf(0x01, 0xFF, 0xAA, 0x55)) {
            val xorPattern = basePattern.clone()
            for (i in xorPattern.indices) {
                xorPattern[i] = (xorPattern[i].toInt() xor xorValue).toByte()
            }
            variations.add(xorPattern)
        }

        return variations
    }

    private fun generateSmartSeeds(uid: ByteArray, sector: Int): List<Long> {
        val seeds = mutableListOf<Long>()

        // Semilla basada en UID
        if (uid.isNotEmpty()) {
            seeds.add(uid.fold(0L) { acc, byte -> acc * 256 + (byte.toInt() and 0xFF) })
        }

        // Semilla basada en sector
        seeds.add(sector.toLong())

        // Semilla basada en tiempo
        seeds.add(System.currentTimeMillis())

        return seeds
    }

    private fun generateUIDBasedKey(uid: ByteArray, sector: Int, attempt: Long): ByteArray {
        val key = ByteArray(KEY_LENGTH)
        val seed = uid.fold(sector.toLong()) { acc, byte -> acc * 256 + (byte.toInt() and 0xFF) } + attempt
        val random = Random(seed)

        for (i in key.indices) {
            key[i] = random.nextInt(256).toByte()
        }

        return key
    }

    private fun generateTimeBasedKey(startTime: Long, attempt: Long): ByteArray {
        val key = ByteArray(KEY_LENGTH)
        val seed = startTime + attempt
        val random = Random(seed)

        for (i in key.indices) {
            key[i] = random.nextInt(256).toByte()
        }

        return key
    }

    private fun generateSectorBasedKey(sector: Int, attempt: Long): ByteArray {
        val key = ByteArray(KEY_LENGTH)
        val seed = sector * 1000L + attempt
        val random = Random(seed)

        for (i in key.indices) {
            key[i] = random.nextInt(256).toByte()
        }

        return key
    }

    private fun generateWeightedRandomKey(): ByteArray {
        val key = ByteArray(KEY_LENGTH)

        // Favorecer ciertos bytes que son más comunes en claves reales
        val commonBytes = listOf(0x00, 0xFF, 0x12, 0x34, 0x56, 0x78, 0x9A, 0xBC, 0xAA, 0x55)

        for (i in key.indices) {
            key[i] = if (secureRandom.nextFloat() < 0.3f) {
                // 30% de probabilidad de usar un byte común
                commonBytes.random().toByte()
            } else {
                // 70% aleatorio
                secureRandom.nextInt(256).toByte()
            }
        }

        return key
    }

    private fun Long.pow(exponent: Int): Long {
        var result = 1L
        repeat(exponent) {
            result *= this
        }
        return result
    }

    fun estimateTime(strategy: BruteForceStrategy): String {
        return when (strategy) {
            BruteForceStrategy.SEQUENTIAL -> "Extremadamente lento (años)"
            BruteForceStrategy.RANDOM -> "Muy lento (meses a años)"
            BruteForceStrategy.PATTERN_BASED -> "Moderado (horas a días)"
            BruteForceStrategy.SMART_RANDOM -> "Rápido a moderado (minutos a horas)"
            BruteForceStrategy.NIBBLE_BASED -> "Lento (días a semanas)"
            BruteForceStrategy.HYBRID -> "Variable (minutos a horas)"
        }
    }
}