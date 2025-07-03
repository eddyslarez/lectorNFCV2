package com.eddyslarez.lectornfc.domain.usecases


import android.nfc.tech.MifareClassic
import com.eddyslarez.lectornfc.data.models.BlockData
import com.eddyslarez.lectornfc.data.repository.MifareRepository
import kotlinx.coroutines.flow.Flow

class ReadCardUseCase(
    private val repository: MifareRepository
) {
    suspend operator fun invoke(mifare: MifareClassic): Flow<List<BlockData>> {
        return repository.readCard(mifare)
    }

    suspend fun readSingleSector(mifare: MifareClassic, sector: Int): List<BlockData> {
        return repository.readSector(mifare, sector)
    }

//    suspend fun readWithKeys(mifare: MifareClassic, keys: Map<Int, ByteArray>): Flow<List<BlockData>> {
//        return repository.readCardWithKeys(mifare, keys)
//    }
}
