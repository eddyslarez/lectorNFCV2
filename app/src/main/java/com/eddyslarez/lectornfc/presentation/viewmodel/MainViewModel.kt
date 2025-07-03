package com.eddyslarez.lectornfc.presentation.viewmodel


import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eddyslarez.lectornfc.data.models.*
import com.eddyslarez.lectornfc.domain.attacks.AdvancedMifareManager
import com.eddyslarez.lectornfc.domain.usecases.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class MainViewModel(
    private val readCardUseCase: ReadCardUseCase,
    private val writeCardUseCase: WriteCardUseCase,
    private val crackCardUseCase: CrackCardUseCase,
    private val exportDataUseCase: ExportDataUseCase,
    private val advancedMifareManager: AdvancedMifareManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<MainEvent>()
    val events: SharedFlow<MainEvent> = _events.asSharedFlow()

    private val _showWriteConfirmation = MutableStateFlow(false)
    val showWriteConfirmation: StateFlow<Boolean> = _showWriteConfirmation.asStateFlow()

    init {
        observeMifareManager()
    }

    private fun observeMifareManager() {
        viewModelScope.launch {
            combine(
                advancedMifareManager.mode,
                advancedMifareManager.isReading,
                advancedMifareManager.isWriting,
                advancedMifareManager.status,
                advancedMifareManager.progress,
                advancedMifareManager.currentSector,
                advancedMifareManager.totalSectors,
                advancedMifareManager.attackMethod,
                advancedMifareManager.cardData,
                advancedMifareManager.foundKeys,
                advancedMifareManager.crackedSectors
            ) { flows ->
                val mode = flows[0] as OperationMode
                val isReading = flows[1] as Boolean
                val isWriting = flows[2] as Boolean
                val status = flows[3] as String
                val progress = flows[4] as String
                val currentSector = flows[5] as Int
                val totalSectors = flows[6] as Int
                val attackMethod = flows[7] as AttackMethod
                val cardData = flows[8] as List<BlockData>
                val foundKeys = flows[9] as Map<Int, KeyPair>
                val crackedSectors = flows[10] as Set<Int>

                _uiState.value = _uiState.value.copy(
                    operationMode = mode,
                    isReading = isReading,
                    isWriting = isWriting,
                    status = status,
                    progress = progress,
                    currentSector = currentSector,
                    totalSectors = totalSectors,
                    attackMethod = attackMethod,
                    cardData = cardData,
                    foundKeys = foundKeys,
                    crackedSectors = crackedSectors,
                    isOperationInProgress = isReading || isWriting
                )
            }.collect()
        }
    }

    fun setOperationMode(mode: OperationMode) {
        if (mode == OperationMode.WRITE && _uiState.value.cardData.isNotEmpty()) {
            _showWriteConfirmation.value = true
        } else {
            advancedMifareManager.setMode(mode)
        }
    }

    fun confirmWrite() {
        _showWriteConfirmation.value = false
        advancedMifareManager.setMode(OperationMode.WRITE)
    }

    fun cancelWrite() {
        _showWriteConfirmation.value = false
    }

    fun setAttackMethod(method: AttackMethod) {
        advancedMifareManager.setAttackMethod(method)
    }

    fun cancelOperation() {
        advancedMifareManager.cancelOperation()
    }

    fun clearData() {
        advancedMifareManager.clearData()
        _uiState.value = _uiState.value.copy(
            cardData = emptyList(),
            foundKeys = emptyMap(),
            crackedSectors = emptySet()
        )
    }

    fun exportData(format: ExportFormat) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isExporting = true)

                val result = exportDataUseCase(
                    cardData = _uiState.value.cardData,
                    foundKeys = _uiState.value.foundKeys,
                    format = format
                )

                if (result.isSuccess) {
                    _events.emit(MainEvent.ExportSuccess(result.getOrNull()!!))
                } else {
                    _events.emit(MainEvent.ExportError(result.exceptionOrNull()?.message ?: "Error desconocido"))
                }
            } catch (e: Exception) {
                _events.emit(MainEvent.ExportError(e.message ?: "Error al exportar"))
            } finally {
                _uiState.value = _uiState.value.copy(isExporting = false)
            }
        }
    }

    fun shareData(shareMethod: ShareMethod) {
        viewModelScope.launch {
            try {
                // Implementar lógica de compartir
                _events.emit(MainEvent.ShareSuccess)
            } catch (e: Exception) {
                _events.emit(MainEvent.ShareError(e.message ?: "Error al compartir"))
            }
        }
    }

    fun showHelp(topic: HelpTopic) {
        _uiState.value = _uiState.value.copy(
            showHelpDialog = true,
            helpTopic = topic
        )
    }

    fun hideHelp() {
        _uiState.value = _uiState.value.copy(
            showHelpDialog = false,
            helpTopic = null
        )
    }

    fun showExportDialog() {
        _uiState.value = _uiState.value.copy(showExportDialog = true)
    }

    fun hideExportDialog() {
        _uiState.value = _uiState.value.copy(showExportDialog = false)
    }
}

data class MainUiState(
    val operationMode: OperationMode = OperationMode.READ,
    val isReading: Boolean = false,
    val isWriting: Boolean = false,
    val isExporting: Boolean = false,
    val isOperationInProgress: Boolean = false,
    val status: String = "Acerca una tarjeta Mifare Classic",
    val progress: String = "",
    val currentSector: Int = 0,
    val totalSectors: Int = 0,
    val attackMethod: AttackMethod = AttackMethod.DICTIONARY,
    val cardData: List<BlockData> = emptyList(),
    val foundKeys: Map<Int, KeyPair> = emptyMap(),
    val crackedSectors: Set<Int> = emptySet(),
    val showExportDialog: Boolean = false,
    val showHelpDialog: Boolean = false,
    val helpTopic: HelpTopic? = null
)

sealed class MainEvent {
    data class ExportSuccess(val filePath: String) : MainEvent()
    data class ExportError(val message: String) : MainEvent()
    object ShareSuccess : MainEvent()
    data class ShareError(val message: String) : MainEvent()
}

enum class ExportFormat {
    TXT, JSON, CSV, XML
}

enum class ShareMethod {
    EMAIL, WHATSAPP, TELEGRAM, FILE
}

enum class HelpTopic {
    READING, WRITING, CRACKING, DICTIONARY_ATTACK, HARDNESTED_ATTACK,
    NONCE_ATTACK, MKF32_ATTACK, EXPORT_DATA, SECURITY_TIPS
}