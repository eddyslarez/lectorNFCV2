package com.eddyslarez.lectornfc
/**
 * Diccionario extendido con claves adicionales para sistemas específicos
 */
class KeyDictionaries2 {

    companion object {
        // Claves específicas de sistemas de parking
        private val parkingKeys = listOf(
            byteArrayOf(0x50, 0x41, 0x52, 0x4B, 0x49, 0x4E), // "PARKIN"
            byteArrayOf(0x50, 0x41, 0x52, 0x4B, 0x31, 0x32), // "PARK12"
            byteArrayOf(0x43, 0x41, 0x52, 0x50, 0x41, 0x52), // "CARPAR"
            byteArrayOf(0x47, 0x41, 0x52, 0x41, 0x47, 0x45), // "GARAGE"
        )

        // Claves de sistemas de lavandería
        private val laundryKeys = listOf(
            byteArrayOf(0x4C, 0x41, 0x55, 0x4E, 0x44, 0x52), // "LAUNDR"
            byteArrayOf(0x57, 0x41, 0x53, 0x48, 0x31, 0x32), // "WASH12"
            byteArrayOf(0x43, 0x4C, 0x45, 0x41, 0x4E, 0x31), // "CLEAN1"
        )

        // Claves de sistemas de vending
        private val vendingKeys = listOf(
            byteArrayOf(0x56, 0x45, 0x4E, 0x44, 0x49, 0x4E), // "VENDIN"
            byteArrayOf(0x43, 0x4F, 0x49, 0x4E, 0x31, 0x32), // "COIN12"
            byteArrayOf(0x4D, 0x41, 0x43, 0x48, 0x49, 0x4E), // "MACHIN"
        )

        // Claves de sistemas de gimnasio
        private val gymKeys = listOf(
            byteArrayOf(0x47, 0x59, 0x4D, 0x4B, 0x45, 0x59), // "GYMKEY"
            byteArrayOf(0x46, 0x49, 0x54, 0x4E, 0x45, 0x53), // "FITNES"
            byteArrayOf(0x53, 0x50, 0x4F, 0x52, 0x54, 0x31), // "SPORT1"
        )

        // Claves de sistemas de biblioteca
        private val libraryKeys = listOf(
            byteArrayOf(0x4C, 0x49, 0x42, 0x52, 0x41, 0x52), // "LIBRAR"
            byteArrayOf(0x42, 0x4F, 0x4F, 0x4B, 0x31, 0x32), // "BOOK12"
            byteArrayOf(0x52, 0x45, 0x41, 0x44, 0x45, 0x52), // "READER"
        )

        // Claves de sistemas universitarios
        private val universityKeys = listOf(
            byteArrayOf(0x55, 0x4E, 0x49, 0x56, 0x45, 0x52), // "UNIVER"
            byteArrayOf(0x53, 0x54, 0x55, 0x44, 0x45, 0x4E), // "STUDEN"
            byteArrayOf(0x43, 0x41, 0x4D, 0x50, 0x55, 0x53), // "CAMPUS"
            byteArrayOf(0x45, 0x44, 0x55, 0x43, 0x41, 0x54), // "EDUCAT"
        )

        // Claves de sistemas de oficina
        private val officeKeys = listOf(
            byteArrayOf(0x4F, 0x46, 0x46, 0x49, 0x43, 0x45), // "OFFICE"
            byteArrayOf(0x57, 0x4F, 0x52, 0x4B, 0x31, 0x32), // "WORK12"
            byteArrayOf(0x42, 0x55, 0x49, 0x4C, 0x44, 0x49), // "BUILDI"
            byteArrayOf(0x43, 0x4F, 0x4D, 0x50, 0x41, 0x4E), // "COMPAN"
        )

        // Claves de sistemas médicos
        private val medicalKeys = listOf(
            byteArrayOf(0x4D, 0x45, 0x44, 0x49, 0x43, 0x41), // "MEDICA"
            byteArrayOf(0x48, 0x4F, 0x53, 0x50, 0x49, 0x54), // "HOSPIT"
            byteArrayOf(0x44, 0x4F, 0x43, 0x54, 0x4F, 0x52), // "DOCTOR"
            byteArrayOf(0x4E, 0x55, 0x52, 0x53, 0x45, 0x31), // "NURSE1"
        )

        // Claves de sistemas de seguridad
        private val securityKeys = listOf(
            byteArrayOf(0x53, 0x45, 0x43, 0x55, 0x52, 0x49), // "SECURI"
            byteArrayOf(0x47, 0x55, 0x41, 0x52, 0x44, 0x31), // "GUARD1"
            byteArrayOf(0x41, 0x4C, 0x41, 0x52, 0x4D, 0x31), // "ALARM1"
            byteArrayOf(0x50, 0x52, 0x4F, 0x54, 0x45, 0x43), // "PROTEC"
        )

        // Claves de sistemas de manufactura
        private val manufacturingKeys = listOf(
            byteArrayOf(0x46, 0x41, 0x43, 0x54, 0x4F, 0x52), // "FACTOR"
            byteArrayOf(0x50, 0x52, 0x4F, 0x44, 0x55, 0x43), // "PRODUC"
            byteArrayOf(0x4D, 0x41, 0x4E, 0x55, 0x46, 0x41), // "MANUFA"
            byteArrayOf(0x49, 0x4E, 0x44, 0x55, 0x53, 0x54), // "INDUST"
        )
    }

    fun getAllExtendedKeys(): List<ByteArray> {
        return parkingKeys + laundryKeys + vendingKeys + gymKeys +
                libraryKeys + universityKeys + officeKeys + medicalKeys +
                securityKeys + manufacturingKeys
    }

    fun getKeysByCategory(category: String): List<ByteArray> {
        return when (category.lowercase()) {
            "parking" -> parkingKeys
            "laundry" -> laundryKeys
            "vending" -> vendingKeys
            "gym" -> gymKeys
            "library" -> libraryKeys
            "university" -> universityKeys
            "office" -> officeKeys
            "medical" -> medicalKeys
            "security" -> securityKeys
            "manufacturing" -> manufacturingKeys
            else -> emptyList()
        }
    }

    fun getExtendedStats(): Map<String, Int> {
        return mapOf(
            "parking_keys" to parkingKeys.size,
            "laundry_keys" to laundryKeys.size,
            "vending_keys" to vendingKeys.size,
            "gym_keys" to gymKeys.size,
            "library_keys" to libraryKeys.size,
            "university_keys" to universityKeys.size,
            "office_keys" to officeKeys.size,
            "medical_keys" to medicalKeys.size,
            "security_keys" to securityKeys.size,
            "manufacturing_keys" to manufacturingKeys.size,
            "total_extended_keys" to getAllExtendedKeys().size
        )
    }
}