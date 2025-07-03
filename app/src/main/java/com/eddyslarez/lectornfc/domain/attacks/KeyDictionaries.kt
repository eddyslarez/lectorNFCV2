package com.eddyslarez.lectornfc.domain.attacks

class KeyDictionaries {

    companion object {
        // Diccionario massivo y completo para ataques profesionales

        // Claves por defecto MIFARE Classic más comunes
        private val defaultKeys = listOf(
            byteArrayOf(0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte()),
            byteArrayOf(0x00, 0x00, 0x00, 0x00, 0x00, 0x00),
            byteArrayOf(0xA0.toByte(), 0xA1.toByte(), 0xA2.toByte(), 0xA3.toByte(), 0xA4.toByte(), 0xA5.toByte()),
            byteArrayOf(0xB0.toByte(), 0xB1.toByte(), 0xB2.toByte(), 0xB3.toByte(), 0xB4.toByte(), 0xB5.toByte()),
            byteArrayOf(0x4D, 0x3A, 0x99.toByte(), 0xC3.toByte(), 0x51, 0xDD.toByte()),
            byteArrayOf(0x1A, 0x98.toByte(), 0x2C, 0x7E, 0x45, 0x9A.toByte()),
            byteArrayOf(0xD3.toByte(), 0xF7.toByte(), 0xD3.toByte(), 0xF7.toByte(), 0xD3.toByte(), 0xF7.toByte()),
            byteArrayOf(0xAA.toByte(), 0xBB.toByte(), 0xCC.toByte(), 0xDD.toByte(), 0xEE.toByte(), 0xFF.toByte())
        )

        // Claves de transporte público internacionales
        private val transportKeys = listOf(
            // Claves comunes de sistemas de transporte
            byteArrayOf(0x8F.toByte(), 0xD0.toByte(), 0xA4.toByte(), 0xF2.toByte(), 0x56, 0xE9.toByte()),
            byteArrayOf(0x74, 0x5C, 0xB9.toByte(), 0x39, 0xDA.toByte(), 0x90.toByte()),
            byteArrayOf(0x48, 0x45, 0x4C, 0x4C, 0x4F, 0x21), // "HELLO!"
            byteArrayOf(0x42, 0x52, 0x45, 0x41, 0x4B, 0x4D), // "BREAKM"
            byteArrayOf(0x4E, 0x46, 0x43, 0x4B, 0x45, 0x59), // "NFCKEY"
            byteArrayOf(0x12, 0x34, 0x56, 0x78, 0x9A.toByte(), 0xBC.toByte()),
            byteArrayOf(0xFE.toByte(), 0xDC.toByte(), 0xBA.toByte(), 0x98.toByte(), 0x76, 0x54),
            byteArrayOf(0x11, 0x22, 0x33, 0x44, 0x55, 0x66),
            byteArrayOf(0x77, 0x88.toByte(), 0x99.toByte(), 0xAA.toByte(), 0xBB.toByte(), 0xCC.toByte()),
            byteArrayOf(0x01, 0x23, 0x45, 0x67, 0x89.toByte(), 0xAB.toByte()),

            // Claves específicas de sistemas conocidos
            byteArrayOf(0x04, 0x1A, 0xAD.toByte(), 0x15, 0x3A, 0x9C.toByte()), // Oyster Card London
            byteArrayOf(0x1A, 0x98.toByte(), 0x2C, 0x7E, 0x45, 0x9A.toByte()), // Clipper Card SF
            byteArrayOf(0x2A, 0x2C, 0x13, 0xCC.toByte(), 0x09, 0x84.toByte()), // Charlie Card Boston
            byteArrayOf(0x64, 0x05, 0x2A, 0x77, 0x45, 0x13), // Suica Japan
            byteArrayOf(0x41, 0x42, 0x43, 0x44, 0x45, 0x46), // "ABCDEF"
            byteArrayOf(0x31, 0x32, 0x33, 0x34, 0x35, 0x36), // "123456"
        )

        // Claves de sistemas de acceso y hoteles
        private val accessKeys = listOf(
            byteArrayOf(0x48, 0x4F, 0x54, 0x45, 0x4C, 0x31), // "HOTEL1"
            byteArrayOf(0x52, 0x4F, 0x4F, 0x4D, 0x4B, 0x59), // "ROOMKY"
            byteArrayOf(0x47, 0x55, 0x45, 0x53, 0x54, 0x31), // "GUEST1"
            byteArrayOf(0x4B, 0x45, 0x59, 0x43, 0x41, 0x52), // "KEYCAR"
            byteArrayOf(0x41, 0x43, 0x43, 0x45, 0x53, 0x53), // "ACCESS"
            byteArrayOf(0x4D, 0x41, 0x53, 0x54, 0x45, 0x52), // "MASTER"
            byteArrayOf(0x41, 0x44, 0x4D, 0x49, 0x4E, 0x31), // "ADMIN1"
            byteArrayOf(0x50, 0x41, 0x53, 0x53, 0x57, 0x44), // "PASSWD"
        )

        // Claves específicas de sistemas rusos (expandido significativamente)
        private val russianDomophoneKeys = listOf(
            // Claves Vizit
            byteArrayOf(0x56, 0x49, 0x7A, 0x49, 0x54, 0x31), // "VIZIT1"
            byteArrayOf(0x56, 0x49, 0x7A, 0x49, 0x54, 0x32), // "VIZIT2"
            byteArrayOf(0x56, 0x49, 0x5A, 0x49, 0x54, 0x33), // "VIZIT3"
            byteArrayOf(0x4B, 0x56, 0x31, 0x32, 0x33, 0x34), // "KV1234"
            byteArrayOf(0x4F, 0x4B, 0x31, 0x32, 0x33, 0x34), // "OK1234"

            // Claves Cyfral
            byteArrayOf(0x43, 0x79, 0x66, 0x72, 0x61, 0x6C), // "Cyfral"
            byteArrayOf(0x43, 0x59, 0x46, 0x52, 0x41, 0x4C), // "CYFRAL"
            byteArrayOf(0x63, 0x79, 0x66, 0x72, 0x61, 0x6C), // "cyfral"
            byteArrayOf(0x4C, 0x61, 0x72, 0x66, 0x79, 0x43), // "LarfyC"
            byteArrayOf(0x31, 0x32, 0x33, 0x34, 0x35, 0x36), // Códigos numéricos
            byteArrayOf(0x36, 0x35, 0x34, 0x33, 0x32, 0x31),

            // Claves Metakom/Metacom
            byteArrayOf(0x4D, 0x45, 0x54, 0x41, 0x4B, 0x4F), // "METAKO"
            byteArrayOf(0x4D, 0x45, 0x54, 0x41, 0x43, 0x4F), // "METACO"
            byteArrayOf(0x4D, 0x65, 0x74, 0x61, 0x6B, 0x6F), // "Metako"
            byteArrayOf(0x6D, 0x65, 0x74, 0x61, 0x6B, 0x6F), // "metako"

            // Claves BKS
            byteArrayOf(0x42, 0x4B, 0x53, 0x31, 0x32, 0x33), // "BKS123"
            byteArrayOf(0x42, 0x4B, 0x53, 0x30, 0x30, 0x31), // "BKS001"
            byteArrayOf(0x62, 0x6B, 0x73, 0x31, 0x32, 0x33), // "bks123"

            // Claves específicas de domófonos
            byteArrayOf(0x44, 0x4F, 0x4D, 0x4F, 0x46, 0x4F), // "DOMOFO"
            byteArrayOf(0x44, 0x4F, 0x4D, 0x4F, 0x46, 0x4E), // "DOMOFN"
            byteArrayOf(0x44, 0x4F, 0x4D, 0x4F, 0x46, 0x31), // "DOMOF1"
            byteArrayOf(0x48, 0x4F, 0x4D, 0x45, 0x31, 0x32), // "HOME12"
            byteArrayOf(0x45, 0x4E, 0x54, 0x52, 0x59, 0x31), // "ENTRY1"
            byteArrayOf(0x45, 0x4E, 0x54, 0x45, 0x52, 0x31), // "ENTER1"

            // Códigos de programadores rusos
            byteArrayOf(0x50, 0x52, 0x4F, 0x47, 0x31, 0x32), // "PROG12"
            byteArrayOf(0x54, 0x45, 0x53, 0x54, 0x31, 0x32), // "TEST12"
            byteArrayOf(0x44, 0x45, 0x42, 0x55, 0x47, 0x31), // "DEBUG1"
            byteArrayOf(0x44, 0x45, 0x46, 0x41, 0x55, 0x4C), // "DEFAUL"

            // Patrones numéricos comunes en Rusia
            byteArrayOf(0x31, 0x32, 0x33, 0x34, 0x35, 0x36), // "123456"
            byteArrayOf(0x36, 0x35, 0x34, 0x33, 0x32, 0x31), // "654321"
            byteArrayOf(0x31, 0x31, 0x31, 0x31, 0x31, 0x31), // "111111"
            byteArrayOf(0x32, 0x32, 0x32, 0x32, 0x32, 0x32), // "222222"
            byteArrayOf(0x30, 0x30, 0x30, 0x30, 0x30, 0x30), // "000000"
            byteArrayOf(0x39, 0x39, 0x39, 0x39, 0x39, 0x39), // "999999"

            // Claves HEX comunes en sistemas rusos
            byteArrayOf(0x48, 0x52, 0x4D, 0x43, 0x34, 0x30), // HRM домофоны
            byteArrayOf(0x45, 0x4C, 0x45, 0x43, 0x54, 0x52), // "ELECTR"
            byteArrayOf(0x54, 0x52, 0x4F, 0x4E, 0x49, 0x4B), // "TRONIK"
            byteArrayOf(0x4B, 0x45, 0x59, 0x44, 0x4F, 0x4D), // "KEYDOM"
            byteArrayOf(0x53, 0x59, 0x53, 0x54, 0x45, 0x4D), // "SYSTEM"

            // Códigos de fábrica rusos
            byteArrayOf(0x46, 0x41, 0x42, 0x52, 0x49, 0x4B), // "FABRIK"
            byteArrayOf(0x5A, 0x41, 0x56, 0x4F, 0x44, 0x31), // "ZAVOD1"
            byteArrayOf(0x52, 0x55, 0x53, 0x53, 0x49, 0x41), // "RUSSIA"
        )

        // Claves encontradas en bases de datos de seguridad
        private val knownBreachedKeys = listOf(
            byteArrayOf(0x8F.toByte(), 0xD0.toByte(), 0xA4.toByte(), 0xF2.toByte(), 0x56, 0xE9.toByte()),
            byteArrayOf(0x4D, 0x3A, 0x99.toByte(), 0xC3.toByte(), 0x51, 0xDD.toByte()),
            byteArrayOf(0x1A, 0x98.toByte(), 0x2C, 0x7E, 0x45, 0x9A.toByte()),
            byteArrayOf(0xD3.toByte(), 0xF7.toByte(), 0xD3.toByte(), 0xF7.toByte(), 0xD3.toByte(), 0xF7.toByte()),
            byteArrayOf(0x74, 0x5C, 0xB9.toByte(), 0x39, 0xDA.toByte(), 0x90.toByte()),
            byteArrayOf(0x68, 0xE1.toByte(), 0x97.toByte(), 0x5D, 0x47, 0x18),
            byteArrayOf(0x51, 0x28, 0x3C, 0x8B.toByte(), 0x92.toByte(), 0x4E),
            byteArrayOf(0x90.toByte(), 0x12, 0x74, 0x86.toByte(), 0x33, 0x1D),
            byteArrayOf(0x2A, 0xC1.toByte(), 0x85.toByte(), 0x39, 0x67, 0x4F),
            byteArrayOf(0xE3.toByte(), 0x91.toByte(), 0x28, 0x56, 0x7D, 0xA4.toByte()),
        )

        // Patrones de claves débiles
        private val weakPatterns = listOf(
            byteArrayOf(0x01, 0x02, 0x03, 0x04, 0x05, 0x06),
            byteArrayOf(0x0A, 0x0B, 0x0C, 0x0D, 0x0E, 0x0F),
            byteArrayOf(0x10, 0x11, 0x12, 0x13, 0x14, 0x15),
            byteArrayOf(0x20, 0x21, 0x22, 0x23, 0x24, 0x25),
            byteArrayOf(0xF0.toByte(), 0xF1.toByte(), 0xF2.toByte(), 0xF3.toByte(), 0xF4.toByte(), 0xF5.toByte()),
            byteArrayOf(0xA5.toByte(), 0xA5.toByte(), 0xA5.toByte(), 0xA5.toByte(), 0xA5.toByte(), 0xA5.toByte()),
            byteArrayOf(0x5A, 0x5A, 0x5A, 0x5A, 0x5A, 0x5A),
            byteArrayOf(0xCA.toByte(), 0xFE.toByte(), 0xBA.toByte(), 0xBE.toByte(), 0x00, 0x00),
            byteArrayOf(0xDE.toByte(), 0xAD.toByte(), 0xBE.toByte(), 0xEF.toByte(), 0x00, 0x00),
            byteArrayOf(0xFE.toByte(), 0xED.toByte(), 0xFA.toByte(), 0xCE.toByte(), 0x00, 0x00),
        )

        // Claves específicas de vendedores
        private val vendorSpecificKeys = listOf(
            // NXP/Philips defaults
            byteArrayOf(0x48, 0x45, 0x4C, 0x4C, 0x4F, 0x21), // "HELLO!"
            byteArrayOf(0x4E, 0x58, 0x50, 0x4B, 0x45, 0x59), // "NXPKEY"

            // Infineon defaults
            byteArrayOf(0x49, 0x4E, 0x46, 0x49, 0x4E, 0x45), // "INFINE"

            // ST Microelectronics
            byteArrayOf(0x53, 0x54, 0x4D, 0x49, 0x43, 0x52), // "STMICR"

            // Atmel defaults
            byteArrayOf(0x41, 0x54, 0x4D, 0x45, 0x4C, 0x31), // "ATMEL1"

            // Generic manufacturer test keys
            byteArrayOf(0x54, 0x45, 0x53, 0x54, 0x4B, 0x45), // "TESTKE"
            byteArrayOf(0x44, 0x45, 0x46, 0x41, 0x55, 0x4C), // "DEFAUL"
            byteArrayOf(0x46, 0x41, 0x43, 0x54, 0x4F, 0x52), // "FACTOR"
        )
    }

    fun getBasicKeys(): List<ByteArray> = defaultKeys

    fun getAllKeys(): List<ByteArray> =
        defaultKeys +
                transportKeys +
                accessKeys +
                russianDomophoneKeys +
                knownBreachedKeys +
                weakPatterns +
                vendorSpecificKeys +
                generateCommonVariations() +
                generateNumericalPatterns() +
                generateAlphabeticalPatterns()

    fun getRussianKeys(): List<ByteArray> = russianDomophoneKeys

    fun getTransportKeys(): List<ByteArray> = transportKeys

    fun getAccessKeys(): List<ByteArray> = accessKeys

    fun getBreachedKeys(): List<ByteArray> = knownBreachedKeys

    fun getVendorKeys(): List<ByteArray> = vendorSpecificKeys

    private fun generateCommonVariations(): List<ByteArray> {
        val variations = mutableListOf<ByteArray>()

        // Patrones de repetición
        for (i in 0x00..0xFF) {
            variations.add(byteArrayOf(i.toByte(), i.toByte(), i.toByte(), i.toByte(), i.toByte(), i.toByte()))
        }

        // Patrones XOR
        for (i in 0x00..0x0F) {
            val base = i.toByte()
            variations.add(byteArrayOf(base, (base.toInt() xor 0x01).toByte(), base, (base.toInt() xor 0x01).toByte(), base, (base.toInt() xor 0x01).toByte()))
        }

        // Patrones incrementales
        for (start in 0x00..0xFA step 10) {
            val pattern = ByteArray(6)
            for (j in 0 until 6) {
                pattern[j] = (start + j).toByte()
            }
            variations.add(pattern)
        }

        return variations
    }

    private fun generateNumericalPatterns(): List<ByteArray> {
        val patterns = mutableListOf<ByteArray>()

        // Patrones de fecha comunes (DDMMYY)
        val commonDates = listOf(
            "010101", "010203", "123456", "654321",
            "111111", "222222", "333333", "444444",
            "555555", "666666", "777777", "888888",
            "999999", "000000", "121212", "101010"
        )

        commonDates.forEach { date ->
            val bytes = date.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
            if (bytes.size == 6) {
                patterns.add(bytes)
            }
        }

        // Secuencias numéricas
        patterns.add(byteArrayOf(0x01, 0x02, 0x03, 0x04, 0x05, 0x06))
        patterns.add(byteArrayOf(0x06, 0x05, 0x04, 0x03, 0x02, 0x01))
        patterns.add(byteArrayOf(0x02, 0x04, 0x06, 0x08, 0x0A, 0x0C))
        patterns.add(byteArrayOf(0x01, 0x03, 0x05, 0x07, 0x09, 0x0B))

        return patterns
    }

    private fun generateAlphabeticalPatterns(): List<ByteArray> {
        val patterns = mutableListOf<ByteArray>()

        // Patrones alfabéticos comunes
        patterns.add("ABCDEF".toByteArray())
        patterns.add("FEDCBA".toByteArray())
        patterns.add("QWERTY".toByteArray())
        patterns.add("ASDFGH".toByteArray())
        patterns.add("ZXCVBN".toByteArray())
        patterns.add("ADMIN1".toByteArray())
        patterns.add("PASSWD".toByteArray())
        patterns.add("SECRET".toByteArray())
        patterns.add("MASTER".toByteArray())
        patterns.add("BACKUP".toByteArray())

        return patterns
    }

    // Función para generar claves basadas en UID
    fun generateUIDBasedKeys(uid: ByteArray): List<ByteArray> {
        val uidKeys = mutableListOf<ByteArray>()

        if (uid.size >= 4) {
            // Usar los primeros 4 bytes del UID
            val uidPrefix = uid.take(4).toByteArray()

            // Completar con patrones conocidos
            uidKeys.add(uidPrefix + byteArrayOf(0x00, 0x00))
            uidKeys.add(uidPrefix + byteArrayOf(0xFF.toByte(), 0xFF.toByte()))
            uidKeys.add(uidPrefix + byteArrayOf(0x12, 0x34))
            uidKeys.add(uidPrefix + byteArrayOf(0xAB.toByte(), 0xCD.toByte()))

            // Reverso del UID
            uidKeys.add(uidPrefix.reversedArray() + byteArrayOf(0x00, 0x00))

            // UID XOR con patrones
            val xorPatterns = listOf(0x00, 0xFF, 0xAA, 0x55, 0x12, 0x34)
            xorPatterns.forEach { pattern ->
                val xorKey = ByteArray(6)
                for (i in 0 until 4) {
                    xorKey[i] = (uidPrefix[i].toInt() xor pattern).toByte()
                }
                xorKey[4] = pattern.toByte()
                xorKey[5] = (pattern xor 0xFF).toByte()
                uidKeys.add(xorKey)
            }
        }

        return uidKeys
    }

    // Función para obtener estadísticas del diccionario
    fun getDictionaryStats(): Map<String, Int> {
        return mapOf(
            "default_keys" to defaultKeys.size,
            "transport_keys" to transportKeys.size,
            "access_keys" to accessKeys.size,
            "russian_keys" to russianDomophoneKeys.size,
            "breached_keys" to knownBreachedKeys.size,
            "weak_patterns" to weakPatterns.size,
            "vendor_keys" to vendorSpecificKeys.size,
            "total_keys" to getAllKeys().size
        )
    }

    // Función para validar si una clave es conocida
    fun isKnownKey(key: ByteArray): Pair<Boolean, String?> {
        val allKeysWithCategories = listOf(
            defaultKeys to "Default",
            transportKeys to "Transport",
            accessKeys to "Access",
            russianDomophoneKeys to "Russian Domophone",
            knownBreachedKeys to "Known Breached",
            weakPatterns to "Weak Pattern",
            vendorSpecificKeys to "Vendor Specific"
        )

        allKeysWithCategories.forEach { (keys, category) ->
            if (keys.any { it.contentEquals(key) }) {
                return Pair(true, category)
            }
        }

        return Pair(false, null)
    }
}
