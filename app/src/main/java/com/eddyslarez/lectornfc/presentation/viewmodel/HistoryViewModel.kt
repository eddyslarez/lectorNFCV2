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
                repository.getAllScanResults().collect { scanResults ->
                    // Convertir ScanResult a ScanHistoryEntity
                    val historyEntities = scanResults.map { scanResult ->
                        ScanHistoryEntity(
                            uid = scanResult.uid,
                            cardType = scanResult.cardType,
                            timestamp = scanResult.timestamp.time,
                            totalSectors = scanResult.sectorCount,
                            crackedSectors = scanResult.crackedSectors,
                            totalBlocks = scanResult.totalBlocks,
                            readableBlocks = scanResult.readableBlocks,
                            foundKeys = 0, // Se podría calcular desde FoundKey
                            attackMethod = scanResult.attackMethod,
                            operationMode = "SCAN",
                            scanDuration = scanResult.duration,
                            successRate = if (scanResult.sectorCount > 0) {
                                (scanResult.crackedSectors.toFloat() / scanResult.sectorCount) * 100
                            } else 0f,
                            rawData = "",
                            notes = scanResult.notes
                        )
                    }
                    _historyItems.value = historyEntities
                }
            } catch (e: Exception) {
                // Manejar error
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
                // Aquí necesitarías implementar el método de borrado en el repositorio
                // repository.deleteScanHistory(item)
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
                // Implementar borrado masivo
                // repository.clearAllHistory()
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

    fun filterHistory(query: String, dateRange: DateRange? = null, attackMethod: String? = null) {
        viewModelScope.launch {
            try {
                val allItems = _historyItems.value
                val filteredItems = allItems.filter { item ->
                    val matchesQuery = query.isEmpty() ||
                            item.uid.contains(query, ignoreCase = true) ||
                            item.cardType.contains(query, ignoreCase = true) ||
                            item.notes.contains(query, ignoreCase = true)

                    val matchesDateRange = dateRange == null ||
                            (item.timestamp >= dateRange.startDate && item.timestamp <= dateRange.endDate)

                    val matchesAttackMethod = attackMethod == null ||
                            item.attackMethod.equals(attackMethod, ignoreCase = true)

                    matchesQuery && matchesDateRange && matchesAttackMethod
                }

                _historyItems.value = filteredItems
            } catch (e: Exception) {
                // Manejar error
            }
        }
    }

    fun sortHistory(sortBy: SortCriteria, ascending: Boolean = true) {
        viewModelScope.launch {
            val sortedItems = when (sortBy) {
                SortCriteria.DATE -> {
                    if (ascending) _historyItems.value.sortedBy { it.timestamp }
                    else _historyItems.value.sortedByDescending { it.timestamp }
                }
                SortCriteria.SUCCESS_RATE -> {
                    if (ascending) _historyItems.value.sortedBy { it.successRate }
                    else _historyItems.value.sortedByDescending { it.successRate }
                }
                SortCriteria.CARD_TYPE -> {
                    if (ascending) _historyItems.value.sortedBy { it.cardType }
                    else _historyItems.value.sortedByDescending { it.cardType }
                }
                SortCriteria.ATTACK_METHOD -> {
                    if (ascending) _historyItems.value.sortedBy { it.attackMethod }
                    else _historyItems.value.sortedByDescending { it.attackMethod }
                }
                SortCriteria.DURATION -> {
                    if (ascending) _historyItems.value.sortedBy { it.scanDuration }
                    else _historyItems.value.sortedByDescending { it.scanDuration }
                }
            }
            _historyItems.value = sortedItems
        }
    }

    fun getItemsByDateRange(startDate: Long, endDate: Long): List<ScanHistoryEntity> {
        return _historyItems.value.filter { item ->
            item.timestamp >= startDate && item.timestamp <= endDate
        }
    }

    fun getItemsByAttackMethod(attackMethod: String): List<ScanHistoryEntity> {
        return _historyItems.value.filter { item ->
            item.attackMethod.equals(attackMethod, ignoreCase = true)
        }
    }

    fun getSuccessfulItems(): List<ScanHistoryEntity> {
        return _historyItems.value.filter { item ->
            item.successRate > 0f
        }
    }

    fun getRecentItems(count: Int = 10): List<ScanHistoryEntity> {
        return _historyItems.value
            .sortedByDescending { it.timestamp }
            .take(count)
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