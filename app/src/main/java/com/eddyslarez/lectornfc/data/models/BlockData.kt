package com.eddyslarez.lectornfc.data.models


data class BlockData(
    val sector: Int,
    val block: Int,
    val data: ByteArray,
    val isTrailer: Boolean = false,
    val keyUsed: ByteArray? = null,
    val keyType: String = "A",
    val error: String? = null,
    val cracked: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
) {
    fun dataAsHex(): String {
        return if (data.isNotEmpty()) {
            data.joinToString(" ") { "%02X".format(it) }
        } else {
            error ?: "Sin datos"
        }
    }

    fun dataAsString(): String {
        return if (data.isNotEmpty()) {
            data.map { byte ->
                val char = byte.toInt() and 0xFF
                if (char in 32..126) char.toChar() else '.'
            }.joinToString("")
        } else {
            ""
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as BlockData

        if (sector != other.sector) return false
        if (block != other.block) return false
        if (!data.contentEquals(other.data)) return false
        if (isTrailer != other.isTrailer) return false

        return true
    }

    override fun hashCode(): Int {
        var result = sector
        result = 31 * result + block
        result = 31 * result + data.contentHashCode()
        result = 31 * result + isTrailer.hashCode()
        return result
    }
}