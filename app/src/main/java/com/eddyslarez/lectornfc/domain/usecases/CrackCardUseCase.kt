package com.eddyslarez.lectornfc.domain.usecases


import android.nfc.tech.MifareClassic
import com.eddyslarez.lectornfc.data.models.AttackMethod
import com.eddyslarez.lectornfc.data.models.KeyPair
import com.eddyslarez.lectornfc.data.repository.MifareRepository
import kotlinx.coroutines.flow.Flow

class CrackCardUseCase(
    private val repository: MifareRepository
) {
    suspend operator fun invoke(
        mifare: MifareClassic,
        method: AttackMethod
    ): Flow<CrackResult> {
        return repository.crackCard(mifare, method)
    }

    suspend fun crackSingleSector(
        mifare: MifareClassic,
        sector: Int,
        method: AttackMethod
    ): KeyPair? {
        return repository.crackSector(mifare, sector, method)
    }

    suspend fun validateCard(mifare: MifareClassic): CardValidation {
        return try {
            val uid = mifare.tag.id
            val size = mifare.size
            val sectorCount = mifare.sectorCount
            val type = mifare.type

            CardValidation(
                isValid = true,
                uid = uid,
                size = size,
                sectorCount = sectorCount,
                type = type,
                isSupported = type == MifareClassic.TYPE_CLASSIC
            )
        } catch (e: Exception) {
            CardValidation(
                isValid = false,
                error = e.message
            )
        }
    }
}

data class CrackResult(
    val sector: Int,
    val keyPair: KeyPair?,
    val method: AttackMethod,
    val success: Boolean,
    val timeElapsed: Long,
    val attempts: Int = 0
)

data class CardValidation(
    val isValid: Boolean,
    val uid: ByteArray? = null,
    val size: Int = 0,
    val sectorCount: Int = 0,
    val type: Int = 0,
    val isSupported: Boolean = false,
    val error: String? = null
)
