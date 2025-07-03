package com.eddyslarez.lectornfc.data.repository


import android.nfc.tech.MifareClassic
import android.util.Log
import com.eddyslarez.lectornfc.data.database.dao.*
import com.eddyslarez.lectornfc.data.database.entities.*
import com.eddyslarez.lectornfc.data.models.*
import com.eddyslarez.lectornfc.domain.usecases.CrackResult
import com.eddyslarez.lectornfc.utils.NFCHelper
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
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

            // Crear sesión en base de datos
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

            // Actualizar sesión
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

            // Guardar resultado del escaneo
            val scanResult = ScanResult(
                sessionId = sessionId,
                uid = uid.joinToString("") { "%02X".format(it) },
                cardType = "Mifare Classic",
                sectorCount = mifare.sectorCount,
                crackedSectors = crackedSectors,
                totalBlocks = blocks.size,
                readableBlocks = blocks.count { it.cracked },
                timestamp = Date(),
                duration = endTime - startTime,
                attackMethod = "READ",
                success = crackedSectors > 0,
                notes = "Lectura completada"
            )
            scanResultDao.insertScanResult(scanResult)

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

    suspend fun writeCard(mifare: MifareClassic, data: List<BlockData>): Flow<WriteResult> = flow {
        try {
            mifare.connect()
            var writtenBlocks = 0
            val errors = mutableListOf<String>()
            val writableBlocks = data.filter { !it.isTrailer && it.data.isNotEmpty() }

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

    suspend fun writeBlock(mifare: MifareClassic, blockData: BlockData): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                if (!mifare.isConnected) {
                    mifare.connect()
                }

                val key = blockData.keyUsed ?: return@withContext false

                // Intentar autenticar
                val authenticated = try {
                    if (blockData.keyType == "B") {
                        mifare.authenticateSectorWithKeyB(blockData.sector, key)
                    } else {
                        mifare.authenticateSectorWithKeyA(blockData.sector, key)
                    }
                } catch (e: Exception) {
                    false
                }

                if (authenticated && !blockData.isTrailer) {
                    try {
                        mifare.writeBlock(blockData.block, blockData.data)

                        // Verificar escritura
                        val verification = mifare.readBlock(blockData.block)
                        verification.contentEquals(blockData.data)
                    } catch (e: Exception) {
                        false
                    }
                } else {
                    false
                }
            } catch (e: Exception) {
                false
            } finally {
                try {
                    if (mifare.isConnected) {
                        mifare.close()
                    }
                } catch (e: Exception) {
                    // Ignorar
                }
            }
        }
    }

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
    suspend fun getAllScanHistoryOnce(): List<ScanHistoryEntity> {
        return scanHistoryDao.getAllScanHistoryOnce()
    }

    suspend fun markAsExported(id: Int) {
        scanHistoryDao.markAsExported(id)
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
            Log.d("SaveToHistory", "Saving scan to history...")

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
                notes = "Escaneo automático",
                exported = false

            )

            scanHistoryDao.insertScanHistory(historyEntity)
            Log.d("SaveToHistory", "Scan saved successfully with UID: ${historyEntity.uid}")

        } catch (e: Exception) {
            Log.e("SaveToHistory", "Error saving to history", e)
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