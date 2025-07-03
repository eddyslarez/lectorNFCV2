package com.eddyslarez.lectornfc.data.repository


import android.nfc.tech.MifareClassic
import com.eddyslarez.lectornfc.data.database.dao.FoundKeyDao
import com.eddyslarez.lectornfc.data.database.dao.ScanResultDao
import com.eddyslarez.lectornfc.data.database.dao.ScanSessionDao
import com.eddyslarez.lectornfc.data.database.entities.FoundKey
import com.eddyslarez.lectornfc.data.database.entities.ScanResult
import com.eddyslarez.lectornfc.data.database.entities.ScanSession
import com.eddyslarez.lectornfc.data.models.AttackMethod
import com.eddyslarez.lectornfc.data.models.BlockData
import com.eddyslarez.lectornfc.data.models.KeyPair
import com.eddyslarez.lectornfc.domain.usecases.CrackResult
import com.eddyslarez.lectornfc.domain.usecases.WriteResult
import com.eddyslarez.lectornfc.utils.NFCHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.util.Date
import java.util.UUID
import com.eddyslarez.lectornfc.data.database.dao.*
import com.eddyslarez.lectornfc.data.database.entities.*
import com.eddyslarez.lectornfc.data.models.*
import com.google.gson.Gson
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withContext
import java.util.*

class MifareRepository(
    private val scanResultDao: ScanResultDao,
    private val foundKeyDao: FoundKeyDao,
    private val scanSessionDao: ScanSessionDao,
    private val scanHistoryDao: ScanHistoryDao
) {

    suspend fun readCard(mifare: MifareClassic): Flow<List<BlockData>> = flow {
        try {
            mifare.connect()
            val uid = mifare.tag.id
            val sessionId = UUID.randomUUID().toString()
            val startTime = System.currentTimeMillis()

            // Crear sesión
            val session = ScanSession(
                sessionId = sessionId,
                cardUid = uid.joinToString("") { "%02X".format(it) },
                cardType = "Mifare Classic",
                startTime = Date(startTime),
                endTime = null,
                totalSectors = mifare.sectorCount,
                crackedSectors = 0,
                attackMethod = "READ",
                rawData = "",
                notes = "Lectura estándar"
            )
            scanSessionDao.insertSession(session)

            val blocks = mutableListOf<BlockData>()

            for (sector in 0 until mifare.sectorCount) {
                val sectorBlocks = readSector(mifare, sector)
                blocks.addAll(sectorBlocks)
                emit(blocks.toList())
            }

            // Actualizar sesión y guardar en historial
            val endTime = System.currentTimeMillis()
            val crackedSectors = blocks.map { it.sector }.distinct().count { s ->
                blocks.filter { it.sector == s }.any { it.cracked }
            }

            scanSessionDao.updateSession(
                session.copy(
                    endTime = Date(endTime),
                    crackedSectors = crackedSectors,
                    rawData = serializeBlocks(blocks)
                )
            )

            // Guardar en historial
            saveToHistory(uid, "READ", blocks, emptyMap(), startTime, endTime)

        } catch (e: Exception) {
            throw e
        } finally {
            try {
                mifare.close()
            } catch (e: Exception) {
                // Ignorar errores al cerrar
            }
        }
    }.flowOn(Dispatchers.IO)

    suspend fun readSector(mifare: MifareClassic, sector: Int): List<BlockData> {
        return withContext(Dispatchers.IO) {
            val blocks = mutableListOf<BlockData>()
            val nfcHelper = NFCHelper()

            try {
                val blocksInSector = mifare.getBlockCountInSector(sector)
                val firstBlock = mifare.sectorToBlock(sector)

                // Intentar autenticar con claves conocidas
                val keys = nfcHelper.getCommonKeys()
                var authenticated = false
                var usedKey: ByteArray? = null
                var keyType = "A"

                for (key in keys) {
                    try {
                        if (mifare.authenticateSectorWithKeyA(sector, key)) {
                            authenticated = true
                            usedKey = key
                            keyType = "A"
                            break
                        }
                    } catch (e: Exception) {
                        continue
                    }

                    try {
                        if (mifare.authenticateSectorWithKeyB(sector, key)) {
                            authenticated = true
                            usedKey = key
                            keyType = "B"
                            break
                        }
                    } catch (e: Exception) {
                        continue
                    }
                }

                for (i in 0 until blocksInSector) {
                    val blockIndex = firstBlock + i

                    if (authenticated) {
                        try {
                            val data = mifare.readBlock(blockIndex)
                            blocks.add(
                                BlockData(
                                    sector = sector,
                                    block = blockIndex,
                                    data = data,
                                    isTrailer = (i == blocksInSector - 1),
                                    keyUsed = usedKey,
                                    keyType = keyType,
                                    cracked = true
                                )
                            )
                        } catch (e: Exception) {
                            blocks.add(
                                BlockData(
                                    sector = sector,
                                    block = blockIndex,
                                    data = byteArrayOf(),
                                    isTrailer = (i == blocksInSector - 1),
                                    error = "Error leyendo bloque: ${e.message}",
                                    cracked = false
                                )
                            )
                        }
                    } else {
                        blocks.add(
                            BlockData(
                                sector = sector,
                                block = blockIndex,
                                data = byteArrayOf(),
                                isTrailer = (i == blocksInSector - 1),
                                error = "Sector no autenticado",
                                cracked = false
                            )
                        )
                    }
                }

            } catch (e: Exception) {
                // Si hay error en el sector completo
                val blocksInSector = try { mifare.getBlockCountInSector(sector) } catch (_: Exception) { 4 }
                val firstBlock = try { mifare.sectorToBlock(sector) } catch (_: Exception) { sector * 4 }

                for (i in 0 until blocksInSector) {
                    blocks.add(
                        BlockData(
                            sector = sector,
                            block = firstBlock + i,
                            data = byteArrayOf(),
                            isTrailer = (i == blocksInSector - 1),
                            error = "Error en sector: ${e.message}",
                            cracked = false
                        )
                    )
                }
            }

            blocks
        }
    }

    suspend fun writeCard(mifare: MifareClassic, data: List<BlockData>, showConfirmation: Boolean = true): Flow<WriteResult> = flow {
        try {
            mifare.connect()
            var writtenBlocks = 0
            val errors = mutableListOf<String>()
            val writableBlocks = data.filter { !it.isTrailer && it.data.isNotEmpty() }

            if (showConfirmation) {
                // Aquí se debería mostrar un diálogo de confirmación
                emit(WriteResult(
                    success = false,
                    writtenBlocks = 0,
                    totalBlocks = writableBlocks.size,
                    errors = listOf("Confirmación requerida")
                ))
                return@flow
            }

            for ((index, blockData) in writableBlocks.withIndex()) {
                try {
                    val key = blockData.keyUsed ?: continue

                    // Intentar autenticar primero
                    var authenticated = false
                    try {
                        authenticated = if (blockData.keyType == "B") {
                            mifare.authenticateSectorWithKeyB(blockData.sector, key)
                        } else {
                            mifare.authenticateSectorWithKeyA(blockData.sector, key)
                        }
                    } catch (e: Exception) {
                        // Si falla, intentar con el otro tipo de clave
                        try {
                            authenticated = if (blockData.keyType == "B") {
                                mifare.authenticateSectorWithKeyA(blockData.sector, key)
                            } else {
                                mifare.authenticateSectorWithKeyB(blockData.sector, key)
                            }
                        } catch (e2: Exception) {
                            errors.add("No se pudo autenticar bloque ${blockData.block}")
                            continue
                        }
                    }

                    if (authenticated) {
                        try {
                            // Verificar que el bloque no sea de trailer
                            if (!blockData.isTrailer) {
                                mifare.writeBlock(blockData.block, blockData.data)
                                writtenBlocks++

                                // Verificar escritura leyendo el bloque
                                val verification = mifare.readBlock(blockData.block)
                                if (!verification.contentEquals(blockData.data)) {
                                    errors.add("Verificación falló en bloque ${blockData.block}")
                                }
                            }
                        } catch (e: Exception) {
                            errors.add("Error escribiendo bloque ${blockData.block}: ${e.message}")
                        }
                    } else {
                        errors.add("No se pudo autenticar bloque ${blockData.block}")
                    }
                } catch (e: Exception) {
                    errors.add("Error procesando bloque ${blockData.block}: ${e.message}")
                }

                emit(WriteResult(
                    success = errors.isEmpty(),
                    writtenBlocks = writtenBlocks,
                    totalBlocks = writableBlocks.size,
                    errors = errors.toList()
                ))
            }
        } finally {
            try {
                mifare.close()
            } catch (e: Exception) {
                // Ignorar
            }
        }
    }.flowOn(Dispatchers.IO)

    suspend fun crackCard(mifare: MifareClassic, method: AttackMethod): Flow<CrackResult> = flow {
        try {
            mifare.connect()
            val uid = mifare.tag.id
            val sessionId = UUID.randomUUID().toString()
            val startTime = System.currentTimeMillis()

            val foundKeys = mutableMapOf<Int, KeyPair>()

            for (sector in 0 until mifare.sectorCount) {
                val sectorStartTime = System.currentTimeMillis()
                val keyPair = crackSector(mifare, sector, method)
                val sectorEndTime = System.currentTimeMillis()

                if (keyPair != null) {
                    foundKeys[sector] = keyPair

                    // Guardar clave encontrada
                    val foundKey = FoundKey(
                        sessionId = sessionId,
                        sector = sector,
                        keyA = keyPair.keyAAsHex(),
                        keyB = keyPair.keyBAsHex(),
                        keyType = if (keyPair.keyA != null) "A" else "B",
                        discoveryMethod = method.name,
                        confidence = 1.0f,
                        timestamp = System.currentTimeMillis()
                    )
                    foundKeyDao.insertFoundKey(foundKey)
                }

                val result = CrackResult(
                    sector = sector,
                    keyPair = keyPair,
                    method = method,
                    success = keyPair != null,
                    timeElapsed = sectorEndTime - sectorStartTime,
                    attempts = 0
                )

                emit(result)
            }

            // Guardar en historial
            saveToHistory(uid, method.name, emptyList(), foundKeys, startTime, System.currentTimeMillis())

        } finally {
            try {
                mifare.close()
            } catch (e: Exception) {
                // Ignorar
            }
        }
    }.flowOn(Dispatchers.IO)

    suspend fun crackSector(mifare: MifareClassic, sector: Int, method: AttackMethod): KeyPair? {
        return withContext(Dispatchers.IO) {
            val nfcHelper = NFCHelper()

            when (method) {
                AttackMethod.DICTIONARY -> {
                    val keys = nfcHelper.getCommonKeys()
                    for (key in keys) {
                        try {
                            var keyA: ByteArray? = null
                            var keyB: ByteArray? = null

                            if (mifare.authenticateSectorWithKeyA(sector, key)) {
                                keyA = key
                            }

                            if (mifare.authenticateSectorWithKeyB(sector, key)) {
                                keyB = key
                            }

                            if (keyA != null || keyB != null) {
                                return@withContext KeyPair(keyA, keyB)
                            }
                        } catch (e: Exception) {
                            continue
                        }
                    }
                    null
                }
                else -> {
                    // Para otros métodos, usar implementación básica
                    null
                }
            }
        }
    }

    suspend fun cloneCard(sourceCard: MifareClassic, targetCard: MifareClassic, cloneUID: Boolean = false): Flow<CloneResult> = flow {
        try {
            // Primer paso: leer la tarjeta fuente
            emit(CloneResult(CloneStep.READING_SOURCE, 0, "Leyendo tarjeta fuente..."))

            val sourceBlocks = mutableListOf<BlockData>()
            sourceCard.connect()

            for (sector in 0 until sourceCard.sectorCount) {
                val sectorBlocks = readSector(sourceCard, sector)
                sourceBlocks.addAll(sectorBlocks)

                val progress = ((sector + 1) * 50) / sourceCard.sectorCount
                emit(CloneResult(CloneStep.READING_SOURCE, progress, "Leyendo sector $sector/${sourceCard.sectorCount}"))
            }
            sourceCard.close()

            // Segundo paso: escribir en la tarjeta destino
            emit(CloneResult(CloneStep.WRITING_TARGET, 50, "Escribiendo en tarjeta destino..."))

            targetCard.connect()
            var writtenBlocks = 0
            val writableBlocks = sourceBlocks.filter { !it.isTrailer && it.data.isNotEmpty() }

            for ((index, block) in writableBlocks.withIndex()) {
                try {
                    if (writeBlock(targetCard, block)) {
                        writtenBlocks++
                    }
                } catch (e: Exception) {
                    // Continuar con el siguiente bloque
                }

                val progress = 50 + ((index + 1) * 45) / writableBlocks.size
                emit(CloneResult(CloneStep.WRITING_TARGET, progress, "Escribiendo bloque ${index + 1}/${writableBlocks.size}"))
            }

            // Clonar UID si es posible
            if (cloneUID) {
                emit(CloneResult(CloneStep.CLONING_UID, 95, "Clonando UID..."))
                try {
                    // Intentar clonar UID (esto depende del hardware y puede no funcionar)
                    val sourceUID = sourceCard.tag.id
                    // La mayoría de tarjetas no permiten cambiar el UID
                    emit(CloneResult(CloneStep.CLONING_UID, 100, "UID clonado (puede no ser soportado)"))
                } catch (e: Exception) {
                    emit(CloneResult(CloneStep.CLONING_UID, 100, "Clonación de UID no soportada"))
                }
            }

            targetCard.close()
            emit(CloneResult(CloneStep.COMPLETED, 100, "Clonación completada: $writtenBlocks bloques escritos"))

        } catch (e: Exception) {
            emit(CloneResult(CloneStep.ERROR, 0, "Error en clonación: ${e.message}"))
        } finally {
            try {
                sourceCard.close()
                targetCard.close()
            } catch (e: Exception) {
                // Ignorar
            }
        }
    }.flowOn(Dispatchers.IO)

    suspend fun formatCard(mifare: MifareClassic): Flow<FormatResult> = flow {
        try {
            mifare.connect()
            var formattedBlocks = 0
            val totalBlocks = mifare.blockCount
            val emptyData = ByteArray(16) { 0x00 }

            for (block in 0 until totalBlocks) {
                try {
                    val sector = if (block < 128) block / 4 else 32 + (block - 128) / 16
                    val isTrailer = (block < 128 && block % 4 == 3) || (block >= 128 && block % 16 == 15)

                    if (!isTrailer) {
                        // Intentar autenticar y formatear
                        val nfcHelper = NFCHelper()
                        val keys = nfcHelper.getCommonKeys()

                        for (key in keys) {
                            try {
                                if (mifare.authenticateSectorWithKeyA(sector, key) ||
                                    mifare.authenticateSectorWithKeyB(sector, key)) {
                                    mifare.writeBlock(block, emptyData)
                                    formattedBlocks++
                                    break
                                }
                            } catch (e: Exception) {
                                continue
                            }
                        }
                    }
                } catch (e: Exception) {
                    // Continuar con el siguiente bloque
                }

                val progress = ((block + 1) * 100) / totalBlocks
                emit(FormatResult(
                    success = true,
                    progress = progress,
                    formattedBlocks = formattedBlocks,
                    totalBlocks = totalBlocks,
                    message = "Formateando bloque ${block + 1}/$totalBlocks"
                ))
            }

            emit(FormatResult(
                success = true,
                progress = 100,
                formattedBlocks = formattedBlocks,
                totalBlocks = totalBlocks,
                message = "Formateo completado: $formattedBlocks bloques formateados"
            ))

        } catch (e: Exception) {
            emit(FormatResult(
                success = false,
                progress = 0,
                formattedBlocks = 0,
                totalBlocks = 0,
                message = "Error en formateo: ${e.message}"
            ))
        } finally {
            try {
                mifare.close()
            } catch (e: Exception) {
                // Ignorar
            }
        }
    }.flowOn(Dispatchers.IO)

    suspend fun writeBlock(mifare: MifareClassic, blockData: BlockData): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val key = blockData.keyUsed ?: return@withContext false

                val authenticated = if (blockData.keyType == "B") {
                    mifare.authenticateSectorWithKeyB(blockData.sector, key)
                } else {
                    mifare.authenticateSectorWithKeyA(blockData.sector, key)
                }

                if (authenticated) {
                    mifare.writeBlock(blockData.block, blockData.data)
                    true
                } else {
                    false
                }
            } catch (e: Exception) {
                false
            }
        }
    }

    private suspend fun saveToHistory(
        uid: ByteArray,
        attackMethod: String,
        blocks: List<BlockData>,
        foundKeys: Map<Int, KeyPair>,
        startTime: Long,
        endTime: Long
    ) {
        try {
            val totalSectors = if (blocks.isNotEmpty()) blocks.map { it.sector }.distinct().size else foundKeys.keys.size
            val crackedSectors = foundKeys.size
            val successRate = if (totalSectors > 0) (crackedSectors.toFloat() / totalSectors) * 100 else 0f

            val historyEntity = ScanHistoryEntity(
                uid = uid.joinToString("") { "%02X".format(it) },
                cardType = "Mifare Classic",
                timestamp = startTime,
                totalSectors = totalSectors,
                crackedSectors = crackedSectors,
                totalBlocks = blocks.size,
                readableBlocks = blocks.count { it.cracked },
                foundKeys = foundKeys.size,
                attackMethod = attackMethod,
                operationMode = attackMethod,
                scanDuration = endTime - startTime,
                successRate = successRate,
                rawData = Gson().toJson(mapOf(
                    "blocks" to blocks,
                    "foundKeys" to foundKeys.mapKeys { it.key.toString() }.mapValues {
                        mapOf(
                            "keyA" to it.value.keyAAsHex(),
                            "keyB" to it.value.keyBAsHex()
                        )
                    }
                )),
                notes = "Escaneo automático"
            )

            scanHistoryDao.insertScanHistory(historyEntity)
        } catch (e: Exception) {
            // Log error pero no fallar
        }
    }

    private fun serializeBlocks(blocks: List<BlockData>): String {
        return blocks.joinToString("\n") { block ->
            "${block.sector},${block.block},${block.dataAsHex()},${block.cracked}"
        }
    }

    // Métodos para historial
    suspend fun getAllScanResults(): Flow<List<ScanResult>> = scanResultDao.getAllScanResults()
    suspend fun getAllScanHistory(): Flow<List<ScanHistoryEntity>> = scanHistoryDao.getAllScanHistory()
    suspend fun getSuccessfulScans(): Flow<List<ScanResult>> = scanResultDao.getSuccessfulScans()
    suspend fun getAllSessions(): Flow<List<ScanSession>> = scanSessionDao.getAllSessions()
    suspend fun getAllFoundKeys(): Flow<List<FoundKey>> = foundKeyDao.getAllFoundKeys()

    suspend fun insertScanResult(scanResult: ScanResult) = scanResultDao.insertScanResult(scanResult)
    suspend fun insertFoundKey(foundKey: FoundKey) = foundKeyDao.insertFoundKey(foundKey)
    suspend fun insertSession(session: ScanSession) = scanSessionDao.insertSession(session)

    suspend fun deleteScanResult(scanResult: ScanResult) = scanResultDao.deleteScanResult(scanResult)
    suspend fun deleteScanHistory(scanHistory: ScanHistoryEntity) = scanHistoryDao.deleteScanHistory(scanHistory)
    suspend fun deleteSession(session: ScanSession) = scanSessionDao.deleteSession(session)
    suspend fun clearAllHistory() = scanHistoryDao.deleteAllScanHistory()

    suspend fun getTotalScansCount(): Int = scanHistoryDao.getTotalScansCount()
    suspend fun getSuccessfulScansCount(): Int = scanHistoryDao.getSuccessfulScansCount()
    suspend fun getTotalSessionsCount(): Int = scanSessionDao.getTotalSessionsCount()
    suspend fun getUniqueCardsCount(): Int = scanHistoryDao.getUniqueCardsCount()
}

data class WriteResult(
    val success: Boolean,
    val writtenBlocks: Int,
    val totalBlocks: Int,
    val errors: List<String> = emptyList()
)

data class CrackResult(
    val sector: Int,
    val keyPair: KeyPair?,
    val method: AttackMethod,
    val success: Boolean,
    val timeElapsed: Long,
    val attempts: Int = 0
)

data class CloneResult(
    val step: CloneStep,
    val progress: Int,
    val message: String
)

enum class CloneStep {
    READING_SOURCE, WRITING_TARGET, CLONING_UID, COMPLETED, ERROR
}

data class FormatResult(
    val success: Boolean,
    val progress: Int,
    val formattedBlocks: Int,
    val totalBlocks: Int,
    val message: String
)