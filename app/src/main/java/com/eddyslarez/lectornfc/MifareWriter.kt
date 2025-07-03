package com.eddyslarez.lectornfc

import android.nfc.tech.MifareClassic
import android.util.Log

class MifareWriter {

    companion object {
        private const val TAG = "MifareWriter"

        // Bloques que NO se deben escribir
        private val PROTECTED_BLOCKS = setOf(0) // Bloque 0 siempre protegido

        // Claves comunes para intentar
        private val COMMON_KEYS = arrayOf(
            byteArrayOf(0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte()),
            byteArrayOf(0x00, 0x00, 0x00, 0x00, 0x00, 0x00),
            byteArrayOf(0xA0.toByte(), 0xA1.toByte(), 0xA2.toByte(), 0xA3.toByte(), 0xA4.toByte(), 0xA5.toByte()),
            byteArrayOf(0xD3.toByte(), 0xF7.toByte(), 0xD3.toByte(), 0xF7.toByte(), 0xD3.toByte(), 0xF7.toByte())
        )
    }

    data class WriteResult(
        val success: Boolean,
        val blockNumber: Int,
        val errorMessage: String? = null,
        val keyUsed: ByteArray? = null
    )

    fun writeToBlock(
        mifareClassic: MifareClassic,
        blockNumber: Int,
        data: ByteArray,
        keys: List<ByteArray> = COMMON_KEYS.toList()
    ): WriteResult {

        // Verificaciones previas
        if (blockNumber in PROTECTED_BLOCKS) {
            return WriteResult(
                success = false,
                blockNumber = blockNumber,
                errorMessage = "El bloque $blockNumber está protegido y no se puede escribir"
            )
        }

        if (data.size != 16) {
            return WriteResult(
                success = false,
                blockNumber = blockNumber,
                errorMessage = "Los datos deben tener exactamente 16 bytes"
            )
        }

        // Verificar si es bloque de sector (trailer)
        if (isSectorTrailerBlock(blockNumber)) {
            Log.w(TAG, "Advertencia: Escribiendo en bloque de sector $blockNumber")
        }

        val sectorIndex = mifareClassic.blockToSector(blockNumber)

        // Intentar con diferentes claves
        for (key in keys) {
            try {
                Log.d(TAG, "Intentando escribir bloque $blockNumber con clave: ${key.toHexString()}")

                // Intentar autenticar con clave A
                var authenticated = mifareClassic.authenticateSectorWithKeyA(sectorIndex, key)

                if (!authenticated) {
                    // Si falla, intentar con clave B
                    authenticated = mifareClassic.authenticateSectorWithKeyB(sectorIndex, key)
                }

                if (authenticated) {
                    Log.d(TAG, "Autenticación exitosa para sector $sectorIndex")

                    // Leer datos actuales para comparar
                    val currentData = try {
                        mifareClassic.readBlock(blockNumber)
                    } catch (e: Exception) {
                        Log.w(TAG, "No se pudo leer bloque $blockNumber antes de escribir", e)
                        null
                    }

                    // Escribir datos
                    mifareClassic.writeBlock(blockNumber, data)
                    Log.d(TAG, "Datos escritos en bloque $blockNumber")

                    // Verificar escritura
                    val verificationData = mifareClassic.readBlock(blockNumber)

                    if (verificationData.contentEquals(data)) {
                        Log.d(TAG, "Verificación exitosa para bloque $blockNumber")
                        return WriteResult(
                            success = true,
                            blockNumber = blockNumber,
                            keyUsed = key
                        )
                    } else {
                        Log.e(TAG, "Verificación falló para bloque $blockNumber")
                        Log.e(TAG, "Esperado: ${data.toHexString()}")
                        Log.e(TAG, "Obtenido: ${verificationData.toHexString()}")

                        return WriteResult(
                            success = false,
                            blockNumber = blockNumber,
                            errorMessage = "Verificación de escritura falló",
                            keyUsed = key
                        )
                    }
                } else {
                    Log.d(TAG, "Autenticación falló para sector $sectorIndex con clave: ${key.toHexString()}")
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error al escribir bloque $blockNumber con clave ${key.toHexString()}", e)

                // Si es un error específico de bloque protegido
                if (e.message?.contains("Authentication") == true ||
                    e.message?.contains("block") == true) {
                    continue // Intentar con siguiente clave
                }

                return WriteResult(
                    success = false,
                    blockNumber = blockNumber,
                    errorMessage = "Error de escritura: ${e.message}",
                    keyUsed = key
                )
            }
        }

        return WriteResult(
            success = false,
            blockNumber = blockNumber,
            errorMessage = "No se pudo autenticar con ninguna clave disponible"
        )
    }

    fun writeMultipleBlocks(
        mifareClassic: MifareClassic,
        blockData: Map<Int, ByteArray>,
        keys: List<ByteArray> = COMMON_KEYS.toList()
    ): List<WriteResult> {
        val results = mutableListOf<WriteResult>()

        for ((blockNumber, data) in blockData) {
            val result = writeToBlock(mifareClassic, blockNumber, data, keys)
            results.add(result)

            // Si falla un bloque crítico, podrías decidir parar
            if (!result.success) {
                Log.w(TAG, "Falló escritura en bloque $blockNumber: ${result.errorMessage}")
            }
        }

        return results
    }

    private fun isSectorTrailerBlock(blockNumber: Int): Boolean {
        return (blockNumber + 1) % 4 == 0
    }

    private fun ByteArray.toHexString(): String {
        return this.joinToString("") { "%02X".format(it) }
    }

    fun getWritableBlocks(totalBlocks: Int): List<Int> {
        val writableBlocks = mutableListOf<Int>()

        for (i in 0 until totalBlocks) {
            if (i !in PROTECTED_BLOCKS && !isSectorTrailerBlock(i)) {
                writableBlocks.add(i)
            }
        }

        return writableBlocks
    }

    fun getSafeDataBlocks(totalBlocks: Int): List<Int> {
        // Devuelve bloques que son seguros para escribir datos de usuario
        return getWritableBlocks(totalBlocks).filter { blockNumber ->
            val sector = blockNumber / 4
            val blockInSector = blockNumber % 4

            // Evitar sector 0 completamente para mayor seguridad
            sector > 0 && blockInSector < 3
        }
    }
}