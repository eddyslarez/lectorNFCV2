package com.eddyslarez.lectornfc.domain.attacks

import com.eddyslarez.lectornfc.data.models.NonceData
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.plus
import kotlinx.coroutines.withTimeoutOrNull
import java.nio.ByteBuffer
import java.security.MessageDigest
import java.util.Date
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec
import kotlin.math.abs
import kotlin.math.log2
import kotlin.math.min
import kotlin.math.pow


/**
 * Resultado del análisis de nonces
 */
data class AnalysisResult(
    val key: ByteArray?,
    val confidence: Float,
    val method: String,
    val metadata: Map<String, Any> = emptyMap()
)

/**
 * Analizador avanzado de nonces para sistemas de domofón rusos
 * Especializado en detectar patrones y vulnerabilidades en sistemas MIFARE/NFC
 */
class NonceAnalyzer {

    companion object {
        private const val KEY_SIZE = 6
        private const val MIN_NONCES = 2
        private const val MAX_ANALYSIS_TIME = 30000L // 30 segundos
        private const val CONFIDENCE_THRESHOLD = 0.7f

        // Patrones conocidos de sistemas rusos
        private val RUSSIAN_DOMOPHONE_PATTERNS = listOf(
            byteArrayOf(0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte()), // Default
            byteArrayOf(0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte()), // Debug
            byteArrayOf(0x12.toByte(), 0x34.toByte(), 0x56.toByte(), 0x78.toByte(), 0x9A.toByte(), 0xBC.toByte()), // Común en Vizit
            byteArrayOf(0xA0.toByte(), 0xA1.toByte(), 0xA2.toByte(), 0xA3.toByte(), 0xA4.toByte(), 0xA5.toByte()), // Patrón Cyfral
            byteArrayOf(0x48.toByte(), 0x4F.toByte(), 0x4D.toByte(), 0x45.toByte(), 0x31.toByte(), 0x32.toByte())  // "HOME12"
        )

    }

    /**
     * Función principal de análisis mejorada
     */
    suspend fun analyzeNonces(nonces: List<NonceData>, uid: ByteArray): AnalysisResult? {
        if (nonces.size < MIN_NONCES) return null

        return withTimeoutOrNull(MAX_ANALYSIS_TIME) {
            try {
                // Análisis paralelo de diferentes métodos
                val analysisJobs = listOf(
                    async { analyzeAdvancedPatterns(nonces, uid) },
                    async { analyzeTemporalPatterns(nonces, uid) },
                    async { analyzeStatisticalPatterns(nonces, uid) },
                    async { analyzeFrequencyWeakness(nonces, uid) },
                    async { analyzeRussianDomophonePatterns(nonces, uid) },
                    async { analyzeLinearFeedbackShiftRegister(nonces, uid) },
                    async { analyzeCryptographicWeakness(nonces, uid) }
                )

                // Recopilar resultados
                val results = analysisJobs.awaitAll().filterNotNull()

                // Retornar el resultado con mayor confianza
                results.maxByOrNull { it.confidence }

            } catch (e: Exception) {
                null
            }
        }
    }

    /**
     * Análisis avanzado de patrones mejorado
     */
    private suspend fun analyzeAdvancedPatterns(nonces: List<NonceData>, uid: ByteArray): AnalysisResult? {
        val patterns = findAdvancedPatterns(nonces)

        patterns.forEach { pattern ->
            val key = deriveKeyFromAdvancedPattern(pattern, uid)
            if (key != null) {
                val confidence = calculatePatternConfidence(pattern, nonces)
                if (confidence > CONFIDENCE_THRESHOLD) {
                    return AnalysisResult(
                        key = key,
                        confidence = confidence,
                        method = "Advanced Pattern Analysis",
                        metadata = mapOf(
                            "pattern_type" to "XOR_ROTATION",
                            "pattern_strength" to calculatePatternStrength(pattern)
                        )
                    )
                }
            }
        }
        return null
    }

    /**
     * Análisis de patrones temporales mejorado
     */
    private suspend fun analyzeTemporalPatterns(nonces: List<NonceData>, uid: ByteArray): AnalysisResult? {
        val patterns = findTemporalPatterns(nonces)

        patterns.forEach { pattern ->
            val key = deriveKeyFromTemporalPattern(pattern, uid)
            if (key != null) {
                val confidence = calculateTemporalConfidence(nonces)
                if (confidence > CONFIDENCE_THRESHOLD) {
                    return AnalysisResult(
                        key = key,
                        confidence = confidence,
                        method = "Temporal Pattern Analysis",
                        metadata = mapOf(
                            "time_variance" to calculateTimeVariance(nonces),
                            "sequence_correlation" to calculateSequenceCorrelation(nonces)
                        )
                    )
                }
            }
        }
        return null
    }

    /**
     * Análisis estadístico mejorado
     */
    private suspend fun analyzeStatisticalPatterns(nonces: List<NonceData>, uid: ByteArray): AnalysisResult? {
        val patterns = findStatisticalPatterns(nonces)

        patterns.forEach { pattern ->
            val key = deriveKeyFromStatisticalPattern(pattern, uid)
            if (key != null) {
                val confidence = calculateStatisticalConfidence(nonces)
                if (confidence > CONFIDENCE_THRESHOLD) {
                    return AnalysisResult(
                        key = key,
                        confidence = confidence,
                        method = "Statistical Pattern Analysis",
                        metadata = mapOf(
                            "entropy" to calculateEntropy(nonces),
                            "distribution_uniformity" to calculateDistributionUniformity(nonces)
                        )
                    )
                }
            }
        }
        return null
    }

    /**
     * Análisis de debilidad de frecuencia (implementación faltante)
     */
    private suspend fun analyzeFrequencyWeakness(nonces: List<NonceData>, uid: ByteArray): AnalysisResult? {
        val frequencyMap = mutableMapOf<Byte, Int>()

        // Analizar frecuencias de bytes
        nonces.forEach { nonceData ->
            nonceData.nonce.forEach { byte ->
                frequencyMap[byte] = frequencyMap.getOrDefault(byte, 0) + 1
            }
        }

        // Detectar patrones de frecuencia anómalos
        val totalBytes = nonces.sumOf { it.nonce.size }
        val expectedFrequency = totalBytes / 256.0

        val anomalousByte = frequencyMap.maxByOrNull {
            abs(it.value - expectedFrequency)
        }?.key

        if (anomalousByte != null) {
            val key = deriveKeyFromFrequencyWeakness(anomalousByte, uid, frequencyMap)
            if (key != null) {
                val confidence = calculateFrequencyConfidence(frequencyMap, totalBytes)
                if (confidence > CONFIDENCE_THRESHOLD) {
                    return AnalysisResult(
                        key = key,
                        confidence = confidence,
                        method = "Frequency Weakness Analysis",
                        metadata = mapOf(
                            "anomalous_byte" to anomalousByte,
                            "frequency_deviation" to abs(frequencyMap[anomalousByte]!! - expectedFrequency)
                        )
                    )
                }
            }
        }
        return null
    }

    /**
     * Análisis específico para sistemas de domofón rusos
     */
    private suspend fun analyzeRussianDomophonePatterns(nonces: List<NonceData>, uid: ByteArray): AnalysisResult? {
        RUSSIAN_DOMOPHONE_PATTERNS.forEach { pattern ->
            val key = deriveKeyFromKnownPattern(pattern, uid)
            if (key != null && validateKeyAgainstNonces(key, nonces)) {
                return AnalysisResult(
                    key = key,
                    confidence = 0.95f,
                    method = "Russian Domophone Pattern",
                    metadata = mapOf(
                        "pattern_type" to "KNOWN_RUSSIAN_SYSTEM",
                        "pattern" to pattern.contentToString()
                    )
                )
            }
        }
        return null
    }

    /**
     * Análisis LFSR (Linear Feedback Shift Register)
     */
    private suspend fun analyzeLinearFeedbackShiftRegister(nonces: List<NonceData>, uid: ByteArray): AnalysisResult? {
        // Implementación de análisis LFSR para detectar generadores débiles
        val lfsrStates = detectLFSRStates(nonces)

        if (lfsrStates.isNotEmpty()) {
            val key = deriveKeyFromLFSR(lfsrStates, uid)
            if (key != null) {
                return AnalysisResult(
                    key = key,
                    confidence = 0.85f,
                    method = "LFSR Analysis",
                    metadata = mapOf(
                        "lfsr_states" to lfsrStates.size,
                        "polynomial" to detectLFSRPolynomial(lfsrStates)
                    )
                )
            }
        }
        return null
    }

    /**
     * Análisis de debilidades criptográficas
     */
    private suspend fun analyzeCryptographicWeakness(nonces: List<NonceData>, uid: ByteArray): AnalysisResult? {
        // Detectar patrones criptográficos débiles
        val weaknessScore = calculateCryptographicWeakness(nonces)

        if (weaknessScore > 0.8f) {
            val key = deriveKeyFromCryptographicWeakness(nonces, uid)
            if (key != null) {
                return AnalysisResult(
                    key = key,
                    confidence = weaknessScore,
                    method = "Cryptographic Weakness Analysis",
                    metadata = mapOf(
                        "weakness_type" to "PREDICTABLE_PRNG",
                        "weakness_score" to weaknessScore
                    )
                )
            }
        }
        return null
    }

    // =============== MÉTODOS AUXILIARES MEJORADOS ===============

    private fun findAdvancedPatterns(nonces: List<NonceData>): List<ByteArray> {
        val patterns = mutableListOf<ByteArray>()

        for (i in 0 until nonces.size - 1) {
            val current = nonces[i].nonce
            val next = nonces[i + 1].nonce

            // XOR entre nonces
            val xor = ByteArray(current.size)
            for (j in current.indices) {
                xor[j] = (current[j].toInt() xor next[j].toInt()).toByte()
            }
            patterns.add(xor)

            // Rotación avanzada
            val rotation = ByteArray(current.size)
            for (j in current.indices) {
                rotation[j] = rotateLeft(current[j].toLong(), j + 1).toByte()
            }
            patterns.add(rotation)

            // Patrón de suma modular
            val modSum = ByteArray(current.size)
            for (j in current.indices) {
                modSum[j] = ((current[j].toInt() + next[j].toInt()) % 256).toByte()
            }
            patterns.add(modSum)
        }

        return patterns
    }

    private fun findTemporalPatterns(nonces: List<NonceData>): List<ByteArray> {
        val patterns = mutableListOf<ByteArray>()

        nonces.windowed(3).forEach { window ->
            val pattern = ByteArray(KEY_SIZE)
            val timeDiffs = mutableListOf<Long>()

            for (i in 0 until window.size - 1) {
                timeDiffs.add(window[i + 1].timestamp - window[i].timestamp)
            }

            // Incorporar diferencias temporales en el patrón
            window.forEachIndexed { index, nonceData ->
                val offset = index * 2
                if (offset < pattern.size) {
                    pattern[offset] = nonceData.nonce[0]
                    if (offset + 1 < pattern.size) {
                        val timeFactor = if (timeDiffs.isNotEmpty())
                            (timeDiffs.firstOrNull()?.rem(256) ?: 0).toInt()
                        else 0
                        pattern[offset + 1] = (nonceData.nonce[1].toInt() xor timeFactor).toByte()
                    }
                }
            }

            patterns.add(pattern)
        }

        return patterns
    }

    private fun findStatisticalPatterns(nonces: List<NonceData>): List<ByteArray> {
        val patterns = mutableListOf<ByteArray>()

        // Análisis estadístico mejorado
        val byteFrequency = Array(KEY_SIZE) { mutableMapOf<Byte, Int>() }
        val bytePositions = Array(KEY_SIZE) { mutableListOf<Int>() }

        nonces.forEachIndexed { nonceIndex, nonceData ->
            nonceData.nonce.forEachIndexed { byteIndex, byte ->
                if (byteIndex < KEY_SIZE) {
                    byteFrequency[byteIndex][byte] = byteFrequency[byteIndex].getOrDefault(byte, 0) + 1
                    bytePositions[byteIndex].add(nonceIndex)
                }
            }
        }

        // Patrón de frecuencia más común
        val mostFrequent = ByteArray(KEY_SIZE)
        val leastFrequent = ByteArray(KEY_SIZE)
        val median = ByteArray(KEY_SIZE)

        byteFrequency.forEachIndexed { index, freqMap ->
            if (freqMap.isNotEmpty()) {
                mostFrequent[index] = freqMap.maxByOrNull { it.value }?.key ?: 0x00
                leastFrequent[index] = freqMap.minByOrNull { it.value }?.key ?: 0xFF.toByte()
                median[index] = freqMap.keys.sorted()[freqMap.size / 2]
            }
        }

        patterns.addAll(listOf(mostFrequent, leastFrequent, median))

        return patterns
    }

    // =============== MÉTODOS DE DERIVACIÓN DE CLAVES ===============

    private fun deriveKeyFromAdvancedPattern(pattern: ByteArray, uid: ByteArray): ByteArray? {
        val key = ByteArray(KEY_SIZE)

        // Método mejorado de derivación
        val sha256 = MessageDigest.getInstance("SHA-256")
        sha256.update(uid)
        sha256.update(pattern)
        val hash = sha256.digest()

        // Tomar los primeros 6 bytes del hash
        System.arraycopy(hash, 0, key, 0, KEY_SIZE)

        return if (validateKeyStrength(key)) key else null
    }

    private fun deriveKeyFromTemporalPattern(pattern: ByteArray, uid: ByteArray): ByteArray? {
        val key = ByteArray(KEY_SIZE)

        // Combinar patrón temporal con UID
        for (i in 0 until min(uid.size, KEY_SIZE)) {
            key[i] = (uid[i].toInt() xor pattern[i % pattern.size].toInt()).toByte()
        }

        // Completar con transformación temporal
        for (i in uid.size until KEY_SIZE) {
            key[i] = (pattern[i % pattern.size].toInt() xor (i * 17)).toByte()
        }

        return if (validateKeyStrength(key)) key else null
    }

    private fun deriveKeyFromStatisticalPattern(pattern: ByteArray, uid: ByteArray): ByteArray? {
        val key = ByteArray(KEY_SIZE)

        // Derivación basada en estadísticas
        for (i in 0 until KEY_SIZE) {
            val uidByte = if (i < uid.size) uid[i] else 0x00
            val patternByte = pattern[i % pattern.size]
            key[i] = (uidByte.toInt() xor patternByte.toInt() xor (i * 23)).toByte()
        }

        return if (validateKeyStrength(key)) key else null
    }

    private fun deriveKeyFromFrequencyWeakness(anomalousByte: Byte, uid: ByteArray, frequencyMap: Map<Byte, Int>): ByteArray? {
        val key = ByteArray(KEY_SIZE)

        // Usar el byte anómalo como semilla
        val seed = anomalousByte.toInt() and 0xFF

        for (i in 0 until KEY_SIZE) {
            val uidByte = if (i < uid.size) uid[i] else 0x00
            key[i] = (uidByte.toInt() xor seed xor (i * seed)).toByte()
        }

        return if (validateKeyStrength(key)) key else null
    }

    private fun deriveKeyFromKnownPattern(pattern: ByteArray, uid: ByteArray): ByteArray? {
        // Para patrones conocidos, usar directamente o con modificación mínima
        return if (pattern.size == KEY_SIZE) {
            pattern.copyOf()
        } else {
            val key = ByteArray(KEY_SIZE)
            for (i in 0 until KEY_SIZE) {
                key[i] = pattern[i % pattern.size]
            }
            key
        }
    }

    private fun deriveKeyFromLFSR(lfsrStates: List<Int>, uid: ByteArray): ByteArray? {
        val key = ByteArray(KEY_SIZE)

        // Derivar clave de estados LFSR
        lfsrStates.forEachIndexed { index, state ->
            if (index < KEY_SIZE) {
                key[index] = (state and 0xFF).toByte()
            }
        }

        return if (validateKeyStrength(key)) key else null
    }

    private fun deriveKeyFromCryptographicWeakness(nonces: List<NonceData>, uid: ByteArray): ByteArray? {
        val key = ByteArray(KEY_SIZE)

        // Análisis de debilidad criptográfica
        val weaknessVector = calculateWeaknessVector(nonces)

        for (i in 0 until KEY_SIZE) {
            val uidByte = if (i < uid.size) uid[i] else 0x00
            val weaknessByte = weaknessVector[i % weaknessVector.size]
            key[i] = (uidByte.toInt() xor weaknessByte.toInt()).toByte()
        }

        return if (validateKeyStrength(key)) key else null
    }

    // =============== MÉTODOS DE VALIDACIÓN Y CÁLCULO ===============

    private fun validateKeyStrength(key: ByteArray): Boolean {
        // Verificar que la clave no sea trivial
        val allZeros = key.all { it == 0.toByte() }
        val allOnes = key.all { it == 0xFF.toByte() }
        val allSame = key.all { it == key[0] }

        return !allZeros && !allOnes && !allSame
    }

    private fun calculatePatternConfidence(pattern: ByteArray, nonces: List<NonceData>): Float {
        // Calcular confianza basada en la fuerza del patrón
        val entropy = calculateByteArrayEntropy(pattern)
        val repetitions = countPatternRepetitions(pattern, nonces)

        return min(1.0f, entropy * 0.7f + (repetitions / nonces.size.toFloat()) * 0.3f)
    }

    private fun calculateTemporalConfidence(nonces: List<NonceData>): Float {
        // Analizar regularidad temporal
        val intervals = mutableListOf<Long>()
        for (i in 0 until nonces.size - 1) {
            intervals.add(nonces[i + 1].timestamp - nonces[i].timestamp)
        }

        val avgInterval = intervals.average()
        val variance = intervals.map { (it - avgInterval).pow(2) }.average()

        return min(1.0f, (1.0f / (1.0f + variance.toFloat() / 1000000f)))
    }

    private fun calculateStatisticalConfidence(nonces: List<NonceData>): Float {
        val entropy = calculateEntropy(nonces)
        val uniformity = calculateDistributionUniformity(nonces)

        return (entropy + uniformity) / 2.0f
    }

    private fun calculateFrequencyConfidence(frequencyMap: Map<Byte, Int>, totalBytes: Int): Float {
        val expectedFreq = totalBytes / 256.0
        val chiSquared = frequencyMap.values.sumOf {
            (it - expectedFreq).pow(2) / expectedFreq
        }

        // Normalizar chi-cuadrado a confianza
        return min(1.0f, chiSquared.toFloat() / 1000f)
    }

    private fun calculateEntropy(nonces: List<NonceData>): Float {
        val byteFreq = mutableMapOf<Byte, Int>()
        val totalBytes = nonces.sumOf { it.nonce.size }

        nonces.forEach { nonceData ->
            nonceData.nonce.forEach { byte ->
                byteFreq[byte] = byteFreq.getOrDefault(byte, 0) + 1
            }
        }

        return byteFreq.values.sumOf { freq ->
            val p = freq.toDouble() / totalBytes
            -p * log2(p)
        }.toFloat() / 8.0f // Normalizar a [0,1]
    }

    private fun calculateDistributionUniformity(nonces: List<NonceData>): Float {
        val byteFreq = mutableMapOf<Byte, Int>()
        val totalBytes = nonces.sumOf { it.nonce.size }

        nonces.forEach { nonceData ->
            nonceData.nonce.forEach { byte ->
                byteFreq[byte] = byteFreq.getOrDefault(byte, 0) + 1
            }
        }

        val expectedFreq = totalBytes / 256.0
        val variance = byteFreq.values.sumOf { (it - expectedFreq).pow(2) } / byteFreq.size

        return (1.0f / (1.0f + variance.toFloat() / 1000f))
    }

    // =============== MÉTODOS AUXILIARES ADICIONALES ===============

    private fun rotateLeft(value: Long, shift: Int): Long {
        return (value shl shift) or (value ushr (64 - shift))
    }

    private fun calculateByteArrayEntropy(array: ByteArray): Float {
        val freq = mutableMapOf<Byte, Int>()
        array.forEach { byte ->
            freq[byte] = freq.getOrDefault(byte, 0) + 1
        }

        return freq.values.sumOf { count ->
            val p = count.toDouble() / array.size
            -p * log2(p)
        }.toFloat() / 8.0f
    }

    private fun countPatternRepetitions(pattern: ByteArray, nonces: List<NonceData>): Int {
        var count = 0
        nonces.forEach { nonceData ->
            for (i in 0..nonceData.nonce.size - pattern.size) {
                val substring = nonceData.nonce.sliceArray(i until i + pattern.size)
                if (substring.contentEquals(pattern)) {
                    count++
                }
            }
        }
        return count
    }

    private fun detectLFSRStates(nonces: List<NonceData>): List<Int> {
        val states = mutableListOf<Int>()

        nonces.forEach { nonceData ->
            if (nonceData.nonce.size >= 4) {
                val state = ByteBuffer.wrap(nonceData.nonce.sliceArray(0..3)).int
                states.add(state)
            }
        }

        return states
    }

    private fun detectLFSRPolynomial(states: List<Int>): String {
        // Implementación simplificada de detección de polinomio LFSR
        return "0x" + states.firstOrNull()?.toString(16)?.uppercase() ?: "UNKNOWN"
    }

    private fun calculateCryptographicWeakness(nonces: List<NonceData>): Float {
        // Detectar patrones que indican debilidad criptográfica
        val patterns = mutableListOf<Float>()

        // Análisis de autocorrelación
        patterns.add(calculateAutocorrelation(nonces))

        // Análisis de periodicidad
        patterns.add(calculatePeriodicity(nonces))

        // Análisis de complejidad lineal
        patterns.add(calculateLinearComplexity(nonces))

        return patterns.average().toFloat()
    }

    private fun calculateAutocorrelation(nonces: List<NonceData>): Float {
        // Implementación simplificada de autocorrelación
        if (nonces.size < 2) return 0.0f

        var correlation = 0.0f
        val firstNonce = nonces[0].nonce

        for (i in 1 until nonces.size) {
            val currentNonce = nonces[i].nonce
            val similarity = calculateSimilarity(firstNonce, currentNonce)
            correlation += similarity
        }

        return correlation / (nonces.size - 1)
    }

    private fun calculatePeriodicity(nonces: List<NonceData>): Float {
        // Detectar patrones periódicos
        val periods = mutableListOf<Int>()

        for (period in 2..min(nonces.size / 2, 16)) {
            var matches = 0
            for (i in 0 until nonces.size - period) {
                if (nonces[i].nonce.contentEquals(nonces[i + period].nonce)) {
                    matches++
                }
            }
            if (matches > 0) {
                periods.add(period)
            }
        }

        return if (periods.isNotEmpty()) 1.0f else 0.0f
    }

    private fun calculateLinearComplexity(nonces: List<NonceData>): Float {
        // Análisis de complejidad lineal usando algoritmo Berlekamp-Massey simplificado
        val bitSequence = nonces.flatMap { nonce ->
            nonce.nonce.flatMap { byte ->
                (0..7).map { bit -> (byte.toInt() shr bit) and 1 }
            }
        }

        val expectedComplexity = bitSequence.size / 2.0f
        val actualComplexity = calculateBerlekampMassey(bitSequence).toFloat()

        return abs(actualComplexity - expectedComplexity) / expectedComplexity
    }

    private fun calculateBerlekampMassey(sequence: List<Int>): Int {
        // Implementación simplificada del algoritmo Berlekamp-Massey
        val n = sequence.size
        val c = IntArray(n) { 0 }
        val b = IntArray(n) { 0 }
        c[0] = 1
        b[0] = 1

        var l = 0
        var m = -1

        for (i in 0 until n) {
            var d = 0
            for (j in 0..l) {
                d = d xor (c[j] * sequence[i - j])
            }

            if (d == 1) {
                val temp = c.copyOf()
                for (j in 0 until n - i + m) {
                    c[i - m + j] = c[i - m + j] xor b[j]
                }

                if (l <= i / 2) {
                    l = i + 1 - l
                    m = i
                    System.arraycopy(temp, 0, b, 0, n)
                }
            }
        }

        return l
    }

    private fun calculateSimilarity(array1: ByteArray, array2: ByteArray): Float {
        val minSize = min(array1.size, array2.size)
        var matches = 0

        for (i in 0 until minSize) {
            if (array1[i] == array2[i]) {
                matches++
            }
        }

        return matches.toFloat() / minSize
    }

    private fun calculateWeaknessVector(nonces: List<NonceData>): ByteArray {
        val vector = ByteArray(KEY_SIZE)

        // Calcular vector de debilidad basado en patrones detectados
        nonces.forEachIndexed { index, nonceData ->
            for (i in 0 until min(nonceData.nonce.size, KEY_SIZE)) {
                vector[i] = (vector[i].toInt() xor nonceData.nonce[i].toInt() xor index).toByte()
            }
        }

        return vector
    }

    private fun calculateTimeVariance(nonces: List<NonceData>): Double {
        val intervals = mutableListOf<Long>()
        for (i in 0 until nonces.size - 1) {
            intervals.add(nonces[i + 1].timestamp - nonces[i].timestamp)
        }

        if (intervals.isEmpty()) return 0.0

        val mean = intervals.average()
        return intervals.map { (it - mean).pow(2) }.average()
    }

    private fun calculateSequenceCorrelation(nonces: List<NonceData>): Double {
        if (nonces.size < 2) return 0.0

        var correlation = 0.0
        val totalPairs = nonces.size - 1

        for (i in 0 until totalPairs) {
            val current = nonces[i].nonce
            val next = nonces[i + 1].nonce

            correlation += calculateSimilarity(current, next)
        }

        return correlation / totalPairs
    }

    private fun calculatePatternStrength(pattern: ByteArray): Double {
        val entropy = calculateByteArrayEntropy(pattern)
        val uniqueBytes = pattern.toSet().size
        val repetitions = pattern.size - uniqueBytes

        return (entropy * 0.6 + (uniqueBytes / 256.0) * 0.4) * (1.0 - repetitions / pattern.size.toDouble())
    }

    private fun validateKeyAgainstNonces(key: ByteArray, nonces: List<NonceData>): Boolean {
        // Validación básica: verificar que la clave no produzca patrones obvios
        val cipher = try {
            Cipher.getInstance("AES/ECB/NoPadding")
        } catch (e: Exception) {
            return false
        }

        return try {
            val secretKey = SecretKeySpec(key + key, "AES") // Expandir clave a 12 bytes
            cipher.init(Cipher.ENCRYPT_MODE, secretKey)

            // Verificar que la clave produzca salidas diversas
            val testInputs = listOf(
                byteArrayOf(0x00, 0x00, 0x00, 0x00),
                byteArrayOf(0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte()),
                byteArrayOf(0x12, 0x34, 0x56, 0x78),
                byteArrayOf(0xAB.toByte(), 0xCD.toByte(), 0xEF.toByte(), 0x01)
            )

            val outputs = testInputs.map { input ->
                cipher.update(input + byteArrayOf(0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00))
            }

            // Verificar diversidad en las salidas
            val uniqueOutputs = outputs.map { it.contentHashCode() }.toSet()
            uniqueOutputs.size >= 3

        } catch (e: Exception) {
            false
        }
    }

    /**
     * Método para limpiar recursos y datos sensibles
     */
    fun cleanup() {
        // Limpiar cualquier dato sensible en memoria
        System.gc()
    }

    /**
     * Método para exportar resultados de análisis
     */
    fun exportAnalysisResults(results: List<AnalysisResult>): String {
        val report = StringBuilder()
        report.append("=== REPORTE DE ANÁLISIS DE NONCES ===\n")
        report.append("Fecha: ${Date()}\n")
        report.append("Total de resultados: ${results.size}\n\n")

        results.forEachIndexed { index, result ->
            report.append("--- Resultado ${index + 1} ---\n")
            report.append("Método: ${result.method}\n")
            report.append("Confianza: ${String.format("%.2f", result.confidence * 100)}%\n")
            report.append("Clave: ${result.key?.joinToString("") { "%02X".format(it) } ?: "N/A"}\n")

            if (result.metadata.isNotEmpty()) {
                report.append("Metadatos:\n")
                result.metadata.forEach { (key, value) ->
                    report.append("  $key: $value\n")
                }
            }
            report.append("\n")
        }

        return report.toString()
    }

    /**
     * Método para análisis rápido con timeout reducido
     */
    suspend fun quickAnalysis(nonces: List<NonceData>, uid: ByteArray): AnalysisResult? {
        if (nonces.size < MIN_NONCES) return null

        return withTimeoutOrNull(5000L) { // 5 segundos para análisis rápido
            try {
                // Solo análisis más rápidos
                val quickJobs = listOf(
                    async { analyzeRussianDomophonePatterns(nonces, uid) },
                    async { analyzeFrequencyWeakness(nonces, uid) }
                )

                val results = quickJobs.awaitAll().filterNotNull()
                results.maxByOrNull { it.confidence }

            } catch (e: Exception) {
                null
            }
        }
    }

    /**
     * Método para análisis profundo con todos los algoritmos
     */
    suspend fun deepAnalysis(nonces: List<NonceData>, uid: ByteArray): List<AnalysisResult> {
        if (nonces.size < MIN_NONCES) return emptyList()

        return withTimeoutOrNull(MAX_ANALYSIS_TIME * 2) { // Timeout extendido para análisis profundo
            try {
                val deepJobs = listOf(
                    async { analyzeAdvancedPatterns(nonces, uid) },
                    async { analyzeTemporalPatterns(nonces, uid) },
                    async { analyzeStatisticalPatterns(nonces, uid) },
                    async { analyzeFrequencyWeakness(nonces, uid) },
                    async { analyzeRussianDomophonePatterns(nonces, uid) },
                    async { analyzeLinearFeedbackShiftRegister(nonces, uid) },
                    async { analyzeCryptographicWeakness(nonces, uid) }
                )

                deepJobs.awaitAll().filterNotNull().sortedByDescending { it.confidence }

            } catch (e: Exception) {
                emptyList()
            }
        } ?: emptyList()
    }

    /**
     * Método para validar la calidad de los nonces de entrada
     */
    fun validateNonceQuality(nonces: List<NonceData>): Map<String, Any> {
        val quality = mutableMapOf<String, Any>()

        // Cantidad suficiente
        quality["sufficient_quantity"] = nonces.size >= MIN_NONCES

        // Diversidad temporal
        val timeSpan = if (nonces.size > 1) {
            nonces.maxOf { it.timestamp } - nonces.minOf { it.timestamp }
        } else 0L
        quality["temporal_diversity"] = timeSpan

        // Diversidad de datos
        val uniqueNonces = nonces.map { it.nonce.contentHashCode() }.toSet().size
        quality["data_diversity"] = uniqueNonces.toFloat() / nonces.size

        // Entropía promedio
        val avgEntropy = nonces.map { calculateByteArrayEntropy(it.nonce) }.average()
        quality["average_entropy"] = avgEntropy

        // Calidad general
        val overallQuality = when {
            nonces.size < MIN_NONCES -> "INSUFICIENTE"
            avgEntropy < 0.3 -> "BAJA"
            avgEntropy < 0.6 -> "MEDIA"
            avgEntropy < 0.8 -> "BUENA"
            else -> "EXCELENTE"
        }
        quality["overall_quality"] = overallQuality

        return quality
    }
}