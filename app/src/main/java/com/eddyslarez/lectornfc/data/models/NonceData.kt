package com.eddyslarez.lectornfc.data.models


data class NonceData(
    val nonce: ByteArray,
    val timestamp: Long = System.currentTimeMillis(),
    val key: ByteArray,
    val sector: Int,
    val sequenceNumber: Int = 0,
    val source: String = "unknown"
) {
    fun nonceAsHex(): String = nonce.joinToString("") { "%02X".format(it) }
    fun keyAsHex(): String = key.joinToString("") { "%02X".format(it) }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as NonceData

        if (!nonce.contentEquals(other.nonce)) return false
        if (timestamp != other.timestamp) return false
        if (!key.contentEquals(other.key)) return false
        if (sector != other.sector) return false

        return true
    }

    override fun hashCode(): Int {
        var result = nonce.contentHashCode()
        result = 31 * result + timestamp.hashCode()
        result = 31 * result + key.contentHashCode()
        result = 31 * result + sector
        return result
    }
}