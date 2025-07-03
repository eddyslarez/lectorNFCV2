package com.eddyslarez.lectornfc.utils


import android.content.Context
import android.nfc.NfcAdapter
import android.nfc.tech.MifareClassic
import java.security.SecureRandom

class NFCHelper(private val context: Context? = null) {

    companion object {
        // Claves más comunes de Mifare Classic
        private val COMMON_KEYS = listOf(
            // Claves por defecto
            byteArrayOf(0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte()),
            byteArrayOf(0x00, 0x00, 0x00, 0x00, 0x00, 0x00),
            byteArrayOf(0xA0.toByte(), 0xA1.toByte(), 0xA2.toByte(), 0xA3.toByte(), 0xA4.toByte(), 0xA5.toByte()),
            byteArrayOf(0xB0.toByte(), 0xB1.toByte(), 0xB2.toByte(), 0xB3.toByte(), 0xB4.toByte(), 0xB5.toByte()),

            // Claves de transporte
            byteArrayOf(0x4D, 0x3A, 0x99.toByte(), 0xC3.toByte(), 0x51, 0xDD.toByte()),
            byteArrayOf(0x1A, 0x98.toByte(), 0x2C, 0x7E, 0x45, 0x9A.toByte()),
            byteArrayOf(0xD3.toByte(), 0xF7.toByte(), 0xD3.toByte(), 0xF7.toByte(), 0xD3.toByte(), 0xF7.toByte()),
            byteArrayOf(0xAA.toByte(), 0xBB.toByte(), 0xCC.toByte(), 0xDD.toByte(), 0xEE.toByte(), 0xFF.toByte()),

            // Claves de domófonos rusos
            byteArrayOf(0x48, 0x4F, 0x54, 0x45, 0x4C, 0x31), // "HOTEL1"
            byteArrayOf(0x52, 0x4F, 0x4F, 0x4D, 0x4B, 0x59), // "ROOMKY"
            byteArrayOf(0x47, 0x55, 0x45, 0x53, 0x54, 0x31), // "GUEST1"
            byteArrayOf(0x4B, 0x45, 0x59, 0x43, 0x41, 0x52), // "KEYCAR"

            // Claves específicas de Vizit/Cyfral
            byteArrayOf(0x56, 0x49, 0x7A, 0x49, 0x54, 0x31), // "VIZIT1"
            byteArrayOf(0x43, 0x79, 0x66, 0x72, 0x61, 0x6C), // "Cyfral"

            // Claves numéricas comunes
            byteArrayOf(0x12, 0x34, 0x56, 0x78, 0x9A.toByte(), 0xBC.toByte()),
            byteArrayOf(0x01, 0x23, 0x45, 0x67, 0x89.toByte(), 0xAB.toByte()),
            byteArrayOf(0x11, 0x22, 0x33, 0x44, 0x55, 0x66),
            byteArrayOf(0x31, 0x32, 0x33, 0x34, 0x35, 0x36), // "123456"

            // Patrones débiles
            byteArrayOf(0x01, 0x02, 0x03, 0x04, 0x05, 0x06),
            byteArrayOf(0xFE.toByte(), 0xDC.toByte(), 0xBA.toByte(), 0x98.toByte(), 0x76, 0x54)
        )
    }

    private val nfcAdapter: NfcAdapter? by lazy {
        context?.let { NfcAdapter.getDefaultAdapter(it) }
    }

    fun isNfcEnabled(): Boolean {
        return nfcAdapter?.isEnabled == true
    }

    fun isNfcSupported(): Boolean {
        return nfcAdapter != null
    }

    fun getCommonKeys(): List<ByteArray> {
        return COMMON_KEYS.map { it.clone() }
    }

    fun getCardInfo(mifare: MifareClassic): CardInfo {
        return try {
            mifare.connect()

            val uid = mifare.tag.id
            val size = mifare.size
            val sectorCount = mifare.sectorCount
            val blockCount = mifare.blockCount
            val type = when (mifare.type) {
                MifareClassic.TYPE_CLASSIC -> "Mifare Classic"
                MifareClassic.TYPE_PLUS -> "Mifare Plus"
                MifareClassic.TYPE_PRO -> "Mifare Pro"
                else -> "Desconocido"
            }

            CardInfo(
                uid = uid.joinToString("") { "%02X".format(it) },
                size = size,
                sectorCount = sectorCount,
                blockCount = blockCount,
                type = type,
                isConnected = mifare.isConnected
            )
        } catch (e: Exception) {
            CardInfo(
                uid = "Error",
                size = 0,
                sectorCount = 0,
                blockCount = 0,
                type = "Error: ${e.message}",
                isConnected = false
            )
        } finally {
            try {
                mifare.close()
            } catch (e: Exception) {
                // Ignorar errores al cerrar
            }
        }
    }

    fun authenticateWithKeyA(mifare: MifareClassic, sector: Int, key: ByteArray): Boolean {
        return try {
            mifare.authenticateSectorWithKeyA(sector, key)
        } catch (e: Exception) {
            false
        }
    }

    fun authenticateWithKeyB(mifare: MifareClassic, sector: Int, key: ByteArray): Boolean {
        return try {
            mifare.authenticateSectorWithKeyB(sector, key)
        } catch (e: Exception) {
            false
        }
    }

    fun findWorkingKey(mifare: MifareClassic, sector: Int): WorkingKey? {
        val keys = getCommonKeys()

        for (key in keys) {
            if (authenticateWithKeyA(mifare, sector, key)) {
                return WorkingKey(key, "A")
            }
            if (authenticateWithKeyB(mifare, sector, key)) {
                return WorkingKey(key, "B")
            }
        }

        return null
    }

    fun generateKeyFromUID(uid: ByteArray): List<ByteArray> {
        val keys = mutableListOf<ByteArray>()

        if (uid.size >= 4) {
            // Clave basada en UID directo
            val key1 = ByteArray(6)
            System.arraycopy(uid, 0, key1, 0, minOf(uid.size, 4))
            keys.add(key1)

            // Clave con UID invertido
            val key2 = ByteArray(6)
            val reversedUid = uid.reversedArray()
            System.arraycopy(reversedUid, 0, key2, 0, minOf(reversedUid.size, 4))
            keys.add(key2)

            // Clave con UID XOR
            val key3 = ByteArray(6)
            for (i in 0 until minOf(uid.size, 4)) {
                key3[i] = (uid[i].toInt() xor 0xFF).toByte()
            }
            keys.add(key3)
        }

        return keys
    }

    fun calculateSectorAddress(sector: Int): Int {
        return if (sector < 32) {
            sector * 4
        } else {
            128 + (sector - 32) * 16
        }
    }

    fun getSectorFromBlock(block: Int): Int {
        return if (block < 128) {
            block / 4
        } else {
            32 + (block - 128) / 16
        }
    }

    fun isTrailerBlock(block: Int): Boolean {
        val sector = getSectorFromBlock(block)
        return if (sector < 32) {
            block % 4 == 3
        } else {
            block % 16 == 15
        }
    }

    fun validateKeyFormat(key: String): Boolean {
        // Validar que sea una clave hexadecimal de 12 caracteres
        if (key.length != 12) return false
        return key.all { it.isDigit() || it.uppercaseChar() in 'A'..'F' }
    }

    fun hexStringToByteArray(hex: String): ByteArray {
        val cleanHex = hex.replace(" ", "").replace(":", "")
        return cleanHex.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
    }

    fun byteArrayToHexString(bytes: ByteArray): String {
        return bytes.joinToString("") { "%02X".format(it) }
    }

    fun generateRandomKey(): ByteArray {
        val key = ByteArray(6)
        SecureRandom().nextBytes(key)
        return key
    }

    fun isKeyWeak(key: ByteArray): Boolean {
        // Verificar si la clave es débil
        val allSame = key.all { it == key[0] }
        val sequential = key.toList().zipWithNext().all { (a, b) -> b.toInt() - a.toInt() == 1 }
        val knownWeak = COMMON_KEYS.any { it.contentEquals(key) }

        return allSame || sequential || knownWeak
    }

    fun analyzeKeyStrength(key: ByteArray): KeyStrength {
        return when {
            isKeyWeak(key) -> KeyStrength.WEAK
            key.toSet().size < 4 -> KeyStrength.MEDIUM
            else -> KeyStrength.STRONG
        }
    }

    data class CardInfo(
        val uid: String,
        val size: Int,
        val sectorCount: Int,
        val blockCount: Int,
        val type: String,
        val isConnected: Boolean
    )

    data class WorkingKey(
        val key: ByteArray,
        val type: String // "A" o "B"
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as WorkingKey

            if (!key.contentEquals(other.key)) return false
            if (type != other.type) return false

            return true
        }

        override fun hashCode(): Int {
            var result = key.contentHashCode()
            result = 31 * result + type.hashCode()
            return result
        }
    }

    enum class KeyStrength {
        WEAK, MEDIUM, STRONG
    }
}