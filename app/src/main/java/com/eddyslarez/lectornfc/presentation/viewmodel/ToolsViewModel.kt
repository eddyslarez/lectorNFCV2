package com.eddyslarez.lectornfc.presentation.viewmodel

import android.nfc.tech.MifareClassic
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eddyslarez.lectornfc.data.repository.MifareRepository
import com.eddyslarez.lectornfc.domain.attacks.*
import com.eddyslarez.lectornfc.utils.*
import com.eddyslarez.lectornfc.data.models.BlockData
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

class ToolsViewModel(
    private val repository: MifareRepository
) : ViewModel() {

    private val _currentTool = MutableStateFlow<String?>(null)
    val currentTool: StateFlow<String?> = _currentTool.asStateFlow()

    private val _toolResult = MutableStateFlow<ToolResult?>(null)
    val toolResult: StateFlow<ToolResult?> = _toolResult.asStateFlow()

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()

    private val keyDictionaries = KeyDictionaries()
    private val mkfKey32 = MKFKey32()
    private val cryptoUtils = CryptoUtils()

    fun openTool(toolId: String) {
        _currentTool.value = toolId
        _toolResult.value = null
    }

    fun closeTool() {
        _currentTool.value = null
        _toolResult.value = null
    }

    // Clone Tool Functions
    fun startCloneSourceRead() {
        viewModelScope.launch {
            _isProcessing.value = true
            try {
                // Simular lectura de tarjeta fuente
                delay(2000)

                // Generar datos de ejemplo para la demostración
                val sampleData = generateSampleCardData()

                _toolResult.value = ToolResult.CloneSourceResult(sampleData)
            } catch (e: Exception) {
                _toolResult.value = ToolResult.Error("Error leyendo tarjeta fuente: ${e.message}")
            } finally {
                _isProcessing.value = false
            }
        }
    }

    fun startCloneWrite(sourceData: List<BlockData>?) {
        if (sourceData == null) {
            _toolResult.value = ToolResult.Error("No hay datos de tarjeta fuente")
            return
        }

        viewModelScope.launch {
            _isProcessing.value = true
            try {
                // Simular proceso de clonado con progreso
                for (i in 1..100) {
                    delay(50)
                    _toolResult.value = ToolResult.CloneProgressResult(i / 100f)
                }

                _toolResult.value = ToolResult.CloneCompleteResult(
                    writtenBlocks = sourceData.size,
                    totalBlocks = sourceData.size
                )
            } catch (e: Exception) {
                _toolResult.value = ToolResult.Error("Error en clonado: ${e.message}")
            } finally {
                _isProcessing.value = false
            }
        }
    }

    // Format Tool Functions
    fun startFormat() {
        viewModelScope.launch {
            _isProcessing.value = true
            try {
                // Simular formateo con progreso
                for (i in 1..100) {
                    delay(30)
                    _toolResult.value = ToolResult.FormatProgressResult(i / 100f)
                }

                _toolResult.value = ToolResult.FormatCompleteResult(
                    formattedBlocks = 64,
                    totalBlocks = 64
                )
            } catch (e: Exception) {
                _toolResult.value = ToolResult.Error("Error en formateo: ${e.message}")
            } finally {
                _isProcessing.value = false
            }
        }
    }

    // Key Generator Tool
    fun generateMKF32Key(uid: String, sector: Int, algorithm: MKFKey32.KeyAlgorithm) {
        viewModelScope.launch {
            _isProcessing.value = true
            try {
                val uidBytes = hexStringToByteArray(uid)
                val result = mkfKey32.generateKey(uidBytes, sector, algorithm)

                _toolResult.value = ToolResult.KeyGeneratorResult(
                    key = result.key.joinToString("") { "%02X".format(it) },
                    entropy = result.entropy,
                    algorithm = result.algorithm,
                    validationScore = result.validationScore,
                    metadata = result.metadata
                )
            } catch (e: Exception) {
                _toolResult.value = ToolResult.Error("Error generando clave: ${e.message}")
            } finally {
                _isProcessing.value = false
            }
        }
    }

    // UID Analyzer Tool
    fun analyzeUID(uid: String) {
        viewModelScope.launch {
            _isProcessing.value = true
            try {
                val uidBytes = hexStringToByteArray(uid)
                val entropy = cryptoUtils.calculateEntropy(uidBytes)
                val hasHighEntropy = cryptoUtils.hasHighEntropy(uidBytes)
                val frequencyAnalysis = cryptoUtils.analyzeFrequency(uidBytes)
                val possibleKeys = keyDictionaries.generateUIDBasedKeys(uidBytes)

                _toolResult.value = ToolResult.UIDAnalysisResult(
                    uid = uid,
                    length = uidBytes.size,
                    entropy = entropy,
                    hasHighEntropy = hasHighEntropy,
                    frequencyAnalysis = frequencyAnalysis.mapKeys { it.key.toString() },
                    possibleKeys = possibleKeys.map { it.joinToString("") { byte -> "%02X".format(byte) } },
                    recommendations = generateUIDRecommendations(uidBytes, entropy)
                )
            } catch (e: Exception) {
                _toolResult.value = ToolResult.Error("Error analizando UID: ${e.message}")
            } finally {
                _isProcessing.value = false
            }
        }
    }

    // Dictionary Manager Tool
    fun getDictionaryStats() {
        viewModelScope.launch {
            _isProcessing.value = true
            try {
                val stats = keyDictionaries.getDictionaryStats()

                _toolResult.value = ToolResult.DictionaryStatsResult(
                    totalKeys = stats["total_keys"] ?: 0,
                    defaultKeys = stats["default_keys"] ?: 0,
                    transportKeys = stats["transport_keys"] ?: 0,
                    accessKeys = stats["access_keys"] ?: 0,
                    russianKeys = stats["russian_keys"] ?: 0,
                    breachedKeys = stats["breached_keys"] ?: 0,
                    weakPatterns = stats["weak_patterns"] ?: 0,
                    vendorKeys = stats["vendor_keys"] ?: 0
                )
            } catch (e: Exception) {
                _toolResult.value = ToolResult.Error("Error obteniendo estadísticas: ${e.message}")
            } finally {
                _isProcessing.value = false
            }
        }
    }

    private fun hexStringToByteArray(hex: String): ByteArray {
        val cleanHex = hex.replace(" ", "").replace(":", "").replace("-", "")
        return cleanHex.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
    }

    private fun generateSampleCardData(): List<BlockData> {
        val sampleData = mutableListOf<BlockData>()

        // Generar datos de ejemplo para 16 sectores
        for (sector in 0 until 16) {
            val blocksInSector = if (sector < 32) 4 else 16
            val firstBlock = if (sector < 32) sector * 4 else 128 + (sector - 32) * 16

            for (i in 0 until blocksInSector) {
                val blockIndex = firstBlock + i
                val isTrailer = (i == blocksInSector - 1)

                // Generar datos de ejemplo
                val data = if (isTrailer) {
                    // Datos de trailer con claves por defecto
                    byteArrayOf(0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(),
                        0xFF.toByte(), 0x07.toByte(), 0x80.toByte(), 0x69.toByte(),
                        0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte())
                } else {
                    // Datos de ejemplo para bloques normales
                    ByteArray(16) { (it + blockIndex).toByte() }
                }

                sampleData.add(
                    BlockData(
                        sector = sector,
                        block = blockIndex,
                        data = data,
                        isTrailer = isTrailer,
                        keyUsed = byteArrayOf(0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte()),
                        keyType = "A",
                        cracked = true
                    )
                )
            }
        }

        return sampleData
    }

    private fun generateUIDRecommendations(uid: ByteArray, entropy: Double): List<String> {
        val recommendations = mutableListOf<String>()

        if (entropy < 4.0) {
            recommendations.add("UID tiene baja entropía - posible patrón secuencial")
        }

        if (uid.all { it == uid[0] }) {
            recommendations.add("UID contiene bytes repetidos - posible clon o programación manual")
        }

        if (uid.size == 4) {
            recommendations.add("UID de 4 bytes - tarjeta Mifare Classic estándar")
        } else if (uid.size == 7) {
            recommendations.add("UID de 7 bytes - tarjeta Mifare con UID doble")
        }

        return recommendations
    }

    // Clases de datos para resultados
    sealed class ToolResult {
        data class KeyGeneratorResult(
            val key: String,
            val entropy: Float,
            val algorithm: String,
            val validationScore: Float,
            val metadata: Map<String, Any>
        ) : ToolResult()

        data class UIDAnalysisResult(
            val uid: String,
            val length: Int,
            val entropy: Double,
            val hasHighEntropy: Boolean,
            val frequencyAnalysis: Map<String, Double>,
            val possibleKeys: List<String>,
            val recommendations: List<String>
        ) : ToolResult()

        data class CloneSourceResult(
            val cardData: List<BlockData>
        ) : ToolResult()

        data class CloneProgressResult(
            val progress: Float
        ) : ToolResult()

        data class CloneCompleteResult(
            val writtenBlocks: Int,
            val totalBlocks: Int
        ) : ToolResult()

        data class FormatProgressResult(
            val progress: Float
        ) : ToolResult()

        data class FormatCompleteResult(
            val formattedBlocks: Int,
            val totalBlocks: Int
        ) : ToolResult()

        data class DictionaryStatsResult(
            val totalKeys: Int,
            val defaultKeys: Int,
            val transportKeys: Int,
            val accessKeys: Int,
            val russianKeys: Int,
            val breachedKeys: Int,
            val weakPatterns: Int,
            val vendorKeys: Int
        ) : ToolResult()

        data class Error(val message: String) : ToolResult()
    }
}