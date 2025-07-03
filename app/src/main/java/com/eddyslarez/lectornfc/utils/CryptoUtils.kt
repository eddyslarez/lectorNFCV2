package com.eddyslarez.lectornfc.utils


import android.util.Base64
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec
import kotlin.math.log2
import kotlin.math.pow

class CryptoUtils {

    companion object {
        private const val AES_TRANSFORMATION = "AES/GCM/NoPadding"
        private const val AES_KEY_LENGTH = 256
        private const val GCM_IV_LENGTH = 12
        private const val GCM_TAG_LENGTH = 16
    }

    private val secureRandom = SecureRandom()

    /**
     * Calcula el hash SHA-256 de un array de bytes
     */
    fun sha256(input: ByteArray): ByteArray {
        val digest = MessageDigest.getInstance("SHA-256")
        return digest.digest(input)
    }

    /**
     * Calcula el hash SHA-256 de una cadena
     */
    fun sha256(input: String): String {
        val hash = sha256(input.toByteArray())
        return hash.joinToString("") { "%02x".format(it) }
    }

    /**
     * Calcula el hash MD5 de un array de bytes
     */
    fun md5(input: ByteArray): ByteArray {
        val digest = MessageDigest.getInstance("MD5")
        return digest.digest(input)
    }

    /**
     * Calcula el hash MD5 de una cadena
     */
    fun md5(input: String): String {
        val hash = md5(input.toByteArray())
        return hash.joinToString("") { "%02x".format(it) }
    }

    /**
     * Genera una clave AES aleatoria
     */
    fun generateAESKey(): ByteArray {
        val keyGenerator = KeyGenerator.getInstance("AES")
        keyGenerator.init(AES_KEY_LENGTH)
        return keyGenerator.generateKey().encoded
    }

    /**
     * Cifra datos usando AES-GCM
     */
    fun encryptAES(data: ByteArray, key: ByteArray): EncryptionResult {
        val cipher = Cipher.getInstance(AES_TRANSFORMATION)
        val keySpec = SecretKeySpec(key, "AES")

        val iv = ByteArray(GCM_IV_LENGTH)
        secureRandom.nextBytes(iv)

        val gcmSpec = GCMParameterSpec(GCM_TAG_LENGTH * 8, iv)
        cipher.init(Cipher.ENCRYPT_MODE, keySpec, gcmSpec)

        val encryptedData = cipher.doFinal(data)

        return EncryptionResult(encryptedData, iv)
    }

    /**
     * Descifra datos usando AES-GCM
     */
    fun decryptAES(encryptedData: ByteArray, key: ByteArray, iv: ByteArray): ByteArray {
        val cipher = Cipher.getInstance(AES_TRANSFORMATION)
        val keySpec = SecretKeySpec(key, "AES")
        val gcmSpec = GCMParameterSpec(GCM_TAG_LENGTH * 8, iv)

        cipher.init(Cipher.DECRYPT_MODE, keySpec, gcmSpec)
        return cipher.doFinal(encryptedData)
    }

    /**
     * Genera un salt aleatorio
     */
    fun generateSalt(length: Int = 32): ByteArray {
        val salt = ByteArray(length)
        secureRandom.nextBytes(salt)
        return salt
    }

    /**
     * Deriva una clave usando PBKDF2
     */
    fun deriveKey(password: String, salt: ByteArray, iterations: Int = 10000, keyLength: Int = 32): ByteArray {
        val factory = javax.crypto.SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val spec = javax.crypto.spec.PBEKeySpec(password.toCharArray(), salt, iterations, keyLength * 8)
        return factory.generateSecret(spec).encoded
    }

    /**
     * Calcula la entropía de un array de bytes
     */
    fun calculateEntropy(data: ByteArray): Double {
        if (data.isEmpty()) return 0.0

        val frequencies = mutableMapOf<Byte, Int>()
        data.forEach { byte ->
            frequencies[byte] = frequencies.getOrDefault(byte, 0) + 1
        }

        val length = data.size.toDouble()
        return frequencies.values.sumOf { frequency ->
            val probability = frequency / length
            if (probability > 0) -probability * log2(probability) else 0.0
        }
    }

    /**
     * Verifica si los datos tienen alta entropía (son aleatorios)
     */
    fun hasHighEntropy(data: ByteArray, threshold: Double = 7.0): Boolean {
        return calculateEntropy(data) >= threshold
    }

    /**
     * Genera un número aleatorio criptográficamente seguro
     */
    fun generateSecureRandom(length: Int): ByteArray {
        val random = ByteArray(length)
        secureRandom.nextBytes(random)
        return random
    }

    /**
     * Convierte bytes a Base64
     */
    fun toBase64(data: ByteArray): String {
        return Base64.encodeToString(data, Base64.NO_WRAP)
    }

    /**
     * Convierte Base64 a bytes
     */
    fun fromBase64(base64: String): ByteArray {
        return Base64.decode(base64, Base64.NO_WRAP)
    }

    /**
     * XOR de dos arrays de bytes
     */
    fun xor(a: ByteArray, b: ByteArray): ByteArray {
        val result = ByteArray(minOf(a.size, b.size))
        for (i in result.indices) {
            result[i] = (a[i].toInt() xor b[i].toInt()).toByte()
        }
        return result
    }

    /**
     * Rotación circular a la izquierda
     */
    fun rotateLeft(value: Byte, positions: Int): Byte {
        val intValue = value.toInt() and 0xFF
        val normalizedPositions = positions % 8
        return ((intValue shl normalizedPositions) or (intValue shr (8 - normalizedPositions))).toByte()
    }

    /**
     * Rotación circular a la derecha
     */
    fun rotateRight(value: Byte, positions: Int): Byte {
        val intValue = value.toInt() and 0xFF
        val normalizedPositions = positions % 8
        return ((intValue shr normalizedPositions) or (intValue shl (8 - normalizedPositions))).toByte()
    }

    /**
     * Análisis de frecuencia de bytes
     */
    fun analyzeFrequency(data: ByteArray): Map<Byte, Double> {
        val frequencies = mutableMapOf<Byte, Int>()
        data.forEach { byte ->
            frequencies[byte] = frequencies.getOrDefault(byte, 0) + 1
        }

        val total = data.size.toDouble()
        return frequencies.mapValues { it.value / total }
    }

    /**
     * Calcula el checksum CRC32
     */
    fun crc32(data: ByteArray): Long {
        val crc = java.util.zip.CRC32()
        crc.update(data)
        return crc.value
    }

    /**
     * Verifica la integridad de los datos usando checksum
     */
    fun verifyIntegrity(data: ByteArray, expectedChecksum: Long): Boolean {
        return crc32(data) == expectedChecksum
    }

    /**
     * Genera un UUID aleatorio como bytes
     */
    fun generateUUID(): ByteArray {
        val uuid = java.util.UUID.randomUUID()
        val bb = java.nio.ByteBuffer.wrap(ByteArray(16))
        bb.putLong(uuid.mostSignificantBits)
        bb.putLong(uuid.leastSignificantBits)
        return bb.array()
    }

    /**
     * Limpia datos sensibles de la memoria
     */
    fun clearSensitiveData(data: ByteArray) {
        data.fill(0)
    }

    /**
     * Limpia datos sensibles de una cadena (convertida a char array)
     */
    fun clearSensitiveData(data: CharArray) {
        data.fill('\u0000')
    }

    /**
     * Calcula la distancia de Hamming entre dos arrays
     */
    fun hammingDistance(a: ByteArray, b: ByteArray): Int {
        if (a.size != b.size) throw IllegalArgumentException("Arrays must have the same length")

        return a.zip(b).sumOf { (byteA, byteB) ->
            (byteA.toInt() xor byteB.toInt()).countOneBits()
        }
    }

    /**
     * Genera claves derivadas para diferentes propósitos
     */
    fun deriveMultipleKeys(masterKey: ByteArray, contexts: List<String>): Map<String, ByteArray> {
        return contexts.associateWith { context ->
            val contextBytes = context.toByteArray()
            val combined = masterKey + contextBytes
            sha256(combined)
        }
    }

    /**
     * Validación de fortaleza de clave
     */
    fun validateKeyStrength(key: ByteArray): KeyStrengthResult {
        val entropy = calculateEntropy(key)
        val uniqueBytes = key.toSet().size
        val hasPatterns = detectPatterns(key)

        val strength = when {
            entropy < 4.0 || hasPatterns -> KeyStrength.WEAK
            entropy < 6.0 || uniqueBytes < key.size / 2 -> KeyStrength.MEDIUM
            entropy >= 7.0 && uniqueBytes > key.size * 0.7 -> KeyStrength.STRONG
            else -> KeyStrength.MEDIUM
        }

        return KeyStrengthResult(
            strength = strength,
            entropy = entropy,
            uniqueBytes = uniqueBytes,
            hasPatterns = hasPatterns
        )
    }

    private fun detectPatterns(data: ByteArray): Boolean {
        if (data.size < 2) return false

        // Detectar si todos los bytes son iguales
        val allSame = data.all { it == data[0] }

        // Detectar secuencias incrementales
        val sequential = data.toList().zipWithNext().all { (a, b) -> b.toInt() - a.toInt() == 1 }

        // Detectar patrones de repetición
        val repeating = if (data.size >= 4) {
            val half = data.size / 2
            data.take(half).toByteArray().contentEquals(data.drop(half).take(half).toByteArray())
        } else false

        return allSame || sequential || repeating
    }

    data class EncryptionResult(
        val encryptedData: ByteArray,
        val iv: ByteArray
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as EncryptionResult

            if (!encryptedData.contentEquals(other.encryptedData)) return false
            if (!iv.contentEquals(other.iv)) return false

            return true
        }

        override fun hashCode(): Int {
            var result = encryptedData.contentHashCode()
            result = 31 * result + iv.contentHashCode()
            return result
        }
    }

    data class KeyStrengthResult(
        val strength: KeyStrength,
        val entropy: Double,
        val uniqueBytes: Int,
        val hasPatterns: Boolean
    )

    enum class KeyStrength {
        WEAK, MEDIUM, STRONG
    }
}