package com.eddyslarez.lectornfc.domain.usecases

import android.nfc.tech.MifareClassic
import com.eddyslarez.lectornfc.data.models.BlockData
import com.eddyslarez.lectornfc.data.repository.MifareRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class WriteCardUseCase(
    private val repository: MifareRepository
) {
    suspend operator fun invoke(
        mifare: MifareClassic,
        data: List<BlockData>
    ): Flow<WriteResult> {
        return repository.writeCard(mifare, data).map { repositoryResult ->
            WriteResult(
                success = repositoryResult.success,
                writtenBlocks = repositoryResult.writtenBlocks,
                totalBlocks = repositoryResult.totalBlocks,
                errors = repositoryResult.errors
            )
        }
    }

    suspend fun writeSingleBlock(mifare: MifareClassic, blockData: BlockData): Boolean {
        return repository.writeBlock(mifare, blockData)
    }

    suspend fun validateWriteData(data: List<BlockData>): ValidationResult {
        val errors = mutableListOf<String>()

        // Validar que no hay bloques de trailer
        val trailerBlocks = data.filter { it.isTrailer }
        if (trailerBlocks.isNotEmpty()) {
            errors.add("No se pueden escribir bloques de trailer")
        }

        // Validar que los datos no están vacíos
        val emptyBlocks = data.filter { it.data.isEmpty() }
        if (emptyBlocks.isNotEmpty()) {
            errors.add("Hay bloques sin datos")
        }

        // Validar tamaño de datos
        val invalidSizeBlocks = data.filter { it.data.size != 16 }
        if (invalidSizeBlocks.isNotEmpty()) {
            errors.add("Bloques con tamaño incorrecto (debe ser 16 bytes)")
        }

        return ValidationResult(
            isValid = errors.isEmpty(),
            errors = errors,
            writableBlocks = data.filter { !it.isTrailer && it.data.isNotEmpty() && it.data.size == 16 }
        )
    }
}

data class WriteResult(
    val success: Boolean,
    val writtenBlocks: Int,
    val totalBlocks: Int,
    val errors: List<String> = emptyList()
)

data class ValidationResult(
    val isValid: Boolean,
    val errors: List<String>,
    val writableBlocks: List<BlockData>
)