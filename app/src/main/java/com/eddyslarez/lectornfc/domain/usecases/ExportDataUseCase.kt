package com.eddyslarez.lectornfc.domain.usecases

import com.eddyslarez.lectornfc.data.models.BlockData
import com.eddyslarez.lectornfc.data.models.KeyPair
import com.eddyslarez.lectornfc.presentation.viewmodel.ExportFormat
import com.eddyslarez.lectornfc.utils.ExportManager

class ExportDataUseCase(
    private val exportManager: ExportManager
) {
    suspend operator fun invoke(
        cardData: List<BlockData>,
        foundKeys: Map<Int, KeyPair>,
        format: ExportFormat
    ): Result<String> {
        return exportManager.exportData(cardData, foundKeys, format)
    }

    suspend fun exportSummary(
        cardData: List<BlockData>,
        foundKeys: Map<Int, KeyPair>
    ): ExportSummary {
        val totalBlocks = cardData.size
        val readableBlocks = cardData.count { it.cracked }
        val totalSectors = cardData.map { it.sector }.distinct().size
        val crackedSectors = foundKeys.size
        val successRate = if (totalSectors > 0) (crackedSectors.toFloat() / totalSectors) * 100 else 0f

        return ExportSummary(
            totalBlocks = totalBlocks,
            readableBlocks = readableBlocks,
            totalSectors = totalSectors,
            crackedSectors = crackedSectors,
            foundKeys = foundKeys.size,
            successRate = successRate,
            dataSize = cardData.sumOf { it.data.size }
        )
    }
}

data class ExportSummary(
    val totalBlocks: Int,
    val readableBlocks: Int,
    val totalSectors: Int,
    val crackedSectors: Int,
    val foundKeys: Int,
    val successRate: Float,
    val dataSize: Int
)