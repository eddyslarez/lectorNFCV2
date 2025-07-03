package com.eddyslarez.lectornfc.domain.attacks

import android.nfc.tech.MifareClassic
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.viewModelScope
import com.eddyslarez.lectornfc.data.models.*
import com.eddyslarez.lectornfc.data.repository.MifareRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

class AdvancedMifareManager(
    private val repository: MifareRepository,
    private val dictionaryAttack: DictionaryAttack,
    private val hardnestedAttacker: HardnestedAttacker,
    private val nonceAnalyzer: NonceAnalyzer,
    private val mkfKey32: MKFKey32,
    private val bruteForceAttack: BruteForceAttack
) {
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    
    // Estados observables
    private val _mode = MutableStateFlow(OperationMode.read)
    val mode: StateFlow<OperationMode> = _mode.asStateFlow()

    private val _isReading = MutableStateFlow(false)
    val isReading: StateFlow<Boolean> = _isReading.asStateFlow()

    private val _isWriting = MutableStateFlow(false)
    val isWriting: StateFlow<Boolean> = _isWriting.asStateFlow()

    private val _status = MutableStateFlow("Acerca una tarjeta Mifare Classic")
    val status: StateFlow<String> = _status.asStateFlow()

    private val _progress = MutableStateFlow("")
    val progress: StateFlow<String> = _progress.asStateFlow()

    private val _currentSector = MutableStateFlow(0)
    val currentSector: StateFlow<Int> = _currentSector.asStateFlow()

    private val _totalSectors = MutableStateFlow(0)
    val totalSectors: StateFlow<Int> = _totalSectors.asStateFlow()

    private val _attackMethod = MutableStateFlow(AttackMethod.DICTIONARY)
    val attackMethod: StateFlow<AttackMethod> = _attackMethod.asStateFlow()

    private val _cardData = MutableStateFlow<List<BlockData>>(emptyList())
    val cardData: StateFlow<List<BlockData>> = _cardData.asStateFlow()

    private val _foundKeys = MutableStateFlow<Map<Int, KeyPair>>(emptyMap())
    val foundKeys: StateFlow<Map<Int, KeyPair>> = _foundKeys.asStateFlow()

    private val _crackedSectors = MutableStateFlow<Set<Int>>(emptySet())
    val crackedSectors: StateFlow<Set<Int>> = _crackedSectors.asStateFlow()

    private var currentJob: Job? = null

    fun setMode(newMode: OperationMode) {
        if (!_isReading.value && !_isWriting.value) {
            _mode.value = newMode
            _status.value = when (newMode) {
                OperationMode.read -> "Acerca una tarjeta para leer"
                OperationMode.WRITE -> "Acerca una tarjeta para escribir"
                OperationMode.CRACK -> "Acerca una tarjeta para crackear"
            }
        }
    }

    fun setAttackMethod(method: AttackMethod) {
        _attackMethod.value = method
    }

    fun processNewTag(mifare: MifareClassic) {
        when (_mode.value) {
            OperationMode.read -> startReading(mifare)
            OperationMode.WRITE -> startWriting(mifare)
            OperationMode.CRACK -> startCracking(mifare)
        }
    }

    private fun startReading(mifare: MifareClassic) {
        currentJob?.cancel()
        currentJob = scope.launch {
            try {
                _isReading.value = true
                _status.value = "Leyendo tarjeta..."
                _totalSectors.value = mifare.sectorCount
                _currentSector.value = 0

                repository.readCard(mifare).collect { blocks ->
                    _cardData.value = blocks
                    _currentSector.value = blocks.map { it.sector }.distinct().size
                    _progress.value = "Leídos ${blocks.count { it.cracked }} de ${blocks.size} bloques"
                }

                _status.value = "Lectura completada"
                _progress.value = "Tarjeta leída exitosamente"
            } catch (e: Exception) {
                _status.value = "Error en lectura: ${e.message}"
            } finally {
                _isReading.value = false
            }
        }
    }

    private fun startWriting(mifare: MifareClassic) {
        if (_cardData.value.isEmpty()) {
            _status.value = "No hay datos para escribir"
            return
        }

        currentJob?.cancel()
        currentJob = scope.launch {
            try {
                _isWriting.value = true
                _status.value = "Escribiendo tarjeta..."

                repository.writeCard(mifare, _cardData.value).collect { result ->
                    _progress.value = "Escritos ${result.writtenBlocks} de ${result.totalBlocks} bloques"
                    
                    if (result.success) {
                        _status.value = "Escritura completada"
                    } else {
                        _status.value = "Error en escritura: ${result.errors.firstOrNull() ?: "Error desconocido"}"
                    }
                }
            } catch (e: Exception) {
                _status.value = "Error en escritura: ${e.message}"
            } finally {
                _isWriting.value = false
            }
        }
    }

    private fun startCracking(mifare: MifareClassic) {
        currentJob?.cancel()
        currentJob = scope.launch {
            try {
                _isReading.value = true
                _status.value = "Crackeando tarjeta..."
                _totalSectors.value = mifare.sectorCount
                _currentSector.value = 0

                val foundKeysMap = mutableMapOf<Int, KeyPair>()

                repository.crackCard(mifare, _attackMethod.value).collect { result ->
                    _currentSector.value = result.sector + 1
                    _progress.value = "Crackeando sector ${result.sector}/${mifare.sectorCount}"

                    if (result.success && result.keyPair != null) {
                        foundKeysMap[result.sector] = result.keyPair
                        _foundKeys.value = foundKeysMap.toMap()
                        _crackedSectors.value = foundKeysMap.keys.toSet()
                    }
                }

                _status.value = "Cracking completado: ${foundKeysMap.size} sectores crackeados"
            } catch (e: Exception) {
                _status.value = "Error en cracking: ${e.message}"
            } finally {
                _isReading.value = false
            }
        }
    }

    fun cancelOperation() {
        currentJob?.cancel()
        _isReading.value = false
        _isWriting.value = false
        _status.value = "Operación cancelada"
        _progress.value = ""
    }

    fun clearData() {
        _cardData.value = emptyList()
        _foundKeys.value = emptyMap()
        _crackedSectors.value = emptySet()
        _currentSector.value = 0
        _totalSectors.value = 0
        _progress.value = ""
        _status.value = "Datos limpiados"
    }
}