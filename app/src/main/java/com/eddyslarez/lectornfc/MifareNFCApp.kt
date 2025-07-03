package com.eddyslarez.lectornfc

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
//
//@Composable
//fun MifareNFCApp() {
//    val scrollState = rememberScrollState() // <- agrega esto
//
//    val mode by AdvancedMifareManager.mode
//    val isReading by AdvancedMifareManager.isReading
//    val isWriting by AdvancedMifareManager.isWriting
//    val status by AdvancedMifareManager.status
//    val progress by AdvancedMifareManager.progress
//    val currentSector by AdvancedMifareManager.currentSector
//    val totalSectors by AdvancedMifareManager.totalSectors
//    val attackMethod by AdvancedMifareManager.attackMethod
//    val cardData by AdvancedMifareManager.cardData
//    val foundKeys by AdvancedMifareManager.foundKeys
//    val crackedSectors by AdvancedMifareManager.crackedSectors
//
//    Column(
//        modifier = Modifier
//            .fillMaxSize()
//            .verticalScroll(scrollState) // <- habilita el scroll
//            .padding(16.dp),
//        verticalArrangement = Arrangement.spacedBy(16.dp)
//    ) {
//        // Header
//        Card(
//            modifier = Modifier.fillMaxWidth(),
//            colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A))
//        ) {
//            Column(
//                modifier = Modifier.padding(16.dp),
//                horizontalAlignment = Alignment.CenterHorizontally
//            ) {
//                Text(
//                    text = "🔐 Advanced Mifare Pro",
//                    fontSize = 24.sp,
//                    fontWeight = FontWeight.Bold,
//                    color = Color.White
//                )
//                Text(
//                    text = "Professional NFC Security Tool",
//                    fontSize = 14.sp,
//                    color = Color.Gray
//                )
//            }
//        }
//
//        // Mode Selection
//        Card(
//            modifier = Modifier.fillMaxWidth(),
//            colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2A2A))
//        ) {
//            Column(modifier = Modifier.padding(16.dp)) {
//                Text(
//                    text = "🎯 Modo de Operación",
//                    fontSize = 18.sp,
//                    fontWeight = FontWeight.Bold,
//                    color = Color.White,
//                    modifier = Modifier.padding(bottom = 8.dp)
//                )
//
//                Row(
//                    modifier = Modifier.fillMaxWidth(),
//                    horizontalArrangement = Arrangement.spacedBy(8.dp)
//                ) {
//                    Button(
//                        onClick = { AdvancedMifareManager.setMode(OperationMode.READ) },
//                        colors = ButtonDefaults.buttonColors(
//                            containerColor = if (mode == OperationMode.READ) Color(0xFF4CAF50) else Color(0xFF616161)
//                        ),
//                        modifier = Modifier.weight(1f)
//                    ) {
//                        Text("📖 LEER")
//                    }
//
//                    Button(
//                        onClick = { AdvancedMifareManager.setMode(OperationMode.WRITE) },
//                        colors = ButtonDefaults.buttonColors(
//                            containerColor = if (mode == OperationMode.WRITE) Color(0xFF2196F3) else Color(0xFF616161)
//                        ),
//                        modifier = Modifier.weight(1f)
//                    ) {
//                        Text("✏️ ESCRIBIR")
//                    }
//
//                    Button(
//                        onClick = { AdvancedMifareManager.setMode(OperationMode.CRACK) },
//                        colors = ButtonDefaults.buttonColors(
//                            containerColor = if (mode == OperationMode.CRACK) Color(0xFFFF5722) else Color(0xFF616161)
//                        ),
//                        modifier = Modifier.weight(1f)
//                    ) {
//                        Text("🔓 CRACK")
//                    }
//                }
//            }
//        }
//
//        // Attack Method Selection (only visible in crack mode)
//        if (mode == OperationMode.CRACK) {
//            Card(
//                modifier = Modifier.fillMaxWidth(),
//                colors = CardDefaults.cardColors(containerColor = Color(0xFF3A1A1A))
//            ) {
//                Column(modifier = Modifier.padding(16.dp)) {
//                    Text(
//                        text = "⚔️ Método de Ataque",
//                        fontSize = 18.sp,
//                        fontWeight = FontWeight.Bold,
//                        color = Color.White,
//                        modifier = Modifier.padding(bottom = 8.dp)
//                    )
//
//                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
//                        Row(
//                            modifier = Modifier.fillMaxWidth(),
//                            horizontalArrangement = Arrangement.spacedBy(4.dp)
//                        ) {
//                            Button(
//                                onClick = { AdvancedMifareManager.setAttackMethod(AttackMethod.DICTIONARY) },
//                                colors = ButtonDefaults.buttonColors(
//                                    containerColor = if (attackMethod == AttackMethod.DICTIONARY) Color(0xFFFF9800) else Color(0xFF616161)
//                                ),
//                                modifier = Modifier.weight(1f)
//                            ) {
//                                Text("📚 DICT", fontSize = 12.sp)
//                            }
//
//                            Button(
//                                onClick = { AdvancedMifareManager.setAttackMethod(AttackMethod.NONCE) },
//                                colors = ButtonDefaults.buttonColors(
//                                    containerColor = if (attackMethod == AttackMethod.NONCE) Color(0xFF9C27B0) else Color(0xFF616161)
//                                ),
//                                modifier = Modifier.weight(1f)
//                            ) {
//                                Text("🎲 NONCE", fontSize = 12.sp)
//                            }
//                        }
//
//                        Row(
//                            modifier = Modifier.fillMaxWidth(),
//                            horizontalArrangement = Arrangement.spacedBy(4.dp)
//                        ) {
//                            Button(
//                                onClick = { AdvancedMifareManager.setAttackMethod(AttackMethod.HARDNESTED) },
//                                colors = ButtonDefaults.buttonColors(
//                                    containerColor = if (attackMethod == AttackMethod.HARDNESTED) Color(0xFFE91E63) else Color(0xFF616161)
//                                ),
//                                modifier = Modifier.weight(1f)
//                            ) {
//                                Text("🔥 HARD", fontSize = 12.sp)
//                            }
//
//                            Button(
//                                onClick = { AdvancedMifareManager.setAttackMethod(AttackMethod.MKF32) },
//                                colors = ButtonDefaults.buttonColors(
//                                    containerColor = if (attackMethod == AttackMethod.MKF32) Color(0xFF00BCD4) else Color(0xFF616161)
//                                ),
//                                modifier = Modifier.weight(1f)
//                            ) {
//                                Text("🔑 MKF32", fontSize = 12.sp)
//                            }
//                        }
//
//                        Button(
//                            onClick = { AdvancedMifareManager.setAttackMethod(AttackMethod.COMBINED) },
//                            colors = ButtonDefaults.buttonColors(
//                                containerColor = if (attackMethod == AttackMethod.COMBINED) Color(0xFFFF5722) else Color(0xFF616161)
//                            ),
//                            modifier = Modifier.fillMaxWidth()
//                        ) {
//                            Text("⚡ ATAQUE COMBINADO", fontSize = 14.sp, fontWeight = FontWeight.Bold)
//                        }
//                    }
//                }
//            }
//        }
//
//        // Status and Progress
//        Card(
//            modifier = Modifier.fillMaxWidth(),
//            colors = CardDefaults.cardColors(containerColor = Color(0xFF1A2A1A))
//        ) {
//            Column(modifier = Modifier.padding(16.dp)) {
//                Text(
//                    text = "📊 Estado",
//                    fontSize = 18.sp,
//                    fontWeight = FontWeight.Bold,
//                    color = Color.White,
//                    modifier = Modifier.padding(bottom = 8.dp)
//                )
//
//                Text(
//                    text = status,
//                    fontSize = 14.sp,
//                    color = Color.White,
//                    modifier = Modifier.padding(bottom = 8.dp)
//                )
//
//                if (progress.isNotEmpty()) {
//                    Text(
//                        text = progress,
//                        fontSize = 12.sp,
//                        color = Color.Gray,
//                        modifier = Modifier.padding(bottom = 8.dp)
//                    )
//                }
//
//                if (totalSectors > 0) {
//                    LinearProgressIndicator(
//                        progress = (currentSector.toFloat() / totalSectors.toFloat()),
//                        modifier = Modifier.fillMaxWidth(),
//                        color = Color(0xFF4CAF50)
//                    )
//                    Text(
//                        text = "Sector: $currentSector/$totalSectors",
//                        fontSize = 12.sp,
//                        color = Color.Gray,
//                        modifier = Modifier.padding(top = 4.dp)
//                    )
//                }
//
//                if (mode == OperationMode.CRACK && crackedSectors.isNotEmpty()) {
//                    Text(
//                        text = "🔓 Sectores Crackeados: ${crackedSectors.size}/$totalSectors",
//                        fontSize = 14.sp,
//                        color = Color(0xFF4CAF50),
//                        fontWeight = FontWeight.Bold,
//                        modifier = Modifier.padding(top = 8.dp)
//                    )
//                }
//            }
//        }
//
//        // Action Buttons
//        Card(
//            modifier = Modifier.fillMaxWidth(),
//            colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2A2A))
//        ) {
//            Row(
//                modifier = Modifier
//                    .fillMaxWidth()
//                    .padding(16.dp),
//                horizontalArrangement = Arrangement.spacedBy(8.dp)
//            ) {
//                Button(
//                    onClick = { AdvancedMifareManager.cancelOperation() },
//                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5722)),
//                    modifier = Modifier.weight(1f),
//                    enabled = isReading || isWriting
//                ) {
//                    Text("❌ CANCELAR")
//                }
//
//                Button(
//                    onClick = { AdvancedMifareManager.clearData() },
//                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF757575)),
//                    modifier = Modifier.weight(1f)
//                ) {
//                    Text("🗑️ LIMPIAR")
//                }
//            }
//        }
//
//        // Card Data Display
//        if (cardData.isNotEmpty()) {
//            Card(
//                modifier = Modifier.fillMaxWidth(),
//                colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A2A))
//            ) {
//                Column(modifier = Modifier.padding(16.dp)) {
//                    Text(
//                        text = "💾 Datos de la Tarjeta",
//                        fontSize = 18.sp,
//                        fontWeight = FontWeight.Bold,
//                        color = Color.White,
//                        modifier = Modifier.padding(bottom = 8.dp)
//                    )
//
//                    LazyColumn(
//                        modifier = Modifier.height(300.dp),
//                        verticalArrangement = Arrangement.spacedBy(4.dp)
//                    ) {
//                        items(cardData.chunked(4)) { sectorBlocks ->
//                            val sectorNum = sectorBlocks.first().sector
//                            val isCracked = crackedSectors.contains(sectorNum)
//
//                            Card(
//                                colors = CardDefaults.cardColors(
//                                    containerColor = if (isCracked) Color(0xFF1A3A1A) else Color(0xFF3A1A1A)
//                                )
//                            ) {
//                                Column(modifier = Modifier.padding(8.dp)) {
//                                    Text(
//                                        text = "Sector $sectorNum ${if (isCracked) "🔓" else "🔒"}",
//                                        fontSize = 14.sp,
//                                        fontWeight = FontWeight.Bold,
//                                        color = if (isCracked) Color(0xFF4CAF50) else Color(0xFFFF5722),
//                                        modifier = Modifier.padding(bottom = 4.dp)
//                                    )
//
//                                    sectorBlocks.forEach { block ->
//                                        Text(
//                                            text = "B${block.block}: ${block.dataAsHex()}",
//                                            fontSize = 10.sp,
//                                            color = Color.Gray,
//                                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
//                                        )
//                                    }
//                                }
//                            }
//                        }
//                    }
//                }
//            }
//        }
//
//        // Found Keys Display (only in crack mode)
//        if (mode == OperationMode.CRACK && foundKeys.isNotEmpty()) {
//            Card(
//                modifier = Modifier.fillMaxWidth(),
//                colors = CardDefaults.cardColors(containerColor = Color(0xFF1A3A1A))
//            ) {
//                Column(modifier = Modifier.padding(16.dp)) {
//                    Text(
//                        text = "🔑 Claves Encontradas",
//                        fontSize = 18.sp,
//                        fontWeight = FontWeight.Bold,
//                        color = Color.White,
//                        modifier = Modifier.padding(bottom = 8.dp)
//                    )
//
//                    LazyColumn(
//                        modifier = Modifier.height(150.dp),
//                        verticalArrangement = Arrangement.spacedBy(4.dp)
//                    ) {
//                        items(foundKeys.toList()) { (sector, keyPair) ->
//                            Card(
//                                colors = CardDefaults.cardColors(containerColor = Color(0xFF2A4A2A))
//                            ) {
//                                Column(modifier = Modifier.padding(8.dp)) {
//                                    Text(
//                                        text = "Sector $sector",
//                                        fontSize = 12.sp,
//                                        fontWeight = FontWeight.Bold,
//                                        color = Color.White
//                                    )
//                                    keyPair.keyA?.let { key ->
//                                        Text(
//                                            text = "Key A: ${key.joinToString("") { "%02X".format(it) }}",
//                                            fontSize = 10.sp,
//                                            color = Color(0xFF81C784),
//                                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
//                                        )
//                                    }
//                                    keyPair.keyB?.let { key ->
//                                        Text(
//                                            text = "Key B: ${key.joinToString("") { "%02X".format(it) }}",
//                                            fontSize = 10.sp,
//                                            color = Color(0xFF81C784),
//                                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
//                                        )
//                                    }
//                                }
//                            }
//                        }
//                    }
//                }
//            }
//        }
//    }
//}
////
////@OptIn(ExperimentalMaterial3Api::class)
////@Composable
////fun MifareNFCApp() {
////    val cardData by MifareManager.cardData
////    val isReading by MifareManager.isReading
////    val isWriting by MifareManager.isWriting
////    val status by MifareManager.status
////    val mode by MifareManager.mode
////    val progress by MifareManager.progress
////    val currentSector by MifareManager.currentSector
////    val totalSectors by MifareManager.totalSectors
////
////    MaterialTheme {
////        Scaffold(
////            topBar = {
////                TopAppBar(
////                    title = { Text("Mifare Classic NFC") },
////                    colors = TopAppBarDefaults.topAppBarColors(
////                        containerColor = MaterialTheme.colorScheme.primaryContainer
////                    )
////                )
////            }
////        ) { paddingValues ->
////            Column(
////                modifier = Modifier
////                    .fillMaxSize()
////                    .padding(paddingValues)
////                    .padding(16.dp)
////            ) {
////                // Controles superiores
////                ModeSelector(
////                    currentMode = mode,
////                    onModeChange = { MifareManager.setMode(it) },
////                    enabled = !isReading && !isWriting
////                )
////
////                Spacer(modifier = Modifier.height(16.dp))
////
////                // Estado actual con progreso
////                StatusCard(
////                    status = status,
////                    progress = progress,
////                    isReading = isReading,
////                    isWriting = isWriting,
////                    currentSector = currentSector,
////                    totalSectors = totalSectors,
////                    onCancel = { MifareManager.cancelOperation() }
////                )
////
////                Spacer(modifier = Modifier.height(16.dp))
////
////                // Botón para limpiar datos
////                if (cardData.isNotEmpty()) {
////                    Button(
////                        onClick = { MifareManager.clearData() },
////                        modifier = Modifier.fillMaxWidth(),
////                        enabled = !isReading && !isWriting
////                    ) {
////                        Text("Limpiar Datos")
////                    }
////
////                    Spacer(modifier = Modifier.height(16.dp))
////                }
////
////                // Lista de bloques
////                if (cardData.isNotEmpty()) {
////                    Text(
////                        text = "Datos de la tarjeta (${cardData.size} bloques):",
////                        style = MaterialTheme.typography.titleMedium,
////                        fontWeight = FontWeight.Bold
////                    )
////
////                    Spacer(modifier = Modifier.height(8.dp))
////
////                    LazyColumn {
////                        items(cardData) { block ->
////                            BlockCard(block = block)
////                            Spacer(modifier = Modifier.height(4.dp))
////                        }
////                    }
////                }
////            }
////        }
////    }
////}
////
////@Composable
////fun StatusCard(
////    status: String,
////    progress: String,
////    isReading: Boolean,
////    isWriting: Boolean,
////    currentSector: Int,
////    totalSectors: Int,
////    onCancel: () -> Unit
////) {
////    Card(
////        modifier = Modifier.fillMaxWidth(),
////        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
////        colors = CardDefaults.cardColors(
////            containerColor = when {
////                isReading -> MaterialTheme.colorScheme.primaryContainer
////                isWriting -> MaterialTheme.colorScheme.secondaryContainer
////                else -> MaterialTheme.colorScheme.surfaceVariant
////            }
////        )
////    ) {
////        Column(
////            modifier = Modifier
////                .fillMaxWidth()
////                .padding(16.dp)
////        ) {
////            Row(
////                modifier = Modifier.fillMaxWidth(),
////                verticalAlignment = Alignment.CenterVertically
////            ) {
////                if (isReading || isWriting) {
////                    CircularProgressIndicator(
////                        modifier = Modifier.size(24.dp),
////                        strokeWidth = 2.dp
////                    )
////                    Spacer(modifier = Modifier.width(12.dp))
////                }
////
////                Column(modifier = Modifier.weight(1f)) {
////                    Text(
////                        text = status,
////                        style = MaterialTheme.typography.bodyMedium
////                    )
////
////                    if (progress.isNotEmpty()) {
////                        Text(
////                            text = progress,
////                            style = MaterialTheme.typography.bodySmall,
////                            color = MaterialTheme.colorScheme.onSurfaceVariant
////                        )
////                    }
////                }
////
////                if (isReading || isWriting) {
////                    Button(
////                        onClick = onCancel,
////                        colors = ButtonDefaults.buttonColors(
////                            containerColor = MaterialTheme.colorScheme.error
////                        )
////                    ) {
////                        Text("Cancelar")
////                    }
////                }
////            }
////
////            // Barra de progreso
////            if ((isReading || isWriting) && totalSectors > 0) {
////                Spacer(modifier = Modifier.height(8.dp))
////                LinearProgressIndicator(
////                    progress = currentSector.toFloat() / totalSectors.toFloat(),
////                    modifier = Modifier.fillMaxWidth()
////                )
////                Text(
////                    text = "$currentSector / $totalSectors sectores",
////                    style = MaterialTheme.typography.bodySmall,
////                    modifier = Modifier.align(Alignment.CenterHorizontally)
////                )
////            }
////        }
////    }
////}
////
////// Los demás composables se mantienen igual...
////@Composable
////fun ModeSelector(
////    currentMode: OperationMode,
////    onModeChange: (OperationMode) -> Unit,
////    enabled: Boolean
////) {
////    Card(
////        modifier = Modifier.fillMaxWidth(),
////        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
////    ) {
////        Column(
////            modifier = Modifier.padding(16.dp)
////        ) {
////            Text(
////                text = "Modo de operación:",
////                style = MaterialTheme.typography.titleSmall,
////                fontWeight = FontWeight.Bold
////            )
////
////            Spacer(modifier = Modifier.height(8.dp))
////
////            Row(
////                modifier = Modifier.fillMaxWidth(),
////                horizontalArrangement = Arrangement.SpaceEvenly
////            ) {
////                FilterChip(
////                    selected = currentMode == OperationMode.READ,
////                    onClick = { onModeChange(OperationMode.READ) },
////                    label = { Text("Leer") },
////                    enabled = enabled
////                )
////
////                FilterChip(
////                    selected = currentMode == OperationMode.WRITE,
////                    onClick = { onModeChange(OperationMode.WRITE) },
////                    label = { Text("Escribir") },
////                    enabled = enabled
////                )
////            }
////        }
////    }
////}
////
////@Composable
////fun BlockCard(block: BlockData) {
////    Card(
////        modifier = Modifier.fillMaxWidth(),
////        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
////        colors = CardDefaults.cardColors(
////            containerColor = when {
////                block.error != null -> MaterialTheme.colorScheme.errorContainer
////                block.isTrailer -> MaterialTheme.colorScheme.tertiaryContainer
////                else -> MaterialTheme.colorScheme.surface
////            }
////        )
////    ) {
////        Column(
////            modifier = Modifier.padding(12.dp)
////        ) {
////            Row(
////                modifier = Modifier.fillMaxWidth(),
////                horizontalArrangement = Arrangement.SpaceBetween
////            ) {
////                Text(
////                    text = "Sector ${block.sector} - Bloque ${block.block}",
////                    style = MaterialTheme.typography.labelMedium,
////                    fontWeight = FontWeight.Bold
////                )
////
////                Row {
////                    if (block.keyType.isNotEmpty()) {
////                        Text(
////                            text = "Key ${block.keyType}",
////                            style = MaterialTheme.typography.labelSmall,
////                            color = MaterialTheme.colorScheme.primary
////                        )
////                        Spacer(modifier = Modifier.width(8.dp))
////                    }
////
////                    if (block.isTrailer) {
////                        Text(
////                            text = "TRAILER",
////                            style = MaterialTheme.typography.labelSmall,
////                            color = MaterialTheme.colorScheme.tertiary
////                        )
////                    }
////                }
////            }
////
////            Spacer(modifier = Modifier.height(4.dp))
////
////            Text(
////                text = block.dataAsHex(),
////                style = MaterialTheme.typography.bodySmall,
////                fontFamily = FontFamily.Monospace,
////                fontSize = 10.sp
////            )
////        }
////    }
////}