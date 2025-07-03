package com.eddyslarez.lectornfc.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File

class ShareManager(private val context: Context) {

    fun shareFile(filePath: String, shareMethod: String) {
        val file = File(filePath)
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        when (shareMethod.lowercase()) {
            "email" -> shareViaEmail(uri, file.name)
            "whatsapp" -> shareViaWhatsApp(uri)
            "telegram" -> shareViaTelegram(uri)
            else -> shareViaGeneric(uri, file.name)
        }
    }

    private fun shareViaEmail(uri: Uri, fileName: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_EMAIL, arrayOf(""))
            putExtra(Intent.EXTRA_SUBJECT, "Mifare Scan Report - $fileName")
            putExtra(Intent.EXTRA_TEXT, "Adjunto el reporte de escaneo Mifare generado por Advanced Mifare Pro.")
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        context.startActivity(Intent.createChooser(intent, "Enviar por email"))
    }

    private fun shareViaWhatsApp(uri: Uri) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            setPackage("com.whatsapp")
            putExtra(Intent.EXTRA_TEXT, "Reporte de escaneo Mifare")
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            shareViaGeneric(uri, "Reporte Mifare")
        }
    }

    private fun shareViaTelegram(uri: Uri) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            setPackage("org.telegram.messenger")
            putExtra(Intent.EXTRA_TEXT, "Reporte de escaneo Mifare")
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            shareViaGeneric(uri, "Reporte Mifare")
        }
    }

    private fun shareViaGeneric(uri: Uri, fileName: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, "Reporte de escaneo Mifare - $fileName")
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        context.startActivity(Intent.createChooser(intent, "Compartir reporte"))
    }
}

//import android.content.Context
//import android.content.Intent
//import android.net.Uri
//import androidx.core.content.FileProvider
//import com.eddyslarez.lectornfc.data.database.entities.ScanSession
//import com.eddyslarez.lectornfc.data.database.entities.FoundKey
//import com.eddyslarez.lectornfc.data.models.BlockData
//import kotlinx.coroutines.Dispatchers
//import kotlinx.coroutines.withContext
//import java.io.File
//import java.io.FileWriter
//
//class ShareManager(private val context: Context) {
//
//    suspend fun shareViaEmail(
//        session: ScanSession,
//        foundKeys: List<FoundKey>,
//        blockData: List<BlockData>,
//        recipientEmail: String = ""
//    ): Intent? = withContext(Dispatchers.IO) {
//        try {
//            val exportManager = ExportManager(context)
//            val content = exportManager.generateTextReport(session, foundKeys, blockData)
//
//            val subject = "Advanced Mifare Pro - Scan Report (${session.cardUid})"
//            val body = "Adjunto encontrarás el reporte de escaneo de la tarjeta Mifare.\n\n" +
//                    "Detalles del escaneo:\n" +
//                    "- UID: ${session.cardUid}\n" +
//                    "- Método: ${session.attackMethod}\n" +
//                    "- Sectores crackeados: ${session.crackedSectors}/${session.totalSectors}\n" +
//                    "- Claves encontradas: ${foundKeys.size}\n\n" +
//                    "Generado por Advanced Mifare Pro"
//
//            // Create temporary file
//            val file = createTempFile("scan_report", ".txt", content)
//            val uri = FileProvider.getUriForFile(
//                context,
//                "${context.packageName}.fileprovider",
//                file
//            )
//
//            val intent = Intent(Intent.ACTION_SEND).apply {
//                type = "text/plain"
//                putExtra(Intent.EXTRA_EMAIL, arrayOf(recipientEmail))
//                putExtra(Intent.EXTRA_SUBJECT, subject)
//                putExtra(Intent.EXTRA_TEXT, body)
//                putExtra(Intent.EXTRA_STREAM, uri)
//                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
//            }
//
//            Intent.createChooser(intent, "Enviar reporte por email")
//        } catch (e: Exception) {
//            null
//        }
//    }
//
//    suspend fun shareViaWhatsApp(
//        session: ScanSession,
//        foundKeys: List<FoundKey>,
//        blockData: List<BlockData>
//    ): Intent? = withContext(Dispatchers.IO) {
//        try {
//            val summary = generateSummaryText(session, foundKeys, blockData)
//
//            val intent = Intent(Intent.ACTION_SEND).apply {
//                type = "text/plain"
//                setPackage("com.whatsapp")
//                putExtra(Intent.EXTRA_TEXT, summary)
//            }
//
//            intent
//        } catch (e: Exception) {
//            null
//        }
//    }
//
//    suspend fun shareViaTelegram(
//        session: ScanSession,
//        foundKeys: List<FoundKey>,
//        blockData: List<BlockData>
//    ): Intent? = withContext(Dispatchers.IO) {
//        try {
//            val summary = generateSummaryText(session, foundKeys, blockData)
//
//            val intent = Intent(Intent.ACTION_SEND).apply {
//                type = "text/plain"
//                setPackage("org.telegram.messenger")
//                putExtra(Intent.EXTRA_TEXT, summary)
//            }
//
//            intent
//        } catch (e: Exception) {
//            null
//        }
//    }
//
//    suspend fun shareAsFile(
//        session: ScanSession,
//        foundKeys: List<FoundKey>,
//        blockData: List<BlockData>,
//        format: ExportFormat = ExportFormat.TXT
//    ): Intent? = withContext(Dispatchers.IO) {
//        try {
//            val exportManager = ExportManager(context)
//            val content = when (format) {
//                ExportFormat.TXT -> exportManager.generateTextReport(session, foundKeys, blockData)
//                ExportFormat.JSON -> TODO("Implement JSON export")
//                ExportFormat.CSV -> TODO("Implement CSV export")
//                ExportFormat.XML -> TODO("Implement XML export")
//            }
//
//            val extension = when (format) {
//                ExportFormat.TXT -> ".txt"
//                ExportFormat.JSON -> ".json"
//                ExportFormat.CSV -> ".csv"
//                ExportFormat.XML -> ".xml"
//            }
//
//            val fileName = "mifare_scan_${session.cardUid}_${System.currentTimeMillis()}"
//            val file = createTempFile(fileName, extension, content)
//
//            val uri = FileProvider.getUriForFile(
//                context,
//                "${context.packageName}.fileprovider",
//                file
//            )
//
//            val mimeType = when (format) {
//                ExportFormat.TXT -> "text/plain"
//                ExportFormat.JSON -> "application/json"
//                ExportFormat.CSV -> "text/csv"
//                ExportFormat.XML -> "application/xml"
//            }
//
//            val intent = Intent(Intent.ACTION_SEND).apply {
//                type = mimeType
//                putExtra(Intent.EXTRA_STREAM, uri)
//                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
//            }
//
//            Intent.createChooser(intent, "Compartir archivo")
//        } catch (e: Exception) {
//            null
//        }
//    }
//
//    private fun generateSummaryText(
//        session: ScanSession,
//        foundKeys: List<FoundKey>,
//        blockData: List<BlockData>
//    ): String {
//        val sb = StringBuilder()
//
//        sb.appendLine("🔐 *Advanced Mifare Pro - Reporte*")
//        sb.appendLine()
//        sb.appendLine("📊 *Resumen del Escaneo:*")
//        sb.appendLine("• UID: `${session.cardUid}`")
//        sb.appendLine("• Tipo: ${session.cardType}")
//        sb.appendLine("• Método: ${session.attackMethod}")
//        sb.appendLine("• Sectores crackeados: ${session.crackedSectors}/${session.totalSectors}")
//        sb.appendLine("• Éxito: ${if (session.crackedSectors > 0) "✅" else "❌"}")
//
//        if (foundKeys.isNotEmpty()) {
//            sb.appendLine()
//            sb.appendLine("🔑 *Claves encontradas:*")
//            foundKeys.take(5).forEach { key ->
//                sb.appendLine("• Sector ${key.sector}: ${key.keyA ?: key.keyB} (${key.discoveryMethod})")
//            }
//            if (foundKeys.size > 5) {
//                sb.appendLine("• ... y ${foundKeys.size - 5} más")
//            }
//        }
//
//        val readableBlocks = blockData.count { it.cracked }
//        if (readableBlocks > 0) {
//            sb.appendLine()
//            sb.appendLine("📁 *Datos:*")
//            sb.appendLine("• Bloques leídos: $readableBlocks/${blockData.size}")
//        }
//
//        sb.appendLine()
//        sb.appendLine("🕐 Generado: ${java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault()).format(java.util.Date())}")
//
//        return sb.toString()
//    }
//
//    private suspend fun createTempFile(name: String, extension: String, content: String): File = withContext(Dispatchers.IO) {
//        val file = File(context.cacheDir, "$name$extension")
//        FileWriter(file).use { writer ->
//            writer.write(content)
//        }
//        file
//    }
//
//    enum class ExportFormat {
//        TXT, JSON, CSV, XML
//    }
//}
