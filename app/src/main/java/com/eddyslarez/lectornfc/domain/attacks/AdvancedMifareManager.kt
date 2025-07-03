package com.eddyslarez.lectornfc.domain.attacks

import android.nfc.tech.MifareClassic
import com.eddyslarez.lectornfc.data.models.*
import com.eddyslarez.lectornfc.utils.NFCHelper
import com.eddyslarez.lectornfc.utils.CryptoUtils
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlin.random.Random

class AdvancedMifareManager(
    private val keyDictionaries: KeyDictionaries,
    private val nonceAnalyzer: NonceAnalyzer,
    private val hardnestedAttacker: HardnestedAttacker,
    private val mkfKey32: MKFKey32,
    private val dictionaryAttack: DictionaryAttack,
    private val bruteForceAttack: BruteForceAttack
) {

    private val _cardData = MutableStateFlow<List<BlockData>>(emptyList())
    val cardData: StateFlow<List<BlockData>> = _cardData.asStateFlow()

    private val _isReading = MutableStateFlow(false)
    val isReading: StateFlow<Boolean> = _isReading.asStateFlow()

    private val _isWriting = MutableStateFlow(false)
    val isWriting: StateFlow<Boolean> = _isWriting.asStateFlow()

    private val _status = MutableStateFlow("Acerca una tarjeta Mifare Classic")
    val status: StateFlow<String> = _status.asStateFlow()

    private val _mode = MutableStateFlow(OperationMode.READ)
    val mode: StateFlow<OperationMode> = _mode.asStateFlow()

    private val _progress = MutableStateFlow("")
    val progress: StateFlow<String> = _progress.asStateFlow()

    private val _currentSector = MutableStateFlow(0)
    val currentSector: StateFlow<Int> = _currentSector.asStateFlow()

    private val _totalSectors = MutableStateFlow(0)
    val totalSectors: StateFlow<Int> = _totalSectors.asStateFlow()

    private val _attackMethod = MutableStateFlow(AttackMethod.DICTIONARY)
    val attackMethod: StateFlow<AttackMethod> = _attackMethod.asStateFlow()

    private val _foundKeys = MutableStateFlow<Map<Int, KeyPair>>(emptyMap())
    val foundKeys: StateFlow<Map<Int, KeyPair>> = _foundKeys.asStateFlow()

    private val _crackedSectors = MutableStateFlow<Set<Int>>(emptySet())
    val crackedSectors: StateFlow<Set<Int>> = _crackedSectors.asStateFlow()

    private var storedData: List<BlockData> = emptyList()
    private var uid: ByteArray = byteArrayOf()
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var currentJob: Job? = null

    private val cryptoUtils = CryptoUtils()
    private val nfcHelper = NFCHelper()

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
        // Cancelar operación anterior si existe
        currentJob?.cancel()

        currentJob = when (_mode.value) {
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

    private suspend fun readCardAsync(mifare: MifareClassic) {
        withContext(Dispatchers.IO) {
            _isReading.value = true
            _status.value = "Conectando con la tarjeta..."
            _progress.value = "Iniciando lectura..."

            var isConnected = false
            try {
                // Conectar con reintentos
                repeat(3) { attempt ->
                    try {
                        if (!mifare.isConnected) {
                            mifare.connect()
                        }
                        isConnected = true
                        return@repeat
                    } catch (e: Exception) {
                        if (attempt == 2) throw e
                        delay(500)
                    }
                }

                if (!isConnected) {
                    throw Exception("No se pudo conectar con la tarjeta")
                }

                uid = mifare.tag.id
                _totalSectors.value = mifare.sectorCount
                _status.value = "Leyendo ${mifare.sectorCount} sectores..."

                val blocks = mutableListOf<BlockData>()
                val allKeys = keyDictionaries.getAllKeys()

                for (sector in 0 until mifare.sectorCount) {
                    // Verificar si la operación fue cancelada
                    if (!currentJob?.isActive!!) break

                    _currentSector.value = sector
                    _progress.value = "Sector $sector/${mifare.sectorCount - 1}"
                    _status.value = "Procesando sector $sector de ${mifare.sectorCount - 1}"

                    // Reconectar si es necesario
                    if (!mifare.isConnected) {
                        try {
                            mifare.connect()
                        } catch (e: Exception) {
                            _status.value = "Error de conexión en sector $sector"
                            continue
                        }
                    }

                    var authenticated = false
                    var usedKey: ByteArray? = null
                    var keyType = "A"

                    // Probar claves con manejo de errores mejorado
                    for ((index, key) in allKeys.withIndex()) {
                        try {
                            if (mifare.authenticateSectorWithKeyA(sector, key)) {
                                authenticated = true
                                usedKey = key
                                keyType = "A"
                                _progress.value = "Sector $sector: Autenticado con clave A"
                                break
                            }
                        } catch (e: Exception) {
                            // Intentar reconectar si hay error
                            try {
                                if (!mifare.isConnected) {
                                    mifare.connect()
                                }
                            } catch (reconnectError: Exception) {
                                continue
                            }
                        }

                        if (index % 50 == 0) {
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
                                    _progress.value = "Sector $sector: Autenticado con clave B"
                                    break
                                }
                            } catch (e: Exception) {
                                try {
                                    if (!mifare.isConnected) {
                                        mifare.connect()
                                    }
                                } catch (reconnectError: Exception) {
                                    continue
                                }
                            }

                            if (index % 50 == 0) {
                                delay(10)
                            }
                        }
                    }

                    // Leer bloques del sector
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
                    } else {
                        // Sector no autenticado
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

                    _cardData.value = blocks.toList()
                    delay(50)
                }

                storedData = blocks
                _status.value = "Lectura completada: ${blocks.size} bloques"
                _progress.value = "¡Completado!"

            } catch (e: Exception) {
                _status.value = "Error al leer tarjeta: ${e.message}"
                _progress.value = "Error en la lectura"
            } finally {
                try {
                    if (mifare.isConnected) {
                        mifare.close()
                    }
                } catch (e: Exception) {
                    // Ignorar errores al cerrar
                }
                _isReading.value = false
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

            var isConnected = false
            try {
                // Conectar con reintentos
                repeat(3) { attempt ->
                    try {
                        if (!mifare.isConnected) {
                            mifare.connect()
                        }
                        isConnected = true
                        return@repeat
                    } catch (e: Exception) {
                        if (attempt == 2) throw e
                        delay(500)
                    }
                }

                if (!isConnected) {
                    throw Exception("No se pudo conectar con la tarjeta para escritura")
                }

                var writtenBlocks = 0
                val writableBlocks = storedData.filter { !it.isTrailer && it.data.isNotEmpty() }

                _status.value = "Escribiendo ${writableBlocks.size} bloques..."

                for ((index, blockData) in writableBlocks.withIndex()) {
                    // Verificar si la operación fue cancelada
                    if (!currentJob?.isActive!!) break

                    _progress.value = "Escribiendo bloque ${index + 1}/${writableBlocks.size}"

                    // Reconectar si es necesario
                    if (!mifare.isConnected) {
                        try {
                            mifare.connect()
                        } catch (e: Exception) {
                            _status.value = "Error de conexión en bloque ${index + 1}"
                            continue
                        }
                    }

                    try {
                        val key = blockData.keyUsed ?: keyDictionaries.getBasicKeys()[0]
                        var authenticated = false

                        // Intentar autenticar con la clave original
                        try {
                            authenticated = if (blockData.keyType == "B") {
                                mifare.authenticateSectorWithKeyB(blockData.sector, key)
                            } else {
                                mifare.authenticateSectorWithKeyA(blockData.sector, key)
                            }
                        } catch (e: Exception) {
                            // Intentar con el tipo de clave opuesto
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
                                // Escribir el bloque
                                mifare.writeBlock(blockData.block, blockData.data)

                                // Verificar la escritura leyendo el bloque
                                delay(100) // Pequeña pausa para asegurar la escritura
                                val verification = mifare.readBlock(blockData.block)

                                if (verification.contentEquals(blockData.data)) {
                                    writtenBlocks++
                                    _progress.value = "Bloque ${blockData.block} escrito y verificado"
                                } else {
                                    _progress.value = "Bloque ${blockData.block} escrito pero verificación falló"
                                }
                            } catch (e: Exception) {
                                _progress.value = "Error escribiendo bloque ${blockData.block}: ${e.message}"
                                continue
                            }
                        }
                    } catch (e: Exception) {
                        continue
                    }

                    delay(100) // Pausa entre escrituras para estabilidad
                }

                _status.value = "Escritura completada: $writtenBlocks de ${writableBlocks.size} bloques escritos"
                _progress.value = "¡Escritura completada!"

            } catch (e: Exception) {
                _status.value = "Error al escribir tarjeta: ${e.message}"
                _progress.value = "Error en la escritura"
            } finally {
                try {
                    if (mifare.isConnected) {
                        mifare.close()
                    }
                } catch (e: Exception) {
                    // Ignorar errores al cerrar
                }
                _isWriting.value = false
            }
        }
    }

    private suspend fun crackCardAsync(mifare: MifareClassic) {
        withContext(Dispatchers.IO) {
            _isReading.value = true
            _status.value = "Iniciando ataque avanzado..."
            _progress.value = "Analizando tarjeta..."

            var isConnected = false
            try {
                // Conectar con reintentos y manejo robusto
                repeat(5) { attempt ->
                    try {
                        if (!mifare.isConnected) {
                            mifare.connect()
                        }
                        isConnected = true
                        return@repeat
                    } catch (e: Exception) {
                        if (attempt == 4) throw e
                        delay(1000) // Pausa más larga entre reintentos
                    }
                }

                if (!isConnected) {
                    throw Exception("No se pudo establecer conexión estable con la tarjeta")
                }

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
                    AttackMethod.BRUTE_FORCE -> executeBruteForceAttack(mifare)
                }

                // Leer datos con las claves encontradas
                readWithFoundKeys(mifare)

            } catch (e: Exception) {
                _status.value = "Error en ataque: ${e.message}"
                _progress.value = "Error"
            } finally {
                try {
                    if (mifare.isConnected) {
                        mifare.close()
                    }
                } catch (e: Exception) {
                    // Ignorar errores al cerrar
                }
                _isReading.value = false
            }
        }
    }

    private suspend fun executeDictionaryAttack(mifare: MifareClassic) {
        val foundKeys = mutableMapOf<Int, KeyPair>()

        for (sector in 0 until mifare.sectorCount) {
            // Verificar si la operación fue cancelada
            if (!currentJob?.isActive!!) break

            _currentSector.value = sector

            // Mantener conexión estable
            if (!mifare.isConnected) {
                try {
                    mifare.connect()
                } catch (e: Exception) {
                    _progress.value = "Sector $sector: Error de conexión"
                    continue
                }
            }

            val result = dictionaryAttack.attackSector(mifare, sector) { progress ->
                _progress.value = "Sector $sector: Clave ${progress.currentKeyIndex}/${progress.totalKeys} (${(progress.currentKeyIndex * 100 / progress.totalKeys)}%)"
            }

            if (result.success && result.keyPair != null) {
                foundKeys[sector] = result.keyPair
                _foundKeys.value = foundKeys.toMap()
                _crackedSectors.value = foundKeys.keys.toSet()
                _progress.value = "Sector $sector: ¡CRACKEADO! (${result.keySource})"
                delay(200)
            } else {
                _progress.value = "Sector $sector: No se encontró clave"
            }

            delay(100) // Pausa entre sectores para estabilidad
        }
    }

    private suspend fun executeNonceAttack(mifare: MifareClassic) {
        val foundKeys = mutableMapOf<Int, KeyPair>()

        for (sector in 0 until mifare.sectorCount) {
            if (!currentJob?.isActive!!) break

            _currentSector.value = sector
            _progress.value = "Sector $sector: Analizando nonces..."

            if (!mifare.isConnected) {
                try {
                    mifare.connect()
                } catch (e: Exception) {
                    continue
                }
            }

            try {
                val nonces = collectNonces(mifare, sector)
                val analysisResult = nonceAnalyzer.analyzeNonces(nonces, uid)

                if (analysisResult?.key != null) {
                    val keyPair = testKeyOnSector(mifare, sector, analysisResult.key)
                    if (keyPair != null) {
                        foundKeys[sector] = keyPair
                        _foundKeys.value = foundKeys.toMap()
                        _crackedSectors.value = foundKeys.keys.toSet()
                        _progress.value = "Sector $sector: ¡CRACKEADO con ${analysisResult.method}!"
                    }
                }
            } catch (e: Exception) {
                _progress.value = "Sector $sector: Error en análisis de nonce"
            }

            delay(100)
        }
    }

    private suspend fun executeHardnestedAttack(mifare: MifareClassic) {
        val knownKeys = _foundKeys.value.toMutableMap()

        if (knownKeys.isEmpty()) {
            val quickResult = dictionaryAttack.quickScan(mifare, 50)
            quickResult.forEach { (sector, result) ->
                if (result.success && result.keyPair != null) {
                    knownKeys[sector] = result.keyPair
                }
            }
        }

        if (knownKeys.isEmpty()) {
            _progress.value = "No se encontró clave conocida para hardnested"
            return
        }

        val knownSector = knownKeys.keys.first()
        val knownKeyPair = knownKeys[knownSector]!!
        val knownKey = knownKeyPair.keyA ?: knownKeyPair.keyB!!

        for (sector in 0 until mifare.sectorCount) {
            if (sector == knownSector || !currentJob?.isActive!!) continue

            _currentSector.value = sector
            _progress.value = "Sector $sector: Ejecutando hardnested..."

            if (!mifare.isConnected) {
                try {
                    mifare.connect()
                } catch (e: Exception) {
                    continue
                }
            }

            try {
                val foundKey = hardnestedAttacker.attack(mifare, knownSector, knownKey, sector)
                if (foundKey != null) {
                    val keyPair = testKeyOnSector(mifare, sector, foundKey)
                    if (keyPair != null) {
                        knownKeys[sector] = keyPair
                        _foundKeys.value = knownKeys.toMap()
                        _crackedSectors.value = knownKeys.keys.toSet()
                        _progress.value = "Sector $sector: ¡CRACKEADO con hardnested!"
                    }
                }
            } catch (e: Exception) {
                _progress.value = "Sector $sector: Error en hardnested"
            }

            delay(500)
        }
    }

    private suspend fun executeMKF32Attack(mifare: MifareClassic) {
        val foundKeys = mutableMapOf<Int, KeyPair>()

        for (sector in 0 until mifare.sectorCount) {
            if (!currentJob?.isActive!!) break

            _currentSector.value = sector
            _progress.value = "Sector $sector: Ejecutando MKF32..."

            if (!mifare.isConnected) {
                try {
                    mifare.connect()
                } catch (e: Exception) {
                    continue
                }
            }

            try {
                val algorithms = arrayOf(
                    MKFKey32.KeyAlgorithm.ENHANCED,
                    MKFKey32.KeyAlgorithm.RUSSIAN_DOMOPHONE,
                    MKFKey32.KeyAlgorithm.STATISTICAL,
                    MKFKey32.KeyAlgorithm.ADAPTIVE
                )

                var cracked = false
                for (algorithm in algorithms) {
                    if (cracked) break

                    try {
                        val keyResult = mkfKey32.generateKey(uid, sector, algorithm)
                        val keyPair = testKeyOnSector(mifare, sector, keyResult.key)

                        if (keyPair != null) {
                            foundKeys[sector] = keyPair
                            _foundKeys.value = foundKeys.toMap()
                            _crackedSectors.value = foundKeys.keys.toSet()
                            _progress.value = "Sector $sector: ¡CRACKEADO con MKF32 ${algorithm.name}!"
                            cracked = true
                        }
                    } catch (e: Exception) {
                        continue
                    }
                }
            } catch (e: Exception) {
                _progress.value = "Sector $sector: Error en MKF32 - ${e.message}"
            }

            delay(100)
        }
    }

    private suspend fun executeCombinedAttack(mifare: MifareClassic) {
        _progress.value = "Ejecutando ataque combinado..."

        executeDictionaryAttack(mifare)
        executeMKF32Attack(mifare)

        if (_foundKeys.value.isNotEmpty()) {
            executeHardnestedAttack(mifare)
        }

        executeNonceAttack(mifare)
        executeBruteForceAttack(mifare)
    }

    private suspend fun executeBruteForceAttack(mifare: MifareClassic) {
        val foundKeys = _foundKeys.value.toMutableMap()
        val remainingSectors = (0 until mifare.sectorCount).filter { !foundKeys.containsKey(it) }

        if (remainingSectors.isEmpty()) {
            _progress.value = "Todos los sectores ya están crackeados"
            return
        }

        _progress.value = "Iniciando ataque de fuerza bruta en ${remainingSectors.size} sectores..."

        for (sector in remainingSectors) {
            if (!currentJob?.isActive!!) break

            _currentSector.value = sector
            _progress.value = "Sector $sector: Ejecutando fuerza bruta..."

            if (!mifare.isConnected) {
                try {
                    mifare.connect()
                } catch (e: Exception) {
                    continue
                }
            }

            try {
                val result = bruteForceAttack.attackSector(
                    mifare = mifare,
                    sector = sector,
                    strategy = BruteForceAttack.BruteForceStrategy.SMART_RANDOM,
                    maxTimeMs = 60000L
                ) { progress ->
                    _progress.value = "Sector $sector: Fuerza bruta - ${progress.currentAttempt} intentos"
                }

                if (result.success && result.keyPair != null) {
                    foundKeys[sector] = result.keyPair
                    _foundKeys.value = foundKeys.toMap()
                    _crackedSectors.value = foundKeys.keys.toSet()
                    _progress.value = "Sector $sector: ¡CRACKEADO con fuerza bruta!"
                    delay(200)
                }
            } catch (e: Exception) {
                _progress.value = "Sector $sector: Error en fuerza bruta"
            }

            delay(100)
        }
    }

    private fun collectNonces(mifare: MifareClassic, sector: Int): List<NonceData> {
        val nonces = mutableListOf<NonceData>()
        val keys = keyDictionaries.getBasicKeys()

        for (key in keys.take(10)) {
            try {
                val nonce = ByteArray(4)
                Random.nextBytes(nonce)
                nonces.add(NonceData(nonce, System.currentTimeMillis(), key, sector))
            } catch (e: Exception) {
                continue
            }
        }

        return nonces
    }

    private suspend fun testKeyOnSector(mifare: MifareClassic, sector: Int, key: ByteArray): KeyPair? {
        return withContext(Dispatchers.IO) {
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
                    KeyPair(keyA, keyB)
                } else {
                    null
                }
            } catch (e: Exception) {
                null
            }
        }
    }

    private suspend fun readWithFoundKeys(mifare: MifareClassic) {
        val foundKeys = _foundKeys.value
        val blocks = mutableListOf<BlockData>()

        for (sector in 0 until mifare.sectorCount) {
            if (!currentJob?.isActive!!) break

            _currentSector.value = sector
            _progress.value = "Leyendo sector $sector..."

            if (!mifare.isConnected) {
                try {
                    mifare.connect()
                } catch (e: Exception) {
                    continue
                }
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

        _cardData.value = blocks
        storedData = blocks
        _status.value = "Ataque completado: ${_crackedSectors.value.size}/${mifare.sectorCount} sectores crackeados"
        _progress.value = "¡Completado!"
    }

    fun cancelOperation() {
        currentJob?.cancel()
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

/////////2/////////
//class AdvancedMifareManager(
//    private val keyDictionaries: KeyDictionaries,
//    private val nonceAnalyzer: NonceAnalyzer,
//    private val hardnestedAttacker: HardnestedAttacker,
//    private val mkfKey32: MKFKey32,
//    private val dictionaryAttack: DictionaryAttack,
//    private val bruteForceAttack: BruteForceAttack
//) {
//
//    // Cambiar de mutableStateOf a MutableStateFlow para compatibilidad con combine
//    private val _cardData = MutableStateFlow<List<BlockData>>(emptyList())
//    val cardData: StateFlow<List<BlockData>> = _cardData.asStateFlow()
//
//    private val _isReading = MutableStateFlow(false)
//    val isReading: StateFlow<Boolean> = _isReading.asStateFlow()
//
//    private val _isWriting = MutableStateFlow(false)
//    val isWriting: StateFlow<Boolean> = _isWriting.asStateFlow()
//
//    private val _status = MutableStateFlow("Acerca una tarjeta Mifare Classic")
//    val status: StateFlow<String> = _status.asStateFlow()
//
//    private val _mode = MutableStateFlow(OperationMode.READ)
//    val mode: StateFlow<OperationMode> = _mode.asStateFlow()
//
//    private val _progress = MutableStateFlow("")
//    val progress: StateFlow<String> = _progress.asStateFlow()
//
//    private val _currentSector = MutableStateFlow(0)
//    val currentSector: StateFlow<Int> = _currentSector.asStateFlow()
//
//    private val _totalSectors = MutableStateFlow(0)
//    val totalSectors: StateFlow<Int> = _totalSectors.asStateFlow()
//
//    private val _attackMethod = MutableStateFlow(AttackMethod.DICTIONARY)
//    val attackMethod: StateFlow<AttackMethod> = _attackMethod.asStateFlow()
//
//    private val _foundKeys = MutableStateFlow<Map<Int, KeyPair>>(emptyMap())
//    val foundKeys: StateFlow<Map<Int, KeyPair>> = _foundKeys.asStateFlow()
//
//    private val _crackedSectors = MutableStateFlow<Set<Int>>(emptySet())
//    val crackedSectors: StateFlow<Set<Int>> = _crackedSectors.asStateFlow()
//
//    private var storedData: List<BlockData> = emptyList()
//    private var uid: ByteArray = byteArrayOf()
//    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
//    private var currentJob: Job? = null
//
//    private val cryptoUtils = CryptoUtils()
//    private val nfcHelper = NFCHelper()
//
//    fun setMode(newMode: OperationMode) {
//        _mode.value = newMode
//        _status.value = when (newMode) {
//            OperationMode.READ -> "Modo lectura: Acerca una tarjeta para leer"
//            OperationMode.WRITE -> "Modo escritura: Acerca una tarjeta para escribir"
//            OperationMode.CRACK -> "Modo crack: Acerca una tarjeta para descifrar"
//        }
//    }
//
//    fun setAttackMethod(method: AttackMethod) {
//        _attackMethod.value = method
//    }
//
//    fun processNewTag(mifare: MifareClassic) {
//        // Cancelar operación anterior si existe
//        currentJob?.cancel()
//
//        currentJob = when (_mode.value) {
//            OperationMode.READ -> {
//                scope.launch {
//                    readCardAsync(mifare)
//                }
//            }
//            OperationMode.WRITE -> {
//                scope.launch {
//                    writeCardAsync(mifare)
//                }
//            }
//            OperationMode.CRACK -> {
//                scope.launch {
//                    crackCardAsync(mifare)
//                }
//            }
//        }
//    }
//
//    private suspend fun crackCardAsync(mifare: MifareClassic) {
//        withContext(Dispatchers.IO) {
//            _isReading.value = true
//            _status.value = "Iniciando ataque avanzado..."
//            _progress.value = "Analizando tarjeta..."
//
//            try {
//                mifare.connect()
//                delay(100)
//
//                // Obtener información de la tarjeta
//                uid = mifare.tag.id
//                _totalSectors.value = mifare.sectorCount
//                _foundKeys.value = emptyMap()
//                _crackedSectors.value = emptySet()
//
//                _status.value = "Ejecutando ataque ${_attackMethod.value.name.lowercase()}..."
//
//                when (_attackMethod.value) {
//                    AttackMethod.DICTIONARY -> executeDictionaryAttack(mifare)
//                    AttackMethod.NONCE -> executeNonceAttack(mifare)
//                    AttackMethod.HARDNESTED -> executeHardnestedAttack(mifare)
//                    AttackMethod.MKF32 -> executeMKF32Attack(mifare)
//                    AttackMethod.COMBINED -> executeCombinedAttack(mifare)
//                }
//
//                // Leer datos con las claves encontradas
//                readWithFoundKeys(mifare)
//
//            } catch (e: Exception) {
//                _status.value = "Error en ataque: ${e.message}"
//                _progress.value = "Error"
//            } finally {
//                try {
//                    mifare.close()
//                } catch (e: Exception) {
//                    // Ignorar errores al cerrar
//                }
//                _isReading.value = false
//            }
//        }
//    }
//
//    private suspend fun executeDictionaryAttack(mifare: MifareClassic) {
//        val foundKeys = mutableMapOf<Int, KeyPair>()
//
//        for (sector in 0 until mifare.sectorCount) {
//            _currentSector.value = sector
//
//            val result = dictionaryAttack.attackSector(mifare, sector) { progress ->
//                _progress.value = "Sector $sector: Clave ${progress.currentKeyIndex}/${progress.totalKeys} (${(progress.currentKeyIndex * 100 / progress.totalKeys)}%)"
//            }
//
//            if (result.success && result.keyPair != null) {
//                foundKeys[sector] = result.keyPair
//                _foundKeys.value = foundKeys.toMap()
//                _crackedSectors.value = foundKeys.keys.toSet()
//                _progress.value = "Sector $sector: ¡CRACKEADO! (${result.keySource})"
//                delay(200) // Pausa para mostrar el éxito
//            } else {
//                _progress.value = "Sector $sector: No se encontró clave"
//            }
//        }
//    }
//
//    private suspend fun executeNonceAttack(mifare: MifareClassic) {
//        val foundKeys = mutableMapOf<Int, KeyPair>()
//
//        for (sector in 0 until mifare.sectorCount) {
//            _currentSector.value = sector
//            _progress.value = "Sector $sector: Analizando nonces..."
//
//            try {
//                val nonces = collectNonces(mifare, sector)
//                val analysisResult = nonceAnalyzer.analyzeNonces(nonces, uid)
//
//                if (analysisResult?.key != null) {
//                    // Probar la clave encontrada
//                    val keyPair = testKeyOnSector(mifare, sector, analysisResult.key)
//                    if (keyPair != null) {
//                        foundKeys[sector] = keyPair
//                        _foundKeys.value = foundKeys.toMap()
//                        _crackedSectors.value = foundKeys.keys.toSet()
//                        _progress.value = "Sector $sector: ¡CRACKEADO con ${analysisResult.method}!"
//                    }
//                }
//            } catch (e: Exception) {
//                _progress.value = "Sector $sector: Error en análisis de nonce"
//            }
//
//            delay(100)
//        }
//    }
//
//    private suspend fun executeHardnestedAttack(mifare: MifareClassic) {
//        // Primero necesitamos al menos una clave conocida
//        val knownKeys = _foundKeys.value.toMutableMap()
//
//        if (knownKeys.isEmpty()) {
//            // Intentar encontrar al menos una clave con diccionario rápido
//            val quickResult = dictionaryAttack.quickScan(mifare, 50)
//            quickResult.forEach { (sector, result) ->
//                if (result.success && result.keyPair != null) {
//                    knownKeys[sector] = result.keyPair
//                }
//            }
//        }
//
//        if (knownKeys.isEmpty()) {
//            _progress.value = "No se encontró clave conocida para hardnested"
//            return
//        }
//
//        val knownSector = knownKeys.keys.first()
//        val knownKeyPair = knownKeys[knownSector]!!
//        val knownKey = knownKeyPair.keyA ?: knownKeyPair.keyB!!
//
//        for (sector in 0 until mifare.sectorCount) {
//            if (sector == knownSector) continue
//
//            _currentSector.value = sector
//            _progress.value = "Sector $sector: Ejecutando hardnested..."
//
//            try {
//                val foundKey = hardnestedAttacker.attack(mifare, knownSector, knownKey, sector)
//                if (foundKey != null) {
//                    val keyPair = testKeyOnSector(mifare, sector, foundKey)
//                    if (keyPair != null) {
//                        knownKeys[sector] = keyPair
//                        _foundKeys.value = knownKeys.toMap()
//                        _crackedSectors.value = knownKeys.keys.toSet()
//                        _progress.value = "Sector $sector: ¡CRACKEADO con hardnested!"
//                    }
//                }
//            } catch (e: Exception) {
//                _progress.value = "Sector $sector: Error en hardnested"
//            }
//
//            delay(500)
//        }
//    }
//
//    private suspend fun executeMKF32Attack(mifare: MifareClassic) {
//        val foundKeys = mutableMapOf<Int, KeyPair>()
//
//        for (sector in 0 until mifare.sectorCount) {
//            _currentSector.value = sector
//            _progress.value = "Sector $sector: Ejecutando MKF32..."
//
//            try {
//                // Probar diferentes algoritmos MKF32
//                val algorithms = arrayOf(
//                    MKFKey32.KeyAlgorithm.ENHANCED,
//                    MKFKey32.KeyAlgorithm.RUSSIAN_DOMOPHONE,
//                    MKFKey32.KeyAlgorithm.STATISTICAL,
//                    MKFKey32.KeyAlgorithm.ADAPTIVE
//                )
//
//                var cracked = false
//                for (algorithm in algorithms) {
//                    if (cracked) break
//
//                    try {
//                        val keyResult = mkfKey32.generateKey(uid, sector, algorithm)
//                        val keyPair = testKeyOnSector(mifare, sector, keyResult.key)
//
//                        if (keyPair != null) {
//                            foundKeys[sector] = keyPair
//                            _foundKeys.value = foundKeys.toMap()
//                            _crackedSectors.value = foundKeys.keys.toSet()
//                            _progress.value = "Sector $sector: ¡CRACKEADO con MKF32 ${algorithm.name}!"
//                            cracked = true
//                        }
//                    } catch (e: Exception) {
//                        continue
//                    }
//                }
//            } catch (e: Exception) {
//                _progress.value = "Sector $sector: Error en MKF32 - ${e.message}"
//            }
//
//            delay(100)
//        }
//    }
//
//    private suspend fun executeCombinedAttack(mifare: MifareClassic) {
//        _progress.value = "Ejecutando ataque combinado..."
//
//        // 1. Ataque de diccionario rápido
//        executeDictionaryAttack(mifare)
//
//        // 2. MKF32 para sectores restantes
//        executeMKF32Attack(mifare)
//
//        // 3. Hardnested si tenemos claves conocidas
//        if (_foundKeys.value.isNotEmpty()) {
//            executeHardnestedAttack(mifare)
//        }
//
//        // 4. Nonce attack para sectores restantes
//        executeNonceAttack(mifare)
//    }
//
//    private fun collectNonces(mifare: MifareClassic, sector: Int): List<NonceData> {
//        val nonces = mutableListOf<NonceData>()
//        val keys = keyDictionaries.getBasicKeys()
//
//        for (key in keys.take(10)) { // Limitar para no ser demasiado lento
//            try {
//                // Simular recolección de nonces
//                val nonce = ByteArray(4)
//                Random.nextBytes(nonce)
//                nonces.add(NonceData(nonce, System.currentTimeMillis(), key, sector))
//            } catch (e: Exception) {
//                continue
//            }
//        }
//
//        return nonces
//    }
//
//    private suspend fun testKeyOnSector(mifare: MifareClassic, sector: Int, key: ByteArray): KeyPair? {
//        return withContext(Dispatchers.IO) {
//            try {
//                var keyA: ByteArray? = null
//                var keyB: ByteArray? = null
//
//                if (mifare.authenticateSectorWithKeyA(sector, key)) {
//                    keyA = key
//                }
//
//                if (mifare.authenticateSectorWithKeyB(sector, key)) {
//                    keyB = key
//                }
//
//                if (keyA != null || keyB != null) {
//                    KeyPair(keyA, keyB)
//                } else {
//                    null
//                }
//            } catch (e: Exception) {
//                null
//            }
//        }
//    }
//
//    private suspend fun readWithFoundKeys(mifare: MifareClassic) {
//        val foundKeys = _foundKeys.value
//        val blocks = mutableListOf<BlockData>()
//
//        for (sector in 0 until mifare.sectorCount) {
//            _currentSector.value = sector
//            _progress.value = "Leyendo sector $sector..."
//
//            val keyPair = foundKeys[sector]
//            if (keyPair != null) {
//                try {
//                    val key = keyPair.keyA ?: keyPair.keyB!!
//                    val keyType = if (keyPair.keyA != null) "A" else "B"
//
//                    val authenticated = if (keyType == "A") {
//                        mifare.authenticateSectorWithKeyA(sector, key)
//                    } else {
//                        mifare.authenticateSectorWithKeyB(sector, key)
//                    }
//
//                    if (authenticated) {
//                        val blocksInSector = mifare.getBlockCountInSector(sector)
//                        val firstBlock = mifare.sectorToBlock(sector)
//
//                        for (i in 0 until blocksInSector) {
//                            val blockIndex = firstBlock + i
//                            try {
//                                val data = mifare.readBlock(blockIndex)
//                                blocks.add(
//                                    BlockData(
//                                        sector = sector,
//                                        block = blockIndex,
//                                        data = data,
//                                        isTrailer = (i == blocksInSector - 1),
//                                        keyUsed = key,
//                                        keyType = keyType,
//                                        cracked = true
//                                    )
//                                )
//                            } catch (e: Exception) {
//                                blocks.add(
//                                    BlockData(
//                                        sector = sector,
//                                        block = blockIndex,
//                                        data = byteArrayOf(),
//                                        isTrailer = (i == blocksInSector - 1),
//                                        error = "Error: ${e.message}",
//                                        keyType = keyType,
//                                        cracked = false
//                                    )
//                                )
//                            }
//                        }
//                    }
//                } catch (e: Exception) {
//                    // Sector no accesible
//                }
//            } else {
//                // Sector no crackeado
//                val blocksInSector = mifare.getBlockCountInSector(sector)
//                val firstBlock = mifare.sectorToBlock(sector)
//
//                for (i in 0 until blocksInSector) {
//                    blocks.add(
//                        BlockData(
//                            sector = sector,
//                            block = firstBlock + i,
//                            data = byteArrayOf(),
//                            isTrailer = (i == blocksInSector - 1),
//                            error = "Sector no crackeado",
//                            cracked = false
//                        )
//                    )
//                }
//            }
//        }
//
//        _cardData.value = blocks
//        storedData = blocks
//        _status.value = "Ataque completado: ${_crackedSectors.value.size}/${mifare.sectorCount} sectores crackeados"
//        _progress.value = "¡Completado!"
//    }
//
//    // Método de lectura normal mejorado
//    private suspend fun readCardAsync(mifare: MifareClassic) {
//        withContext(Dispatchers.IO) {
//            _isReading.value = true
//            _status.value = "Conectando con la tarjeta..."
//            _progress.value = "Iniciando lectura..."
//
//            try {
//                mifare.connect()
//                delay(100)
//
//                _totalSectors.value = mifare.sectorCount
//                _status.value = "Leyendo ${mifare.sectorCount} sectores..."
//
//                val blocks = mutableListOf<BlockData>()
//                val allKeys = keyDictionaries.getAllKeys()
//
//                for (sector in 0 until mifare.sectorCount) {
//                    _currentSector.value = sector
//                    _progress.value = "Sector $sector/${mifare.sectorCount - 1}"
//
//                    _status.value = "Procesando sector $sector de ${mifare.sectorCount - 1}"
//
//                    var authenticated = false
//                    var usedKey: ByteArray? = null
//                    var keyType = "A"
//
//                    // Probar todas las claves
//                    for ((index, key) in allKeys.withIndex()) {
//                        try {
//                            if (mifare.authenticateSectorWithKeyA(sector, key)) {
//                                authenticated = true
//                                usedKey = key
//                                keyType = "A"
//                                _progress.value = "Sector $sector: Autenticado con clave A (${index + 1}/${allKeys.size})"
//                                break
//                            }
//                        } catch (e: Exception) {
//                            continue
//                        }
//
//                        if (index % 100 == 0) {
//                            delay(10)
//                        }
//                    }
//
//                    if (!authenticated) {
//                        for ((index, key) in allKeys.withIndex()) {
//                            try {
//                                if (mifare.authenticateSectorWithKeyB(sector, key)) {
//                                    authenticated = true
//                                    usedKey = key
//                                    keyType = "B"
//                                    _progress.value = "Sector $sector: Autenticado con clave B (${index + 1}/${allKeys.size})"
//                                    break
//                                }
//                            } catch (e: Exception) {
//                                continue
//                            }
//
//                            if (index % 100 == 0) {
//                                delay(10)
//                            }
//                        }
//                    }
//
//                    if (authenticated) {
//                        val blocksInSector = mifare.getBlockCountInSector(sector)
//                        val firstBlock = mifare.sectorToBlock(sector)
//
//                        for (i in 0 until blocksInSector) {
//                            val blockIndex = firstBlock + i
//                            try {
//                                val data = mifare.readBlock(blockIndex)
//                                blocks.add(
//                                    BlockData(
//                                        sector = sector,
//                                        block = blockIndex,
//                                        data = data,
//                                        isTrailer = (i == blocksInSector - 1),
//                                        keyUsed = usedKey,
//                                        keyType = keyType,
//                                        cracked = true
//                                    )
//                                )
//                            } catch (e: Exception) {
//                                blocks.add(
//                                    BlockData(
//                                        sector = sector,
//                                        block = blockIndex,
//                                        data = byteArrayOf(),
//                                        isTrailer = (i == blocksInSector - 1),
//                                        error = "Error: ${e.message}",
//                                        keyType = keyType,
//                                        cracked = false
//                                    )
//                                )
//                            }
//                        }
//
//                        _cardData.value = blocks.toList()
//
//                    } else {
//                        _progress.value = "Sector $sector: No se pudo autenticar"
//
//                        val blocksInSector = mifare.getBlockCountInSector(sector)
//                        val firstBlock = mifare.sectorToBlock(sector)
//
//                        for (i in 0 until blocksInSector) {
//                            blocks.add(
//                                BlockData(
//                                    sector = sector,
//                                    block = firstBlock + i,
//                                    data = byteArrayOf(),
//                                    isTrailer = (i == blocksInSector - 1),
//                                    error = "Sector no autenticado",
//                                    cracked = false
//                                )
//                            )
//                        }
//                    }
//
//                    delay(50)
//                }
//
//                _cardData.value = blocks
//                storedData = blocks
//                _status.value = "Lectura completada: ${blocks.size} bloques"
//                _progress.value = "¡Completado!"
//
//            } catch (e: Exception) {
//                _status.value = "Error al leer tarjeta: ${e.message}"
//                _progress.value = "Error en la lectura"
//            } finally {
//                try {
//                    mifare.close()
//                } catch (e: Exception) {
//                    // Ignorar errores al cerrar
//                }
//                _isReading.value = false
//            }
//        }
//    }
//
//    private suspend fun writeCardAsync(mifare: MifareClassic) {
//        if (storedData.isEmpty()) {
//            _status.value = "No hay datos para escribir. Primero lee una tarjeta."
//            return
//        }
//
//        withContext(Dispatchers.IO) {
//            _isWriting.value = true
//            _status.value = "Conectando para escritura..."
//
//            try {
//                mifare.connect()
//                delay(100)
//
//                var writtenBlocks = 0
//                val writableBlocks = storedData.filter { !it.isTrailer && it.data.isNotEmpty() }
//
//                _status.value = "Escribiendo ${writableBlocks.size} bloques..."
//
//                for ((index, blockData) in writableBlocks.withIndex()) {
//                    _progress.value = "Escribiendo bloque ${index + 1}/${writableBlocks.size}"
//
//                    try {
//                        val key = blockData.keyUsed ?: keyDictionaries.getBasicKeys()[0]
//                        var authenticated = false
//
//                        try {
//                            authenticated = if (blockData.keyType == "B") {
//                                mifare.authenticateSectorWithKeyB(blockData.sector, key)
//                            } else {
//                                mifare.authenticateSectorWithKeyA(blockData.sector, key)
//                            }
//                        } catch (e: Exception) {
//                            try {
//                                authenticated = if (blockData.keyType == "B") {
//                                    mifare.authenticateSectorWithKeyA(blockData.sector, key)
//                                } else {
//                                    mifare.authenticateSectorWithKeyB(blockData.sector, key)
//                                }
//                            } catch (e2: Exception) {
//                                continue
//                            }
//                        }
//
//                        if (authenticated) {
//                            try {
//                                mifare.writeBlock(blockData.block, blockData.data)
//                                writtenBlocks++
//                            } catch (e: Exception) {
//                                continue
//                            }
//                        }
//                    } catch (e: Exception) {
//                        continue
//                    }
//
//                    delay(50)
//                }
//
//                _status.value = "Escritura completada: $writtenBlocks bloques escritos"
//                _progress.value = "¡Escritura completada!"
//
//            } catch (e: Exception) {
//                _status.value = "Error al escribir tarjeta: ${e.message}"
//                _progress.value = "Error en la escritura"
//            } finally {
//                try {
//                    mifare.close()
//                } catch (e: Exception) {
//                    // Ignorar errores al cerrar
//                }
//                _isWriting.value = false
//            }
//        }
//    }
//
//    fun cancelOperation() {
//        currentJob?.cancel()
//        scope.launch {
//            _isReading.value = false
//            _isWriting.value = false
//            _status.value = "Operación cancelada"
//            _progress.value = ""
//        }
//    }
//
//    fun clearData() {
//        _cardData.value = emptyList()
//        storedData = emptyList()
//        _foundKeys.value = emptyMap()
//        _crackedSectors.value = emptySet()
//        _status.value = "Datos borrados. Acerca una tarjeta Mifare Classic"
//        _progress.value = ""
//    }
//}

///////////1//////
//
//import android.nfc.tech.MifareClassic
//import androidx.compose.runtime.State
//import androidx.compose.runtime.mutableStateOf
//import com.eddyslarez.lectornfc.data.models.AttackMethod
//import com.eddyslarez.lectornfc.data.models.BlockData
//import com.eddyslarez.lectornfc.data.models.KeyPair
//import com.eddyslarez.lectornfc.data.models.NonceData
//import com.eddyslarez.lectornfc.data.models.OperationMode
//
//import kotlinx.coroutines.*
//import kotlin.random.Random
//
//object AdvancedMifareManager {
//    private var _cardData = mutableStateOf<List<BlockData>>(emptyList())
//    val cardData: State<List<BlockData>> = _cardData
//
//    private var _isReading = mutableStateOf(false)
//    val isReading: State<Boolean> = _isReading
//
//    private var _isWriting = mutableStateOf(false)
//    val isWriting: State<Boolean> = _isWriting
//
//    private var _status = mutableStateOf("Acerca una tarjeta Mifare Classic")
//    val status: State<String> = _status
//
//    private var _mode = mutableStateOf(OperationMode.READ)
//    val mode: State<OperationMode> = _mode
//
//    private var _progress = mutableStateOf("")
//    val progress: State<String> = _progress
//
//    private var _currentSector = mutableStateOf(0)
//    val currentSector: State<Int> = _currentSector
//
//    private var _totalSectors = mutableStateOf(0)
//    val totalSectors: State<Int> = _totalSectors
//
//    private var _attackMethod = mutableStateOf(AttackMethod.DICTIONARY)
//    val attackMethod: State<AttackMethod> = _attackMethod
//
//    private var _foundKeys = mutableStateOf<Map<Int, KeyPair>>(emptyMap())
//    val foundKeys: State<Map<Int, KeyPair>> = _foundKeys
//
//    private var _crackedSectors = mutableStateOf<Set<Int>>(emptySet())
//    val crackedSectors: State<Set<Int>> = _crackedSectors
//
//    private var storedData: List<BlockData> = emptyList()
//    private var uid: ByteArray = byteArrayOf()
//    private var atqa: ByteArray = byteArrayOf()
//    private var sak: Byte = 0
//
//    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
//
//    // Diccionarios de claves mejorados
//    private val keyDictionaries = KeyDictionaries()
//    private val nonceAnalyzer = NonceAnalyzer()
//    private val hardnestedAttacker = HardnestedAttacker()
//    private val mkfKey32 = MKFKey32()
//
//    fun setMode(newMode: OperationMode) {
//        _mode.value = newMode
//        _status.value = when (newMode) {
//            OperationMode.READ -> "Modo lectura: Acerca una tarjeta para leer"
//            OperationMode.WRITE -> "Modo escritura: Acerca una tarjeta para escribir"
//            OperationMode.CRACK -> "Modo crack: Acerca una tarjeta para descifrar"
//        }
//    }
//
//    fun setAttackMethod(method: AttackMethod) {
//        _attackMethod.value = method
//    }
//
//    fun processNewTag(mifare: MifareClassic) {
//        when (_mode.value) {
//            OperationMode.READ -> {
//                scope.launch {
//                    readCardAsync(mifare)
//                }
//            }
//            OperationMode.WRITE -> {
//                scope.launch {
//                    writeCardAsync(mifare)
//                }
//            }
//            OperationMode.CRACK -> {
//                scope.launch {
//                    crackCardAsync(mifare)
//                }
//            }
//        }
//    }
//
//    private suspend fun crackCardAsync(mifare: MifareClassic) {
//        withContext(Dispatchers.IO) {
//            _isReading.value = true
//            _status.value = "Iniciando ataque avanzado..."
//            _progress.value = "Analizando tarjeta..."
//
//            try {
//                mifare.connect()
//                delay(100)
//
//                // Obtener información de la tarjeta
//                uid = mifare.tag.id
//                _totalSectors.value = mifare.sectorCount
//                _foundKeys.value = emptyMap()
//                _crackedSectors.value = emptySet()
//
//                _status.value = "Ejecutando ataque ${_attackMethod.value.name.lowercase()}..."
//
//                when (_attackMethod.value) {
//                    AttackMethod.DICTIONARY -> executeDictionaryAttack(mifare)
//                    AttackMethod.NONCE -> executeNonceAttack(mifare)
//                    AttackMethod.HARDNESTED -> executeHardnestedAttack(mifare)
//                    AttackMethod.MKF32 -> executeMKF32Attack(mifare)
//                    AttackMethod.COMBINED -> executeCombinedAttack(mifare)
//                }
//
//                // Leer datos con las claves encontradas
//                readWithFoundKeys(mifare)
//
//            } catch (e: Exception) {
//                withContext(Dispatchers.Main) {
//                    _status.value = "Error en ataque: ${e.message}"
//                    _progress.value = "Error"
//                }
//            } finally {
//                try {
//                    mifare.close()
//                } catch (e: Exception) {
//                    // Ignorar errores al cerrar
//                }
//                withContext(Dispatchers.Main) {
//                    _isReading.value = false
//                }
//            }
//        }
//    }
//
//    private suspend fun executeDictionaryAttack(mifare: MifareClassic) {
//        val keys = keyDictionaries.getAllKeys()
//        val foundKeys = mutableMapOf<Int, KeyPair>()
//        var processedKeys = 0
//
//        for (sector in 0 until mifare.sectorCount) {
//            _currentSector.value = sector
//            var sectorCracked = false
//
//            for ((index, key) in keys.withIndex()) {
//                if (sectorCracked) break
//
//                processedKeys++
//                val progress = (processedKeys.toFloat() / (keys.size * mifare.sectorCount)) * 100
//
//                withContext(Dispatchers.Main) {
//                    _progress.value = "Sector $sector: Probando clave ${index + 1}/${keys.size} (${progress.toInt()}%)"
//                }
//
//                try {
//                    var keyA: ByteArray? = null
//                    var keyB: ByteArray? = null
//
//                    // Probar clave A
//                    if (mifare.authenticateSectorWithKeyA(sector, key)) {
//                        keyA = key
//                        sectorCracked = true
//                    }
//
//                    // Probar clave B
//                    if (mifare.authenticateSectorWithKeyB(sector, key)) {
//                        keyB = key
//                        sectorCracked = true
//                    }
//
//                    if (sectorCracked) {
//                        foundKeys[sector] = KeyPair(keyA, keyB)
//                        withContext(Dispatchers.Main) {
//                            _foundKeys.value = foundKeys.toMap()
//                            _crackedSectors.value = foundKeys.keys.toSet()
//                            _progress.value = "Sector $sector: ¡CRACKEADO! Clave encontrada"
//                        }
//                        delay(200) // Pausa para mostrar el éxito
//                    }
//                } catch (e: Exception) {
//                    continue
//                }
//
//                if (index % 50 == 0) {
//                    delay(10) // Pausa cada 50 intentos
//                }
//            }
//        }
//    }
//
//    private suspend fun executeNonceAttack(mifare: MifareClassic) {
//        val foundKeys = mutableMapOf<Int, KeyPair>()
//
//        for (sector in 0 until mifare.sectorCount) {
//            _currentSector.value = sector
//            withContext(Dispatchers.Main) {
//                _progress.value = "Sector $sector: Analizando nonces..."
//            }
//
//            try {
//                val nonces = collectNonces(mifare, sector)
//                val key = nonceAnalyzer.analyzeNonces(nonces, uid)
//                val analysisResult = nonceAnalyzer.analyzeNonces(nonces, uid)
//
//                if (analysisResult != null) {
//                    foundKeys[sector] = KeyPair(analysisResult.key, null)
//                    withContext(Dispatchers.Main) {
//                        _foundKeys.value = foundKeys.toMap()
//                        _crackedSectors.value = foundKeys.keys.toSet()
//                        _progress.value = "Sector $sector: ¡CRACKEADO con nonce!"
//                    }
//                }
//            } catch (e: Exception) {
//                withContext(Dispatchers.Main) {
//                    _progress.value = "Sector $sector: Error en análisis de nonce"
//                }
//            }
//
//            delay(100)
//        }
//    }
//
//    private suspend fun executeHardnestedAttack(mifare: MifareClassic) {
//        // Primero necesitamos al menos una clave conocida
//        val knownKeys = _foundKeys.value.toMutableMap()
//
//        if (knownKeys.isEmpty()) {
//            // Intentar encontrar al menos una clave con diccionario rápido
//            val basicKeys = keyDictionaries.getBasicKeys()
//            for (sector in 0 until mifare.sectorCount) {
//                for (key in basicKeys) {
//                    try {
//                        if (mifare.authenticateSectorWithKeyA(sector, key)) {
//                            knownKeys[sector] = KeyPair(key, null)
//                            break
//                        }
//                    } catch (e: Exception) {
//                        continue
//                    }
//                }
//                if (knownKeys.isNotEmpty()) break
//            }
//        }
//
//        if (knownKeys.isEmpty()) {
//            withContext(Dispatchers.Main) {
//                _progress.value = "No se encontró clave conocida para hardnested"
//            }
//            return
//        }
//
//        val knownSector = knownKeys.keys.first()
//        val knownKey = knownKeys[knownSector]!!.keyA!!
//
//        for (sector in 0 until mifare.sectorCount) {
//            if (sector == knownSector) continue
//
//            _currentSector.value = sector
//            withContext(Dispatchers.Main) {
//                _progress.value = "Sector $sector: Ejecutando hardnested..."
//            }
//
//            try {
//                val foundKey = hardnestedAttacker.attack(mifare, knownSector, knownKey, sector)
//                if (foundKey != null) {
//                    knownKeys[sector] = KeyPair(foundKey, null)
//                    withContext(Dispatchers.Main) {
//                        _foundKeys.value = knownKeys.toMap()
//                        _crackedSectors.value = knownKeys.keys.toSet()
//                        _progress.value = "Sector $sector: ¡CRACKEADO con hardnested!"
//                    }
//                }
//            } catch (e: Exception) {
//                withContext(Dispatchers.Main) {
//                    _progress.value = "Sector $sector: Error en hardnested"
//                }
//            }
//
//            delay(500)
//        }
//    }
//
//    private suspend fun executeMKF32Attack(mifare: MifareClassic) {
//        val foundKeys = _foundKeys.value.toMutableMap()
//
//        for (sector in 0 until mifare.sectorCount) {
//            _currentSector.value = sector
//            withContext(Dispatchers.Main) {
//                _progress.value = "Sector $sector: Ejecutando MKF32..."
//            }
//
//            try {
//                // Obtener el resultado completo de MKF32
//                val keyResult = mkfKey32.generateKey(
//                    uid = uid,
//                    sector = sector,
//                    algorithm = MKFKey32.KeyAlgorithm.ENHANCED
//                )
//
//                // Extraer la clave del resultado
//                val key = keyResult.key
//
//                // Probar la clave generada
//                val keyASuccess = try {
//                    mifare.authenticateSectorWithKeyA(sector, key)
//                } catch (e: Exception) {
//                    false
//                }
//
//                val keyBSuccess = try {
//                    mifare.authenticateSectorWithKeyB(sector, key)
//                } catch (e: Exception) {
//                    false
//                }
//
//                if (keyASuccess || keyBSuccess) {
//                    foundKeys[sector] = KeyPair(
//                        keyA = if (keyASuccess) key else null,
//                        keyB = if (keyBSuccess) key else null
//                    )
//
//                    withContext(Dispatchers.Main) {
//                        _foundKeys.value = foundKeys.toMap()
//                        _crackedSectors.value = foundKeys.keys.toSet()
//                        _progress.value = "Sector $sector: ¡CRACKEADO con MKF32! (Entropía: ${keyResult.entropy})"
//                    }
//                } else {
//                    // Opcional: probar con otros algoritmos MKF32
//                    val algorithms = arrayOf(
//                        MKFKey32.KeyAlgorithm.RUSSIAN_DOMOPHONE,
//                        MKFKey32.KeyAlgorithm.STATISTICAL,
//                        MKFKey32.KeyAlgorithm.ADAPTIVE
//                    )
//
//                    var cracked = false
//                    for (algorithm in algorithms) {
//                        if (cracked) break
//
//                        try {
//                            val altKeyResult = mkfKey32.generateKey(uid, sector, algorithm)
//                            val altKey = altKeyResult.key
//
//                            val altKeyASuccess = try {
//                                mifare.authenticateSectorWithKeyA(sector, altKey)
//                            } catch (e: Exception) {
//                                false
//                            }
//
//                            val altKeyBSuccess = try {
//                                mifare.authenticateSectorWithKeyB(sector, altKey)
//                            } catch (e: Exception) {
//                                false
//                            }
//
//                            if (altKeyASuccess || altKeyBSuccess) {
//                                foundKeys[sector] = KeyPair(
//                                    keyA = if (altKeyASuccess) altKey else null,
//                                    keyB = if (altKeyBSuccess) altKey else null
//                                )
//
//                                withContext(Dispatchers.Main) {
//                                    _foundKeys.value = foundKeys.toMap()
//                                    _crackedSectors.value = foundKeys.keys.toSet()
//                                    _progress.value = "Sector $sector: ¡CRACKEADO con MKF32 ${algorithm.name}!"
//                                }
//                                cracked = true
//                            }
//                        } catch (e: Exception) {
//                            continue
//                        }
//                    }
//                }
//            } catch (e: Exception) {
//                withContext(Dispatchers.Main) {
//                    _progress.value = "Sector $sector: Error en MKF32 - ${e.message}"
//                }
//            }
//
//            delay(100)
//        }
//    }
//
//    private suspend fun executeCombinedAttack(mifare: MifareClassic) {
//        // Ejecutar todos los ataques en secuencia
//        withContext(Dispatchers.Main) {
//            _progress.value = "Ejecutando ataque combinado..."
//        }
//
//        // 1. Ataque de diccionario básico
//        executeDictionaryAttack(mifare)
//
//        // 2. MKF32 para sectores restantes
//        executeMKF32Attack(mifare)
//
//        // 3. Hardnested si tenemos claves conocidas
//        if (_foundKeys.value.isNotEmpty()) {
//            executeHardnestedAttack(mifare)
//        }
//
//        // 4. Nonce attack para sectores restantes
//        executeNonceAttack(mifare)
//    }
//
//    private fun collectNonces(mifare: MifareClassic, sector: Int): List<NonceData> {
//        val nonces = mutableListOf<NonceData>()
//        val keys = keyDictionaries.getBasicKeys()
//
//        for (key in keys.take(10)) { // Limitar para no ser demasiado lento
//            try {
//                // Simular recolección de nonces
//                val nonce = ByteArray(4)
//                Random.nextBytes(nonce)
//                nonces.add(NonceData(nonce, 1,key, sector))
//            } catch (e: Exception) {
//                continue
//            }
//        }
//
//        return nonces
//    }
//
//    private suspend fun readWithFoundKeys(mifare: MifareClassic) {
//        val foundKeys = _foundKeys.value
//        val blocks = mutableListOf<BlockData>()
//
//        for (sector in 0 until mifare.sectorCount) {
//            _currentSector.value = sector
//            withContext(Dispatchers.Main) {
//                _progress.value = "Leyendo sector $sector..."
//            }
//
//            val keyPair = foundKeys[sector]
//            if (keyPair != null) {
//                try {
//                    val key = keyPair.keyA ?: keyPair.keyB!!
//                    val keyType = if (keyPair.keyA != null) "A" else "B"
//
//                    val authenticated = if (keyType == "A") {
//                        mifare.authenticateSectorWithKeyA(sector, key)
//                    } else {
//                        mifare.authenticateSectorWithKeyB(sector, key)
//                    }
//
//                    if (authenticated) {
//                        val blocksInSector = mifare.getBlockCountInSector(sector)
//                        val firstBlock = mifare.sectorToBlock(sector)
//
//                        for (i in 0 until blocksInSector) {
//                            val blockIndex = firstBlock + i
//                            try {
//                                val data = mifare.readBlock(blockIndex)
//                                blocks.add(
//                                    BlockData(
//                                        sector = sector,
//                                        block = blockIndex,
//                                        data = data,
//                                        isTrailer = (i == blocksInSector - 1),
//                                        keyUsed = key,
//                                        keyType = keyType,
//                                        cracked = true
//                                    )
//                                )
//                            } catch (e: Exception) {
//                                blocks.add(
//                                    BlockData(
//                                        sector = sector,
//                                        block = blockIndex,
//                                        data = byteArrayOf(),
//                                        isTrailer = (i == blocksInSector - 1),
//                                        error = "Error: ${e.message}",
//                                        keyType = keyType,
//                                        cracked = false
//                                    )
//                                )
//                            }
//                        }
//                    }
//                } catch (e: Exception) {
//                    // Sector no accesible
//                }
//            } else {
//                // Sector no crackeado
//                val blocksInSector = mifare.getBlockCountInSector(sector)
//                val firstBlock = mifare.sectorToBlock(sector)
//
//                for (i in 0 until blocksInSector) {
//                    blocks.add(
//                        BlockData(
//                            sector = sector,
//                            block = firstBlock + i,
//                            data = byteArrayOf(),
//                            isTrailer = (i == blocksInSector - 1),
//                            error = "Sector no crackeado",
//                            cracked = false
//                        )
//                    )
//                }
//            }
//        }
//
//        withContext(Dispatchers.Main) {
//            _cardData.value = blocks
//            storedData = blocks
//            _status.value = "Ataque completado: ${_crackedSectors.value.size}/${mifare.sectorCount} sectores crackeados"
//            _progress.value = "¡Completado!"
//        }
//    }
//
//    // Método de lectura normal mejorado
//    private suspend fun readCardAsync(mifare: MifareClassic) {
//        withContext(Dispatchers.IO) {
//            _isReading.value = true
//            _status.value = "Conectando con la tarjeta..."
//            _progress.value = "Iniciando lectura..."
//
//            try {
//                mifare.connect()
//                delay(100)
//
//                _totalSectors.value = mifare.sectorCount
//                _status.value = "Leyendo ${mifare.sectorCount} sectores..."
//
//                val blocks = mutableListOf<BlockData>()
//                val allKeys = keyDictionaries.getAllKeys()
//
//                for (sector in 0 until mifare.sectorCount) {
//                    _currentSector.value = sector
//                    _progress.value = "Sector $sector/${mifare.sectorCount - 1}"
//
//                    withContext(Dispatchers.Main) {
//                        _status.value = "Procesando sector $sector de ${mifare.sectorCount - 1}"
//                    }
//
//                    var authenticated = false
//                    var usedKey: ByteArray? = null
//                    var keyType = "A"
//
//                    // Probar todas las claves
//                    for ((index, key) in allKeys.withIndex()) {
//                        try {
//                            if (mifare.authenticateSectorWithKeyA(sector, key)) {
//                                authenticated = true
//                                usedKey = key
//                                keyType = "A"
//                                withContext(Dispatchers.Main) {
//                                    _progress.value = "Sector $sector: Autenticado con clave A (${index + 1}/${allKeys.size})"
//                                }
//                                break
//                            }
//                        } catch (e: Exception) {
//                            continue
//                        }
//
//                        if (index % 100 == 0) {
//                            delay(10)
//                        }
//                    }
//
//                    if (!authenticated) {
//                        for ((index, key) in allKeys.withIndex()) {
//                            try {
//                                if (mifare.authenticateSectorWithKeyB(sector, key)) {
//                                    authenticated = true
//                                    usedKey = key
//                                    keyType = "B"
//                                    withContext(Dispatchers.Main) {
//                                        _progress.value = "Sector $sector: Autenticado con clave B (${index + 1}/${allKeys.size})"
//                                    }
//                                    break
//                                }
//                            } catch (e: Exception) {
//                                continue
//                            }
//
//                            if (index % 100 == 0) {
//                                delay(10)
//                            }
//                        }
//                    }
//
//                    if (authenticated) {
//                        val blocksInSector = mifare.getBlockCountInSector(sector)
//                        val firstBlock = mifare.sectorToBlock(sector)
//
//                        for (i in 0 until blocksInSector) {
//                            val blockIndex = firstBlock + i
//                            try {
//                                val data = mifare.readBlock(blockIndex)
//                                blocks.add(
//                                    BlockData(
//                                        sector = sector,
//                                        block = blockIndex,
//                                        data = data,
//                                        isTrailer = (i == blocksInSector - 1),
//                                        keyUsed = usedKey,
//                                        keyType = keyType,
//                                        cracked = true
//                                    )
//                                )
//                            } catch (e: Exception) {
//                                blocks.add(
//                                    BlockData(
//                                        sector = sector,
//                                        block = blockIndex,
//                                        data = byteArrayOf(),
//                                        isTrailer = (i == blocksInSector - 1),
//                                        error = "Error: ${e.message}",
//                                        keyType = keyType,
//                                        cracked = false
//                                    )
//                                )
//                            }
//                        }
//
//                        withContext(Dispatchers.Main) {
//                            _cardData.value = blocks.toList()
//                        }
//
//                    } else {
//                        withContext(Dispatchers.Main) {
//                            _progress.value = "Sector $sector: No se pudo autenticar"
//                        }
//
//                        val blocksInSector = mifare.getBlockCountInSector(sector)
//                        val firstBlock = mifare.sectorToBlock(sector)
//
//                        for (i in 0 until blocksInSector) {
//                            blocks.add(
//                                BlockData(
//                                    sector = sector,
//                                    block = firstBlock + i,
//                                    data = byteArrayOf(),
//                                    isTrailer = (i == blocksInSector - 1),
//                                    error = "Sector no autenticado",
//                                    cracked = false
//                                )
//                            )
//                        }
//                    }
//
//                    delay(50)
//                }
//
//                withContext(Dispatchers.Main) {
//                    _cardData.value = blocks
//                    storedData = blocks
//                    _status.value = "Lectura completada: ${blocks.size} bloques"
//                    _progress.value = "¡Completado!"
//                }
//
//            } catch (e: Exception) {
//                withContext(Dispatchers.Main) {
//                    _status.value = "Error al leer tarjeta: ${e.message}"
//                    _progress.value = "Error en la lectura"
//                }
//            } finally {
//                try {
//                    mifare.close()
//                } catch (e: Exception) {
//                    // Ignorar errores al cerrar
//                }
//                withContext(Dispatchers.Main) {
//                    _isReading.value = false
//                }
//            }
//        }
//    }
//
//    private suspend fun writeCardAsync(mifare: MifareClassic) {
//        if (storedData.isEmpty()) {
//            _status.value = "No hay datos para escribir. Primero lee una tarjeta."
//            return
//        }
//
//        withContext(Dispatchers.IO) {
//            _isWriting.value = true
//            _status.value = "Conectando para escritura..."
//
//            try {
//                mifare.connect()
//                delay(100)
//
//                var writtenBlocks = 0
//                val writableBlocks = storedData.filter { !it.isTrailer && it.data.isNotEmpty() }
//
//                withContext(Dispatchers.Main) {
//                    _status.value = "Escribiendo ${writableBlocks.size} bloques..."
//                }
//
//                for ((index, blockData) in writableBlocks.withIndex()) {
//                    withContext(Dispatchers.Main) {
//                        _progress.value = "Escribiendo bloque ${index + 1}/${writableBlocks.size}"
//                    }
//
//                    try {
//                        val key = blockData.keyUsed ?: keyDictionaries.getBasicKeys()[0]
//                        var authenticated = false
//
//                        try {
//                            authenticated = if (blockData.keyType == "B") {
//                                mifare.authenticateSectorWithKeyB(blockData.sector, key)
//                            } else {
//                                mifare.authenticateSectorWithKeyA(blockData.sector, key)
//                            }
//                        } catch (e: Exception) {
//                            try {
//                                authenticated = if (blockData.keyType == "B") {
//                                    mifare.authenticateSectorWithKeyA(blockData.sector, key)
//                                } else {
//                                    mifare.authenticateSectorWithKeyB(blockData.sector, key)
//                                }
//                            } catch (e2: Exception) {
//                                continue
//                            }
//                        }
//
//                        if (authenticated) {
//                            try {
//                                mifare.writeBlock(blockData.block, blockData.data)
//                                writtenBlocks++
//                            } catch (e: Exception) {
//                                continue
//                            }
//                        }
//                    } catch (e: Exception) {
//                        continue
//                    }
//
//                    delay(50)
//                }
//
//                withContext(Dispatchers.Main) {
//                    _status.value = "Escritura completada: $writtenBlocks bloques escritos"
//                    _progress.value = "¡Escritura completada!"
//                }
//
//            } catch (e: Exception) {
//                withContext(Dispatchers.Main) {
//                    _status.value = "Error al escribir tarjeta: ${e.message}"
//                    _progress.value = "Error en la escritura"
//                }
//            } finally {
//                try {
//                    mifare.close()
//                } catch (e: Exception) {
//                    // Ignorar errores al cerrar
//                }
//                withContext(Dispatchers.Main) {
//                    _isWriting.value = false
//                }
//            }
//        }
//    }
//
//    fun cancelOperation() {
//        scope.launch {
//            _isReading.value = false
//            _isWriting.value = false
//            _status.value = "Operación cancelada"
//            _progress.value = ""
//        }
//    }
//
//    fun clearData() {
//        _cardData.value = emptyList()
//        storedData = emptyList()
//        _foundKeys.value = emptyMap()
//        _crackedSectors.value = emptySet()
//        _status.value = "Datos borrados. Acerca una tarjeta Mifare Classic"
//        _progress.value = ""
//    }
//}
//
