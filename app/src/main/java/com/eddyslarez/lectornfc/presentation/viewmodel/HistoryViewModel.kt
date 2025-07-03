package com.eddyslarez.lectornfc.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eddyslarez.lectornfc.data.database.entities.ScanHistoryEntity
import com.eddyslarez.lectornfc.data.repository.MifareRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class HistoryViewModel(
    private val repository: MifareRepository
) : ViewModel() {

    private val _historyItems = MutableStateFlow<List<ScanHistoryEntity>>(emptyList())
    val historyItems: StateFlow<List<ScanHistoryEntity>> = _historyItems.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _statistics = MutableStateFlow<HistoryStatistics?>(null)
    val statistics: StateFlow<HistoryStatistics?> = _statistics.asStateFlow()

    init {
        loadHistory()
        loadStatistics()
    }

    fun loadHistory() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                repository.getAllScanHistory().collect { historyEntities ->
                    _historyItems.value = historyEntities
                }
            } catch (e: Exception) {
                // Manejar error
                _historyItems.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun loadStatistics() {
        viewModelScope.launch {
            try {
                val totalScans = repository.getTotalScansCount()
                val successfulScans = repository.getSuccessfulScansCount()
                val totalSessions = repository.getTotalSessionsCount()
                val uniqueCards = repository.getUniqueCardsCount()

                _statistics.value = HistoryStatistics(
                    totalScans = totalScans,
                    successfulScans = successfulScans,
                    totalSessions = totalSessions,
                    uniqueCards = uniqueCards,
                    successRate = if (totalScans > 0) {
                        (successfulScans.toFloat() / totalScans) * 100
                    } else 0f
                )
            } catch (e: Exception) {
                // Manejar error
            }
        }
    }

    fun deleteItem(item: ScanHistoryEntity) {
        viewModelScope.launch {
            try {
                repository.deleteScanHistory(item)
                loadHistory() // Recargar después de borrar
                loadStatistics()
            } catch (e: Exception) {
                // Manejar error
            }
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            try {
                repository.clearAllHistory()
                _historyItems.value = emptyList()
                loadStatistics()
            } catch (e: Exception) {
                // Manejar error
            }
        }
    }

    fun exportItem(item: ScanHistoryEntity) {
        viewModelScope.launch {
            try {
                // Implementar exportación individual
                // val exportManager = ExportManager(context)
                // exportManager.exportScanHistory(item)
            } catch (e: Exception) {
                // Manejar error
            }
        }
    }

    fun viewItem(item: ScanHistoryEntity) {
        viewModelScope.launch {
            try {
                // Implementar visualización detallada
                // Navegar a una pantalla de detalles o mostrar un diálogo
            } catch (e: Exception) {
                // Manejar error
            }
        }
    }

    data class HistoryStatistics(
        val totalScans: Int,
        val successfulScans: Int,
        val totalSessions: Int,
        val uniqueCards: Int,
        val successRate: Float
    )

    data class DateRange(
        val startDate: Long,
        val endDate: Long
    )

    enum class SortCriteria {
        DATE, SUCCESS_RATE, CARD_TYPE, ATTACK_METHOD, DURATION
    }
}