package com.eddyslarez.lectornfc

import android.nfc.tech.MifareClassic
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import kotlinx.coroutines.*
import kotlin.experimental.xor
import kotlin.math.min
import kotlin.random.Random

object AdvancedMifareManager {
    private var _cardData = mutableStateOf<List<BlockData>>(emptyList())
    val cardData: State<List<BlockData>> = _cardData

    private var _isReading = mutableStateOf(false)
    val isReading: State<Boolean> = _isReading

    private var _isWriting = mutableStateOf(false)
    val isWriting: State<Boolean> = _isWriting

    private var _status = mutableStateOf("Acerca una tarjeta Mifare Classic")
    val status: State<String> = _status

    private var _mode = mutableStateOf(OperationMode.READ)
    val mode: State<OperationMode> = _mode

    private var _progress = mutableStateOf("")
    val progress: State<String> = _progress

    private var _currentSector = mutableStateOf(0)
    val currentSector: State<Int> = _currentSector

    private var _totalSectors = mutableStateOf(0)
    val totalSectors: State<Int> = _totalSectors

    private var _attackMethod = mutableStateOf(AttackMethod.DICTIONARY)
    val attackMethod: State<AttackMethod> = _attackMethod

    private var _foundKeys = mutableStateOf<Map<Int, KeyPair>>(emptyMap())
    val foundKeys: State<Map<Int, KeyPair>> = _foundKeys

    private var _crackedSectors = mutableStateOf<Set<Int>>(emptySet())
    val crackedSectors: State<Set<Int>> = _crackedSectors

    private var storedData: List<BlockData> = emptyList()
    private var uid: ByteArray = byteArrayOf()
    private var atqa: ByteArray = byteArrayOf()
    private var sak: Byte = 0

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    // Diccionarios de claves mejorados
    private val keyDictionaries = KeyDictionaries()
    private val nonceAnalyzer = NonceAnalyzer()
    private val hardnestedAttacker = HardnestedAttacker()
    private val mkfKey32 = MKFKey32()

    fun setMode(newMode: OperationMode) {
        _mode.value = newMode
        _status.value = when (newMode) {
            OperationMode.READ -> "Modo lectura: Acerca una tarjeta para leer"
            OperationMode.WRITE -> "Modo escritura: Acerca una tarjeta para escribir"
            OperationMode.CRACK -> "Modo crack: Acerca una tarjeta para descifrar"
        }
    }

    fun setAttackMethod(method: AttackMethod) {
        _attackMethod.value = method
    }

    fun processNewTag(mifare: MifareClassic) {
        when (_mode.value) {
            OperationMode.READ -> {
                scope.launch {
                    readCardAsync(mifare)
                }
            }
            OperationMode.WRITE -> {
                scope.launch {
                    writeCardAsync(mifare)
                }
            }
            OperationMode.CRACK -> {
                scope.launch {
                    crackCardAsync(mifare)
                }
            }
        }
    }

    private suspend fun crackCardAsync(mifare: MifareClassic) {
        withContext(Dispatchers.IO) {
            _isReading.value = true
            _status.value = "Iniciando ataque avanzado..."
            _progress.value = "Analizando tarjeta..."

            try {
                mifare.connect()
                delay(100)

                // Obtener información de la tarjeta
                uid = mifare.tag.id
                _totalSectors.value = mifare.sectorCount
                _foundKeys.value = emptyMap()
                _crackedSectors.value = emptySet()

                _status.value = "Ejecutando ataque ${_attackMethod.value.name.lowercase()}..."

                when (_attackMethod.value) {
                    AttackMethod.DICTIONARY -> executeDictionaryAttack(mifare)
                    AttackMethod.NONCE -> executeNonceAttack(mifare)
                    AttackMethod.HARDNESTED -> executeHardnestedAttack(mifare)
                    AttackMethod.MKF32 -> executeMKF32Attack(mifare)
                    AttackMethod.COMBINED -> executeCombinedAttack(mifare)
                }

                // Leer datos con las claves encontradas
                readWithFoundKeys(mifare)

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _status.value = "Error en ataque: ${e.message}"
                    _progress.value = "Error"
                }
            } finally {
                try {
                    mifare.close()
                } catch (e: Exception) {
                    // Ignorar errores al cerrar
                }
                withContext(Dispatchers.Main) {
                    _isReading.value = false
                }
            }
        }
    }

    private suspend fun executeDictionaryAttack(mifare: MifareClassic) {
        val keys = keyDictionaries.getAllKeys()
        val foundKeys = mutableMapOf<Int, KeyPair>()
        var processedKeys = 0

        for (sector in 0 until mifare.sectorCount) {
            _currentSector.value = sector
            var sectorCracked = false

            for ((index, key) in keys.withIndex()) {
                if (sectorCracked) break

                processedKeys++
                val progress = (processedKeys.toFloat() / (keys.size * mifare.sectorCount)) * 100

                withContext(Dispatchers.Main) {
                    _progress.value = "Sector $sector: Probando clave ${index + 1}/${keys.size} (${progress.toInt()}%)"
                }

                try {
                    var keyA: ByteArray? = null
                    var keyB: ByteArray? = null

                    // Probar clave A
                    if (mifare.authenticateSectorWithKeyA(sector, key)) {
                        keyA = key
                        sectorCracked = true
                    }

                    // Probar clave B
                    if (mifare.authenticateSectorWithKeyB(sector, key)) {
                        keyB = key
                        sectorCracked = true
                    }

                    if (sectorCracked) {
                        foundKeys[sector] = KeyPair(keyA, keyB)
                        withContext(Dispatchers.Main) {
                            _foundKeys.value = foundKeys.toMap()
                            _crackedSectors.value = foundKeys.keys.toSet()
                            _progress.value = "Sector $sector: ¡CRACKEADO! Clave encontrada"
                        }
                        delay(200) // Pausa para mostrar el éxito
                    }
                } catch (e: Exception) {
                    continue
                }

                if (index % 50 == 0) {
                    delay(10) // Pausa cada 50 intentos
                }
            }
        }
    }

    private suspend fun executeNonceAttack(mifare: MifareClassic) {
        val foundKeys = mutableMapOf<Int, KeyPair>()

        for (sector in 0 until mifare.sectorCount) {
            _currentSector.value = sector
            withContext(Dispatchers.Main) {
                _progress.value = "Sector $sector: Analizando nonces..."
            }

            try {
                val nonces = collectNonces(mifare, sector)
                val key = nonceAnalyzer.analyzeNonces(nonces, uid)
                val analysisResult = nonceAnalyzer.analyzeNonces(nonces, uid)

                if (analysisResult != null) {
                    foundKeys[sector] = KeyPair(analysisResult.key, null)
                    withContext(Dispatchers.Main) {
                        _foundKeys.value = foundKeys.toMap()
                        _crackedSectors.value = foundKeys.keys.toSet()
                        _progress.value = "Sector $sector: ¡CRACKEADO con nonce!"
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _progress.value = "Sector $sector: Error en análisis de nonce"
                }
            }

            delay(100)
        }
    }

    private suspend fun executeHardnestedAttack(mifare: MifareClassic) {
        // Primero necesitamos al menos una clave conocida
        val knownKeys = _foundKeys.value.toMutableMap()

        if (knownKeys.isEmpty()) {
            // Intentar encontrar al menos una clave con diccionario rápido
            val basicKeys = keyDictionaries.getBasicKeys()
            for (sector in 0 until mifare.sectorCount) {
                for (key in basicKeys) {
                    try {
                        if (mifare.authenticateSectorWithKeyA(sector, key)) {
                            knownKeys[sector] = KeyPair(key, null)
                            break
                        }
                    } catch (e: Exception) {
                        continue
                    }
                }
                if (knownKeys.isNotEmpty()) break
            }
        }

        if (knownKeys.isEmpty()) {
            withContext(Dispatchers.Main) {
                _progress.value = "No se encontró clave conocida para hardnested"
            }
            return
        }

        val knownSector = knownKeys.keys.first()
        val knownKey = knownKeys[knownSector]!!.keyA!!

        for (sector in 0 until mifare.sectorCount) {
            if (sector == knownSector) continue

            _currentSector.value = sector
            withContext(Dispatchers.Main) {
                _progress.value = "Sector $sector: Ejecutando hardnested..."
            }

            try {
                val foundKey = hardnestedAttacker.attack(mifare, knownSector, knownKey, sector)
                if (foundKey != null) {
                    knownKeys[sector] = KeyPair(foundKey, null)
                    withContext(Dispatchers.Main) {
                        _foundKeys.value = knownKeys.toMap()
                        _crackedSectors.value = knownKeys.keys.toSet()
                        _progress.value = "Sector $sector: ¡CRACKEADO con hardnested!"
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _progress.value = "Sector $sector: Error en hardnested"
                }
            }

            delay(500)
        }
    }

    private suspend fun executeMKF32Attack(mifare: MifareClassic) {
        val foundKeys = _foundKeys.value.toMutableMap()

        for (sector in 0 until mifare.sectorCount) {
            _currentSector.value = sector
            withContext(Dispatchers.Main) {
                _progress.value = "Sector $sector: Ejecutando MKF32..."
            }

            try {
                // Obtener el resultado completo de MKF32
                val keyResult = mkfKey32.generateKey(
                    uid = uid,
                    sector = sector,
                    algorithm = MKFKey32.KeyAlgorithm.ENHANCED
                )

                // Extraer la clave del resultado
                val key = keyResult.key

                // Probar la clave generada
                val keyASuccess = try {
                    mifare.authenticateSectorWithKeyA(sector, key)
                } catch (e: Exception) {
                    false
                }

                val keyBSuccess = try {
                    mifare.authenticateSectorWithKeyB(sector, key)
                } catch (e: Exception) {
                    false
                }

                if (keyASuccess || keyBSuccess) {
                    foundKeys[sector] = KeyPair(
                        keyA = if (keyASuccess) key else null,
                        keyB = if (keyBSuccess) key else null
                    )

                    withContext(Dispatchers.Main) {
                        _foundKeys.value = foundKeys.toMap()
                        _crackedSectors.value = foundKeys.keys.toSet()
                        _progress.value = "Sector $sector: ¡CRACKEADO con MKF32! (Entropía: ${keyResult.entropy})"
                    }
                } else {
                    // Opcional: probar con otros algoritmos MKF32
                    val algorithms = arrayOf(
                        MKFKey32.KeyAlgorithm.RUSSIAN_DOMOPHONE,
                        MKFKey32.KeyAlgorithm.STATISTICAL,
                        MKFKey32.KeyAlgorithm.ADAPTIVE
                    )

                    var cracked = false
                    for (algorithm in algorithms) {
                        if (cracked) break

                        try {
                            val altKeyResult = mkfKey32.generateKey(uid, sector, algorithm)
                            val altKey = altKeyResult.key

                            val altKeyASuccess = try {
                                mifare.authenticateSectorWithKeyA(sector, altKey)
                            } catch (e: Exception) {
                                false
                            }

                            val altKeyBSuccess = try {
                                mifare.authenticateSectorWithKeyB(sector, altKey)
                            } catch (e: Exception) {
                                false
                            }

                            if (altKeyASuccess || altKeyBSuccess) {
                                foundKeys[sector] = KeyPair(
                                    keyA = if (altKeyASuccess) altKey else null,
                                    keyB = if (altKeyBSuccess) altKey else null
                                )

                                withContext(Dispatchers.Main) {
                                    _foundKeys.value = foundKeys.toMap()
                                    _crackedSectors.value = foundKeys.keys.toSet()
                                    _progress.value = "Sector $sector: ¡CRACKEADO con MKF32 ${algorithm.name}!"
                                }
                                cracked = true
                            }
                        } catch (e: Exception) {
                            continue
                        }
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _progress.value = "Sector $sector: Error en MKF32 - ${e.message}"
                }
            }

            delay(100)
        }
    }

    private suspend fun executeCombinedAttack(mifare: MifareClassic) {
        // Ejecutar todos los ataques en secuencia
        withContext(Dispatchers.Main) {
            _progress.value = "Ejecutando ataque combinado..."
        }

        // 1. Ataque de diccionario básico
        executeDictionaryAttack(mifare)

        // 2. MKF32 para sectores restantes
        executeMKF32Attack(mifare)

        // 3. Hardnested si tenemos claves conocidas
        if (_foundKeys.value.isNotEmpty()) {
            executeHardnestedAttack(mifare)
        }

        // 4. Nonce attack para sectores restantes
        executeNonceAttack(mifare)
    }

    private fun collectNonces(mifare: MifareClassic, sector: Int): List<NonceData> {
        val nonces = mutableListOf<NonceData>()
        val keys = keyDictionaries.getBasicKeys()

        for (key in keys.take(10)) { // Limitar para no ser demasiado lento
            try {
                // Simular recolección de nonces
                val nonce = ByteArray(4)
                Random.nextBytes(nonce)
                nonces.add(NonceData(nonce, 1,key, sector))
            } catch (e: Exception) {
                continue
            }
        }

        return nonces
    }

    private suspend fun readWithFoundKeys(mifare: MifareClassic) {
        val foundKeys = _foundKeys.value
        val blocks = mutableListOf<BlockData>()

        for (sector in 0 until mifare.sectorCount) {
            _currentSector.value = sector
            withContext(Dispatchers.Main) {
                _progress.value = "Leyendo sector $sector..."
            }

            val keyPair = foundKeys[sector]
            if (keyPair != null) {
                try {
                    val key = keyPair.keyA ?: keyPair.keyB!!
                    val keyType = if (keyPair.keyA != null) "A" else "B"

                    val authenticated = if (keyType == "A") {
                        mifare.authenticateSectorWithKeyA(sector, key)
                    } else {
                        mifare.authenticateSectorWithKeyB(sector, key)
                    }

                    if (authenticated) {
                        val blocksInSector = mifare.getBlockCountInSector(sector)
                        val firstBlock = mifare.sectorToBlock(sector)

                        for (i in 0 until blocksInSector) {
                            val blockIndex = firstBlock + i
                            try {
                                val data = mifare.readBlock(blockIndex)
                                blocks.add(
                                    BlockData(
                                        sector = sector,
                                        block = blockIndex,
                                        data = data,
                                        isTrailer = (i == blocksInSector - 1),
                                        keyUsed = key,
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
                                        error = "Error: ${e.message}",
                                        keyType = keyType,
                                        cracked = false
                                    )
                                )
                            }
                        }
                    }
                } catch (e: Exception) {
                    // Sector no accesible
                }
            } else {
                // Sector no crackeado
                val blocksInSector = mifare.getBlockCountInSector(sector)
                val firstBlock = mifare.sectorToBlock(sector)

                for (i in 0 until blocksInSector) {
                    blocks.add(
                        BlockData(
                            sector = sector,
                            block = firstBlock + i,
                            data = byteArrayOf(),
                            isTrailer = (i == blocksInSector - 1),
                            error = "Sector no crackeado",
                            cracked = false
                        )
                    )
                }
            }
        }

        withContext(Dispatchers.Main) {
            _cardData.value = blocks
            storedData = blocks
            _status.value = "Ataque completado: ${_crackedSectors.value.size}/${mifare.sectorCount} sectores crackeados"
            _progress.value = "¡Completado!"
        }
    }

    // Método de lectura normal mejorado
    private suspend fun readCardAsync(mifare: MifareClassic) {
        withContext(Dispatchers.IO) {
            _isReading.value = true
            _status.value = "Conectando con la tarjeta..."
            _progress.value = "Iniciando lectura..."

            try {
                mifare.connect()
                delay(100)

                _totalSectors.value = mifare.sectorCount
                _status.value = "Leyendo ${mifare.sectorCount} sectores..."

                val blocks = mutableListOf<BlockData>()
                val allKeys = keyDictionaries.getAllKeys()

                for (sector in 0 until mifare.sectorCount) {
                    _currentSector.value = sector
                    _progress.value = "Sector $sector/${mifare.sectorCount - 1}"

                    withContext(Dispatchers.Main) {
                        _status.value = "Procesando sector $sector de ${mifare.sectorCount - 1}"
                    }

                    var authenticated = false
                    var usedKey: ByteArray? = null
                    var keyType = "A"

                    // Probar todas las claves
                    for ((index, key) in allKeys.withIndex()) {
                        try {
                            if (mifare.authenticateSectorWithKeyA(sector, key)) {
                                authenticated = true
                                usedKey = key
                                keyType = "A"
                                withContext(Dispatchers.Main) {
                                    _progress.value = "Sector $sector: Autenticado con clave A (${index + 1}/${allKeys.size})"
                                }
                                break
                            }
                        } catch (e: Exception) {
                            continue
                        }

                        if (index % 100 == 0) {
                            delay(10)
                        }
                    }

                    if (!authenticated) {
                        for ((index, key) in allKeys.withIndex()) {
                            try {
                                if (mifare.authenticateSectorWithKeyB(sector, key)) {
                                    authenticated = true
                                    usedKey = key
                                    keyType = "B"
                                    withContext(Dispatchers.Main) {
                                        _progress.value = "Sector $sector: Autenticado con clave B (${index + 1}/${allKeys.size})"
                                    }
                                    break
                                }
                            } catch (e: Exception) {
                                continue
                            }

                            if (index % 100 == 0) {
                                delay(10)
                            }
                        }
                    }

                    if (authenticated) {
                        val blocksInSector = mifare.getBlockCountInSector(sector)
                        val firstBlock = mifare.sectorToBlock(sector)

                        for (i in 0 until blocksInSector) {
                            val blockIndex = firstBlock + i
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
                                        error = "Error: ${e.message}",
                                        keyType = keyType,
                                        cracked = false
                                    )
                                )
                            }
                        }

                        withContext(Dispatchers.Main) {
                            _cardData.value = blocks.toList()
                        }

                    } else {
                        withContext(Dispatchers.Main) {
                            _progress.value = "Sector $sector: No se pudo autenticar"
                        }

                        val blocksInSector = mifare.getBlockCountInSector(sector)
                        val firstBlock = mifare.sectorToBlock(sector)

                        for (i in 0 until blocksInSector) {
                            blocks.add(
                                BlockData(
                                    sector = sector,
                                    block = firstBlock + i,
                                    data = byteArrayOf(),
                                    isTrailer = (i == blocksInSector - 1),
                                    error = "Sector no autenticado",
                                    cracked = false
                                )
                            )
                        }
                    }

                    delay(50)
                }

                withContext(Dispatchers.Main) {
                    _cardData.value = blocks
                    storedData = blocks
                    _status.value = "Lectura completada: ${blocks.size} bloques"
                    _progress.value = "¡Completado!"
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _status.value = "Error al leer tarjeta: ${e.message}"
                    _progress.value = "Error en la lectura"
                }
            } finally {
                try {
                    mifare.close()
                } catch (e: Exception) {
                    // Ignorar errores al cerrar
                }
                withContext(Dispatchers.Main) {
                    _isReading.value = false
                }
            }
        }
    }

    private suspend fun writeCardAsync(mifare: MifareClassic) {
        if (storedData.isEmpty()) {
            _status.value = "No hay datos para escribir. Primero lee una tarjeta."
            return
        }

        withContext(Dispatchers.IO) {
            _isWriting.value = true
            _status.value = "Conectando para escritura..."

            try {
                mifare.connect()
                delay(100)

                var writtenBlocks = 0
                val writableBlocks = storedData.filter { !it.isTrailer && it.data.isNotEmpty() }

                withContext(Dispatchers.Main) {
                    _status.value = "Escribiendo ${writableBlocks.size} bloques..."
                }

                for ((index, blockData) in writableBlocks.withIndex()) {
                    withContext(Dispatchers.Main) {
                        _progress.value = "Escribiendo bloque ${index + 1}/${writableBlocks.size}"
                    }

                    try {
                        val key = blockData.keyUsed ?: keyDictionaries.getBasicKeys()[0]
                        var authenticated = false

                        try {
                            authenticated = if (blockData.keyType == "B") {
                                mifare.authenticateSectorWithKeyB(blockData.sector, key)
                            } else {
                                mifare.authenticateSectorWithKeyA(blockData.sector, key)
                            }
                        } catch (e: Exception) {
                            try {
                                authenticated = if (blockData.keyType == "B") {
                                    mifare.authenticateSectorWithKeyA(blockData.sector, key)
                                } else {
                                    mifare.authenticateSectorWithKeyB(blockData.sector, key)
                                }
                            } catch (e2: Exception) {
                                continue
                            }
                        }

                        if (authenticated) {
                            try {
                                mifare.writeBlock(blockData.block, blockData.data)
                                writtenBlocks++
                            } catch (e: Exception) {
                                continue
                            }
                        }
                    } catch (e: Exception) {
                        continue
                    }

                    delay(50)
                }

                withContext(Dispatchers.Main) {
                    _status.value = "Escritura completada: $writtenBlocks bloques escritos"
                    _progress.value = "¡Escritura completada!"
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _status.value = "Error al escribir tarjeta: ${e.message}"
                    _progress.value = "Error en la escritura"
                }
            } finally {
                try {
                    mifare.close()
                } catch (e: Exception) {
                    // Ignorar errores al cerrar
                }
                withContext(Dispatchers.Main) {
                    _isWriting.value = false
                }
            }
        }
    }

    fun cancelOperation() {
        scope.launch {
            _isReading.value = false
            _isWriting.value = false
            _status.value = "Operación cancelada"
            _progress.value = ""
        }
    }

    fun clearData() {
        _cardData.value = emptyList()
        storedData = emptyList()
        _foundKeys.value = emptyMap()
        _crackedSectors.value = emptySet()
        _status.value = "Datos borrados. Acerca una tarjeta Mifare Classic"
        _progress.value = ""
    }
}

// Clases de datos mejoradas
data class BlockData(
    val sector: Int,
    val block: Int,
    val data: ByteArray,
    val isTrailer: Boolean = false,
    val keyUsed: ByteArray? = null,
    val keyType: String = "A",
    val error: String? = null,
    val cracked: Boolean = false
) {
    fun dataAsHex(): String {
        return if (data.isNotEmpty()) {
            data.joinToString(" ") { "%02X".format(it) }
        } else {
            error ?: "Sin datos"
        }
    }
}

data class KeyPair(
    val keyA: ByteArray?,
    val keyB: ByteArray?
)

data class NonceData(
    val nonce: ByteArray,
        val timestamp: Long = System.currentTimeMillis(),
    val key: ByteArray,
    val sector: Int
)

enum class OperationMode {
    READ, WRITE, CRACK
}

enum class AttackMethod {
    DICTIONARY, NONCE, HARDNESTED, MKF32, COMBINED
}