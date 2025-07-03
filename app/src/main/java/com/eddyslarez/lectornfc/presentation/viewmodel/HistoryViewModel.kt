package com.eddyslarez.lectornfc.presentation.viewmodel


import android.util.Log
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
                // CAMBIO PRINCIPAL: Usar una sola llamada en lugar de collect
                val historyEntities = repository.getAllScanHistoryOnce() // Método que retorna List directamente
                _historyItems.value = historyEntities
            } catch (e: Exception) {
                Log.e("HistoryViewModel", "Error loading history", e)
                _historyItems.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }
    fun debugCheckDatabase() {
        viewModelScope.launch {
            try {
                val count = repository.getTotalScansCount()
                Log.d("HistoryViewModel", "Total scans in database: $count")

                val items = repository.getAllScanHistoryOnce()
                Log.d("HistoryViewModel", "Items retrieved: ${items.size}")
                items.forEach { item ->
                    Log.d("HistoryViewModel", "Item: ${item.uid} - ${item.timestamp}")
                }
            } catch (e: Exception) {
                Log.e("HistoryViewModel", "Error checking database", e)
            }
        }
    }
    // ALTERNATIVA: Si quieres observar cambios en tiempo real
    fun observeHistory() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                repository.getAllScanHistory().collect { historyEntities ->
                    _historyItems.value = historyEntities
                    _isLoading.value = false // Solo la primera vez
                }
            } catch (e: Exception) {
                Log.e("HistoryViewModel", "Error observing history", e)
                _historyItems.value = emptyList()
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
                Log.e("HistoryViewModel", "Error loading statistics", e)
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
                Log.e("HistoryViewModel", "Error deleting item", e)
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
                Log.e("HistoryViewModel", "Error clearing history", e)
            }
        }
    }

    fun exportItem(item: ScanHistoryEntity) {
        viewModelScope.launch {
            try {
                // Marcar como exportado
                repository.markAsExported(item.id)
                loadHistory() // Recargar para mostrar el cambio
                // Aquí puedes agregar la lógica de exportación
                Log.d("HistoryViewModel", "Exporting item: ${item.uid}")
            } catch (e: Exception) {
                Log.e("HistoryViewModel", "Error exporting item", e)
            }
        }
    }

    fun viewItem(item: ScanHistoryEntity) {
        // Implementar navegación a detalles
        Log.d("HistoryViewModel", "Viewing item: ${item.uid}")
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