package com.eddyslarez.lectornfc.data.models


data class KeyPair(
    val keyA: ByteArray?,
    val keyB: ByteArray?
) {
    fun hasKeyA(): Boolean = keyA != null
    fun hasKeyB(): Boolean = keyB != null
    fun hasAnyKey(): Boolean = keyA != null || keyB != null

    fun keyAAsHex(): String? = keyA?.joinToString("") { "%02X".format(it) }
    fun keyBAsHex(): String? = keyB?.joinToString("") { "%02X".format(it) }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as KeyPair

        if (keyA != null) {
            if (other.keyA == null) return false
            if (!keyA.contentEquals(other.keyA)) return false
        } else if (other.keyA != null) return false

        if (keyB != null) {
            if (other.keyB == null) return false
            if (!keyB.contentEquals(other.keyB)) return false
        } else if (other.keyB != null) return false

        return true
    }

    override fun hashCode(): Int {
        var result = keyA?.contentHashCode() ?: 0
        result = 31 * result + (keyB?.contentHashCode() ?: 0)
        return result
    }
}