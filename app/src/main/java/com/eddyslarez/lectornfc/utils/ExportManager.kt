package com.eddyslarez.lectornfc.utils

import android.content.Context
import android.os.Environment
import com.eddyslarez.lectornfc.data.models.BlockData
import com.eddyslarez.lectornfc.data.models.KeyPair
import com.eddyslarez.lectornfc.presentation.viewmodel.ExportFormat
import com.google.gson.GsonBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class ExportManager(private val context: Context) {

    suspend fun exportData(
        cardData: List<BlockData>,
        foundKeys: Map<Int, KeyPair>,
        format: ExportFormat
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val fileName = "mifare_scan_$timestamp.${format.name.lowercase()}"

            val content = when (format) {
                ExportFormat.TXT -> generateTxtContent(cardData, foundKeys)
                ExportFormat.JSON -> generateJsonContent(cardData, foundKeys)
                ExportFormat.CSV -> generateCsvContent(cardData, foundKeys)
                ExportFormat.XML -> generateXmlContent(cardData, foundKeys)
            }

            val file = saveToFile(fileName, content)
            Result.success(file.absolutePath)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun generateTxtContent(cardData: List<BlockData>, foundKeys: Map<Int, KeyPair>): String {
        val sb = StringBuilder()
        val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())

        sb.appendLine("=== ADVANCED MIFARE PRO - SCAN REPORT ===")
        sb.appendLine("Fecha: $timestamp")
        sb.appendLine("Total de bloques: ${cardData.size}")
        sb.appendLine("Bloques leídos: ${cardData.count { it.cracked }}")
        sb.appendLine("Claves encontradas: ${foundKeys.size}")
        sb.appendLine()

        // Claves encontradas
        if (foundKeys.isNotEmpty()) {
            sb.appendLine("=== CLAVES ENCONTRADAS ===")
            foundKeys.toSortedMap().forEach { (sector, keyPair) ->
                sb.appendLine("Sector $sector:")
                keyPair.keyA?.let { key ->
                    sb.appendLine("  Key A: ${key.joinToString(" ") { "%02X".format(it) }}")
                }
                keyPair.keyB?.let { key ->
                    sb.appendLine("  Key B: ${key.joinToString(" ") { "%02X".format(it) }}")
                }
            }
            sb.appendLine()
        }

        // Datos por sector
        sb.appendLine("=== DATOS DE LA TARJETA ===")
        cardData.groupBy { it.sector }.toSortedMap().forEach { (sector, blocks) ->
            val isCracked = blocks.any { it.cracked }
            sb.appendLine("Sector $sector ${if (isCracked) "[CRACKEADO]" else "[BLOQUEADO]"}:")

            blocks.forEach { block ->
                val status = when {
                    block.cracked -> "✓"
                    block.error != null -> "✗"
                    else -> "?"
                }
                sb.appendLine("  Bloque ${block.block}: $status ${block.dataAsHex()}")

                if (block.keyUsed != null) {
                    sb.appendLine("    Clave usada (${block.keyType}): ${block.keyUsed.joinToString(" ") { "%02X".format(it) }}")
                }
            }
            sb.appendLine()
        }

        return sb.toString()
    }

    private fun generateJsonContent(cardData: List<BlockData>, foundKeys: Map<Int, KeyPair>): String {
        val gson = GsonBuilder().setPrettyPrinting().create()

        val exportData = mapOf(
            "timestamp" to SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault()).format(Date()),
            "version" to "3.0.0",
            "statistics" to mapOf(
                "total_blocks" to cardData.size,
                "readable_blocks" to cardData.count { it.cracked },
                "total_sectors" to cardData.map { it.sector }.distinct().size,
                "cracked_sectors" to foundKeys.size,
                "found_keys" to foundKeys.size
            ),
            "found_keys" to foundKeys.map { (sector, keyPair) ->
                mapOf(
                    "sector" to sector,
                    "key_a" to keyPair.keyA?.joinToString("") { "%02X".format(it) },
                    "key_b" to keyPair.keyB?.joinToString("") { "%02X".format(it) }
                )
            },
            "card_data" to cardData.map { block ->
                mapOf(
                    "sector" to block.sector,
                    "block" to block.block,
                    "data" to block.dataAsHex(),
                    "is_trailer" to block.isTrailer,
                    "cracked" to block.cracked,
                    "key_type" to block.keyType,
                    "key_used" to block.keyUsed?.joinToString("") { "%02X".format(it) },
                    "error" to block.error
                )
            }
        )

        return gson.toJson(exportData)
    }

    private fun generateCsvContent(cardData: List<BlockData>, foundKeys: Map<Int, KeyPair>): String {
        val sb = StringBuilder()

        // Header
        sb.appendLine("Sector,Block,Data,IsTrailer,Cracked,KeyType,KeyUsed,Error")

        // Data rows
        cardData.forEach { block ->
            sb.appendLine("${block.sector},${block.block},\"${block.dataAsHex()}\",${block.isTrailer},${block.cracked},${block.keyType},\"${block.keyUsed?.joinToString("") { "%02X".format(it) } ?: ""}\",\"${block.error ?: ""}\"")
        }

        return sb.toString()
    }

    private fun generateXmlContent(cardData: List<BlockData>, foundKeys: Map<Int, KeyPair>): String {
        val sb = StringBuilder()
        val timestamp = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault()).format(Date())

        sb.appendLine("<?xml version=\"1.0\" encoding=\"UTF-8\"?>")
        sb.appendLine("<mifare_scan timestamp=\"$timestamp\" version=\"3.0.0\">")

        // Statistics
        sb.appendLine("  <statistics>")
        sb.appendLine("    <total_blocks>${cardData.size}</total_blocks>")
        sb.appendLine("    <readable_blocks>${cardData.count { it.cracked }}</readable_blocks>")
        sb.appendLine("    <total_sectors>${cardData.map { it.sector }.distinct().size}</total_sectors>")
        sb.appendLine("    <cracked_sectors>${foundKeys.size}</cracked_sectors>")
        sb.appendLine("    <found_keys>${foundKeys.size}</found_keys>")
        sb.appendLine("  </statistics>")

        // Found keys
        if (foundKeys.isNotEmpty()) {
            sb.appendLine("  <found_keys>")
            foundKeys.forEach { (sector, keyPair) ->
                sb.appendLine("    <key sector=\"$sector\">")
                keyPair.keyA?.let { key ->
                    sb.appendLine("      <key_a>${key.joinToString("") { "%02X".format(it) }}</key_a>")
                }
                keyPair.keyB?.let { key ->
                    sb.appendLine("      <key_b>${key.joinToString("") { "%02X".format(it) }}</key_b>")
                }
                sb.appendLine("    </key>")
            }
            sb.appendLine("  </found_keys>")
        }

        // Card data
        sb.appendLine("  <card_data>")
        cardData.forEach { block ->
            sb.appendLine("    <block sector=\"${block.sector}\" number=\"${block.block}\" is_trailer=\"${block.isTrailer}\" cracked=\"${block.cracked}\">")
            sb.appendLine("      <data>${block.dataAsHex()}</data>")
            if (block.keyUsed != null) {
                sb.appendLine("      <key_used type=\"${block.keyType}\">${block.keyUsed.joinToString("") { "%02X".format(it) }}</key_used>")
            }
            if (block.error != null) {
                sb.appendLine("      <error>${block.error}</error>")
            }
            sb.appendLine("    </block>")
        }
        sb.appendLine("  </card_data>")

        sb.appendLine("</mifare_scan>")

        return sb.toString()
    }

    private fun saveToFile(fileName: String, content: String): File {
        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val mifareDir = File(downloadsDir, "MifarePro")

        if (!mifareDir.exists()) {
            mifareDir.mkdirs()
        }

        val file = File(mifareDir, fileName)
        file.writeText(content)

        return file
    }
}

//import android.content.Context
//import android.net.Uri
//import com.eddyslarez.lectornfc.data.database.entities.ScanSession
//import com.eddyslarez.lectornfc.data.database.entities.FoundKey
//import com.eddyslarez.lectornfc.data.models.BlockData
//import com.google.gson.Gson
//import com.google.gson.GsonBuilder
//import kotlinx.coroutines.Dispatchers
//import kotlinx.coroutines.withContext
//import java.io.FileOutputStream
//import java.text.SimpleDateFormat
//import java.util.*
//
//class ExportManager(private val context: Context) {
//
//    companion object {
//        private const val MIME_TYPE_JSON = "application/json"
//        private const val MIME_TYPE_TXT = "text/plain"
//        private const val MIME_TYPE_CSV = "text/csv"
//        private const val MIME_TYPE_XML = "application/xml"
//    }
//
//    private val gson: Gson = GsonBuilder()
//        .setPrettyPrinting()
//        .setDateFormat("yyyy-MM-dd HH:mm:ss")
//        .create()
//
//    suspend fun exportToJson(
//        session: ScanSession,
//        foundKeys: List<FoundKey>,
//        blockData: List<BlockData>,
//        uri: Uri
//    ): Boolean = withContext(Dispatchers.IO) {
//        try {
//            val exportData = ExportData(
//                session = session,
//                foundKeys = foundKeys,
//                blockData = blockData,
//                exportTimestamp = System.currentTimeMillis(),
//                appVersion = getAppVersion()
//            )
//
//            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
//                outputStream.write(gson.toJson(exportData).toByteArray())
//            }
//            true
//        } catch (e: Exception) {
//            false
//        }
//    }
//
//    suspend fun exportToTxt(
//        session: ScanSession,
//        foundKeys: List<FoundKey>,
//        blockData: List<BlockData>,
//        uri: Uri
//    ): Boolean = withContext(Dispatchers.IO) {
//        try {
//            val content = generateTextReport(session, foundKeys, blockData)
//
//            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
//                outputStream.write(content.toByteArray())
//            }
//            true
//        } catch (e: Exception) {
//            false
//        }
//    }
//
//    suspend fun exportToCsv(
//        session: ScanSession,
//        foundKeys: List<FoundKey>,
//        blockData: List<BlockData>,
//        uri: Uri
//    ): Boolean = withContext(Dispatchers.IO) {
//        try {
//            val content = generateCsvReport(session, foundKeys, blockData)
//
//            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
//                outputStream.write(content.toByteArray())
//            }
//            true
//        } catch (e: Exception) {
//            false
//        }
//    }
//
//    suspend fun exportToXml(
//        session: ScanSession,
//        foundKeys: List<FoundKey>,
//        blockData: List<BlockData>,
//        uri: Uri
//    ): Boolean = withContext(Dispatchers.IO) {
//        try {
//            val content = generateXmlReport(session, foundKeys, blockData)
//
//            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
//                outputStream.write(content.toByteArray())
//            }
//            true
//        } catch (e: Exception) {
//            false
//        }
//    }
//
//    fun generateTextReport(
//        session: ScanSession,
//        foundKeys: List<FoundKey>,
//        blockData: List<BlockData>
//    ): String {
//        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
//        val sb = StringBuilder()
//
//        sb.appendLine("=".repeat(80))
//        sb.appendLine("ADVANCED MIFARE PRO - SCAN REPORT")
//        sb.appendLine("=".repeat(80))
//        sb.appendLine()
//
//        // Session Information
//        sb.appendLine("SESSION INFORMATION:")
//        sb.appendLine("Session ID: ${session.sessionId}")
//        sb.appendLine("Card UID: ${session.cardUid}")
//        sb.appendLine("Card Type: ${session.cardType}")
//        sb.appendLine("Start Time: ${dateFormat.format(session.startTime)}")
//        session.endTime?.let {
//            sb.appendLine("End Time: ${dateFormat.format(it)}")
//            val duration = (it.time - session.startTime.time) / 1000
//            sb.appendLine("Duration: ${duration}s")
//        }
//        sb.appendLine("Attack Method: ${session.attackMethod}")
//        sb.appendLine("Total Sectors: ${session.totalSectors}")
//        sb.appendLine("Cracked Sectors: ${session.crackedSectors}")
//        sb.appendLine("Success Rate: ${if (session.totalSectors > 0) (session.crackedSectors * 100 / session.totalSectors) else 0}%")
//        if (session.notes.isNotEmpty()) {
//            sb.appendLine("Notes: ${session.notes}")
//        }
//        sb.appendLine()
//
//        // Found Keys
//        if (foundKeys.isNotEmpty()) {
//            sb.appendLine("FOUND KEYS:")
//            sb.appendLine("-".repeat(60))
//            foundKeys.groupBy { it.sector }.forEach { (sector, keys) ->
//                sb.appendLine("Sector $sector:")
//                keys.forEach { key ->
//                    key.keyA?.let { sb.appendLine("  Key A: $it (${key.discoveryMethod}, confidence: ${key.confidence})") }
//                    key.keyB?.let { sb.appendLine("  Key B: $it (${key.discoveryMethod}, confidence: ${key.confidence})") }
//                }
//                sb.appendLine()
//            }
//        }
//
//        // Block Data
//        if (blockData.isNotEmpty()) {
//            sb.appendLine("BLOCK DATA:")
//            sb.appendLine("-".repeat(60))
//            blockData.groupBy { it.sector }.forEach { (sector, blocks) ->
//                sb.appendLine("Sector $sector:")
//                blocks.forEach { block ->
//                    val status = if (block.cracked) "✓" else "✗"
//                    val type = if (block.isTrailer) " (Trailer)" else ""
//                    sb.appendLine("  Block ${block.block}$type [$status]: ${block.dataAsHex()}")
//                    if (block.keyUsed != null) {
//                        val keyHex = block.keyUsed.joinToString("") { "%02X".format(it) }
//                        sb.appendLine("    Used Key ${block.keyType}: $keyHex")
//                    }
//                    if (block.error != null) {
//                        sb.appendLine("    Error: ${block.error}")
//                    }
//                }
//                sb.appendLine()
//            }
//        }
//
//        // Footer
//        sb.appendLine("=".repeat(80))
//        sb.appendLine("Generated by Advanced Mifare Pro v${getAppVersion()}")
//        sb.appendLine("Export Time: ${dateFormat.format(Date())}")
//        sb.appendLine("=".repeat(80))
//
//        return sb.toString()
//    }
//
//    private fun generateCsvReport(
//        session: ScanSession,
//        foundKeys: List<FoundKey>,
//        blockData: List<BlockData>
//    ): String {
//        val sb = StringBuilder()
//
//        // CSV Header
//        sb.appendLine("Type,Sector,Block,Data,Key_A,Key_B,Key_Type,Discovery_Method,Confidence,Error,Timestamp")
//
//        // Session info row
//        sb.appendLine("SESSION,${session.sessionId},${session.cardUid},${session.cardType},,,,,,,${session.startTime.time}")
//
//        // Found keys
//        foundKeys.forEach { key ->
//            sb.appendLine("KEY,${key.sector},,\"${key.keyA ?: ""}\",\"${key.keyB ?: ""}\",${key.keyType},${key.discoveryMethod},${key.confidence},,${key.timestamp}")
//        }
//
//        // Block data
//        blockData.forEach { block ->
//            val keyA = if (block.keyUsed != null && block.keyType == "A")
//                block.keyUsed.joinToString("") { "%02X".format(it) } else ""
//            val keyB = if (block.keyUsed != null && block.keyType == "B")
//                block.keyUsed.joinToString("") { "%02X".format(it) } else ""
//
//            sb.appendLine("BLOCK,${block.sector},${block.block},\"${block.dataAsHex()}\",\"$keyA\",\"$keyB\",${block.keyType},,,,")
//        }
//
//        return sb.toString()
//    }
//
//    private fun generateXmlReport(
//        session: ScanSession,
//        foundKeys: List<FoundKey>,
//        blockData: List<BlockData>
//    ): String {
//        val sb = StringBuilder()
//
//        sb.appendLine("<?xml version=\"1.0\" encoding=\"UTF-8\"?>")
//        sb.appendLine("<mifare_scan_report>")
//        sb.appendLine("  <metadata>")
//        sb.appendLine("    <app_version>${getAppVersion()}</app_version>")
//        sb.appendLine("    <export_timestamp>${System.currentTimeMillis()}</export_timestamp>")
//        sb.appendLine("  </metadata>")
//
//        // Session
//        sb.appendLine("  <session>")
//        sb.appendLine("    <session_id>${session.sessionId}</session_id>")
//        sb.appendLine("    <card_uid>${session.cardUid}</card_uid>")
//        sb.appendLine("    <card_type>${session.cardType}</card_type>")
//        sb.appendLine("    <start_time>${session.startTime.time}</start_time>")
//        session.endTime?.let {
//            sb.appendLine("    <end_time>${it.time}</end_time>")
//        }
//        sb.appendLine("    <attack_method>${session.attackMethod}</attack_method>")
//        sb.appendLine("    <total_sectors>${session.totalSectors}</total_sectors>")
//        sb.appendLine("    <cracked_sectors>${session.crackedSectors}</cracked_sectors>")
//        if (session.notes.isNotEmpty()) {
//            sb.appendLine("    <notes><![CDATA[${session.notes}]]></notes>")
//        }
//        sb.appendLine("  </session>")
//
//        // Found Keys
//        if (foundKeys.isNotEmpty()) {
//            sb.appendLine("  <found_keys>")
//            foundKeys.forEach { key ->
//                sb.appendLine("    <key>")
//                sb.appendLine("      <sector>${key.sector}</sector>")
//                key.keyA?.let { sb.appendLine("      <key_a>$it</key_a>") }
//                key.keyB?.let { sb.appendLine("      <key_b>$it</key_b>") }
//                sb.appendLine("      <key_type>${key.keyType}</key_type>")
//                sb.appendLine("      <discovery_method>${key.discoveryMethod}</discovery_method>")
//                sb.appendLine("      <confidence>${key.confidence}</confidence>")
//                sb.appendLine("      <timestamp>${key.timestamp}</timestamp>")
//                sb.appendLine("    </key>")
//            }
//            sb.appendLine("  </found_keys>")
//        }
//
//        // Block Data
//        if (blockData.isNotEmpty()) {
//            sb.appendLine("  <block_data>")
//            blockData.forEach { block ->
//                sb.appendLine("    <block>")
//                sb.appendLine("      <sector>${block.sector}</sector>")
//                sb.appendLine("      <block_number>${block.block}</block_number>")
//                sb.appendLine("      <is_trailer>${block.isTrailer}</is_trailer>")
//                sb.appendLine("      <data><![CDATA[${block.dataAsHex()}]]></data>")
//                sb.appendLine("      <cracked>${block.cracked}</cracked>")
//                if (block.keyUsed != null) {
//                    val keyHex = block.keyUsed.joinToString("") { "%02X".format(it) }
//                    sb.appendLine("      <used_key type=\"${block.keyType}\">$keyHex</used_key>")
//                }
//                if (block.error != null) {
//                    sb.appendLine("      <error><![CDATA[${block.error}]]></error>")
//                }
//                sb.appendLine("    </block>")
//            }
//            sb.appendLine("  </block_data>")
//        }
//
//        sb.appendLine("</mifare_scan_report>")
//        return sb.toString()
//    }
//
//    private fun getAppVersion(): String {
//        return try {
//            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
//            packageInfo.versionName ?: "Unknown"
//        } catch (e: Exception) {
//            "Unknown"
//        }
//    }
//
//    data class ExportData(
//        val session: ScanSession,
//        val foundKeys: List<FoundKey>,
//        val blockData: List<BlockData>,
//        val exportTimestamp: Long,
//        val appVersion: String
//    )
//}
