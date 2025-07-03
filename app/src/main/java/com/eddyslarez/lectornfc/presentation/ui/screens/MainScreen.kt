package com.eddyslarez.lectornfc.presentation.ui.screens

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eddyslarez.lectornfc.data.models.*
import com.eddyslarez.lectornfc.presentation.ui.components.*
import com.eddyslarez.lectornfc.presentation.viewmodel.HelpTopic
import com.eddyslarez.lectornfc.presentation.viewmodel.MainViewModel
import org.koin.androidx.compose.koinViewModel

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eddyslarez.lectornfc.data.models.*
import com.eddyslarez.lectornfc.presentation.ui.components.*
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: MainViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val showWriteConfirmation by viewModel.showWriteConfirmation.collectAsState()
    val scrollState = rememberScrollState()

    // Observar eventos
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            // Manejar eventos (mostrar snackbars, etc.)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header profesional
        HeaderCard()

        // Selector de modo de operación
        OperationModeCard(
            currentMode = uiState.operationMode,
            onModeChanged = viewModel::setOperationMode,
            isOperationInProgress = uiState.isOperationInProgress
        )

        // Selector de método de ataque (solo en modo crack)
        if (uiState.operationMode == OperationMode.CRACK) {
            AttackMethodCard(
                currentMethod = uiState.attackMethod,
                onMethodChanged = viewModel::setAttackMethod,
                isOperationInProgress = uiState.isOperationInProgress
            )
        }

        // Estado y progreso
        StatusCard(
            status = uiState.status,
            progress = uiState.progress,
            currentSector = uiState.currentSector,
            totalSectors = uiState.totalSectors,
            crackedSectors = uiState.crackedSectors.size,
            operationMode = uiState.operationMode
        )

        // Botones de acción
        ActionButtonsCard(
            isOperationInProgress = uiState.isOperationInProgress,
            hasData = uiState.cardData.isNotEmpty(),
            onCancel = viewModel::cancelOperation,
            onClear = viewModel::clearData,
            onExport = { viewModel.showExportDialog() },
            onHelp = { viewModel.showHelp(HelpTopic.READING) }
        )

        // Visualización de datos de la tarjeta
        if (uiState.cardData.isNotEmpty()) {
            CardDataViewer(
                cardData = uiState.cardData,
                crackedSectors = uiState.crackedSectors,
                modifier = Modifier.height(400.dp)
            )
        }

        // Visualización de claves encontradas
        if (uiState.operationMode == OperationMode.CRACK && uiState.foundKeys.isNotEmpty()) {
            KeyViewer(
                foundKeys = uiState.foundKeys,
                modifier = Modifier.height(200.dp)
            )
        }

        // Estadísticas del escaneo
        if (uiState.cardData.isNotEmpty()) {
            ScanStatisticsCard(
                totalBlocks = uiState.cardData.size,
                readableBlocks = uiState.cardData.count { it.cracked },
                totalSectors = uiState.totalSectors,
                crackedSectors = uiState.crackedSectors.size,
                foundKeys = uiState.foundKeys.size
            )
        }
    }

    // Diálogo de confirmación de escritura
    if (showWriteConfirmation) {
        WriteConfirmationDialog(
            onConfirm = viewModel::confirmWrite,
            onDismiss = viewModel::cancelWrite,
            blocksToWrite = uiState.cardData.filter { !it.isTrailer && it.data.isNotEmpty() }.size
        )
    }

    // Diálogos
    if (uiState.showExportDialog) {
        ExportDialog(
            onDismiss = viewModel::hideExportDialog,
            onExport = { format ->
                viewModel.hideExportDialog()
                viewModel.exportData(format)
            }
        )
    }

    if (uiState.showHelpDialog && uiState.helpTopic != null) {
        HelpDialog(
            topic = uiState.helpTopic!!,
            onDismiss = viewModel::hideHelp
        )
    }
}

@Composable
private fun WriteConfirmationDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    blocksToWrite: Int
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = Color(0xFFFF5722),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Confirmar Escritura")
            }
        },
        text = {
            Column {
                Text(
                    text = "¿Estás seguro de que quieres escribir datos en la tarjeta?",
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "• Se escribirán $blocksToWrite bloques\n• Esta operación es irreversible\n• Asegúrate de tener la tarjeta correcta",
                    fontSize = 14.sp,
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "⚠️ PRECAUCIÓN: La escritura incorrecta puede dañar la tarjeta",
                    fontSize = 12.sp,
                    color = Color(0xFFFF5722),
                    fontWeight = FontWeight.Bold
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFFF5722)
                )
            ) {
                Text("Confirmar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

@Composable
private fun HeaderCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A))
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Security,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = Color(0xFF4CAF50)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Advanced Mifare Pro",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = "Professional NFC Security Tool v3.0",
                fontSize = 14.sp,
                color = Color.Gray
            )
        }
    }
}

@Composable
private fun OperationModeCard(
    currentMode: OperationMode,
    onModeChanged: (OperationMode) -> Unit,
    isOperationInProgress: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2A2A))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = null,
                    tint = Color(0xFF4CAF50),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Modo de Operación",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ModeButton(
                    text = "📖 LEER",
                    isSelected = currentMode == OperationMode.READ,
                    onClick = { onModeChanged(OperationMode.READ) },
                    enabled = !isOperationInProgress,
                    selectedColor = Color(0xFF4CAF50),
                    modifier = Modifier.weight(1f)
                )

                ModeButton(
                    text = "✏️ ESCRIBIR",
                    isSelected = currentMode == OperationMode.WRITE,
                    onClick = { onModeChanged(OperationMode.WRITE) },
                    enabled = !isOperationInProgress,
                    selectedColor = Color(0xFF2196F3),
                    modifier = Modifier.weight(1f)
                )

                ModeButton(
                    text = "🔓 CRACK",
                    isSelected = currentMode == OperationMode.CRACK,
                    onClick = { onModeChanged(OperationMode.CRACK) },
                    enabled = !isOperationInProgress,
                    selectedColor = Color(0xFFFF5722),
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun ModeButton(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    enabled: Boolean,
    selectedColor: Color,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isSelected) selectedColor else Color(0xFF616161),
            disabledContainerColor = Color(0xFF424242)
        ),
        modifier = modifier
    ) {
        Text(
            text = text,
            fontSize = 12.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@Composable
private fun AttackMethodCard(
    currentMethod: AttackMethod,
    onMethodChanged: (AttackMethod) -> Unit,
    isOperationInProgress: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF3A1A1A))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = null,
                    tint = Color(0xFFFF5722),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Método de Ataque",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AttackMethodButton(
                        text = "📚 DICT",
                        description = "Diccionario",
                        isSelected = currentMethod == AttackMethod.DICTIONARY,
                        onClick = { onMethodChanged(AttackMethod.DICTIONARY) },
                        enabled = !isOperationInProgress,
                        selectedColor = Color(0xFFFF9800),
                        modifier = Modifier.weight(1f)
                    )

                    AttackMethodButton(
                        text = "🎲 NONCE",
                        description = "Análisis Nonce",
                        isSelected = currentMethod == AttackMethod.NONCE,
                        onClick = { onMethodChanged(AttackMethod.NONCE) },
                        enabled = !isOperationInProgress,
                        selectedColor = Color(0xFF9C27B0),
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AttackMethodButton(
                        text = "🔥 HARD",
                        description = "Hardnested",
                        isSelected = currentMethod == AttackMethod.HARDNESTED,
                        onClick = { onMethodChanged(AttackMethod.HARDNESTED) },
                        enabled = !isOperationInProgress,
                        selectedColor = Color(0xFFE91E63),
                        modifier = Modifier.weight(1f)
                    )

                    AttackMethodButton(
                        text = "🔑 MKF32",
                        description = "MKF32 Ruso",
                        isSelected = currentMethod == AttackMethod.MKF32,
                        onClick = { onMethodChanged(AttackMethod.MKF32) },
                        enabled = !isOperationInProgress,
                        selectedColor = Color(0xFF00BCD4),
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AttackMethodButton(
                        text = "💪 BRUTE",
                        description = "Fuerza Bruta",
                        isSelected = currentMethod == AttackMethod.BRUTE_FORCE,
                        onClick = { onMethodChanged(AttackMethod.BRUTE_FORCE) },
                        enabled = !isOperationInProgress,
                        selectedColor = Color(0xFFFF6F00),
                        modifier = Modifier.weight(1f)
                    )

                    // Spacer para mantener el diseño
                    Spacer(modifier = Modifier.weight(1f))
                }

                Button(
                    onClick = { onMethodChanged(AttackMethod.COMBINED) },
                    enabled = !isOperationInProgress,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (currentMethod == AttackMethod.COMBINED)
                            Color(0xFFFF5722) else Color(0xFF616161)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "⚡ ATAQUE COMBINADO",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun AttackMethodButton(
    text: String,
    description: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    enabled: Boolean,
    selectedColor: Color,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isSelected) selectedColor else Color(0xFF616161),
            disabledContainerColor = Color(0xFF424242)
        ),
        modifier = modifier
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = text,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = description,
                fontSize = 8.sp,
                color = Color.Gray
            )
        }
    }
}

@Composable
private fun ScanStatisticsCard(
    totalBlocks: Int,
    readableBlocks: Int,
    totalSectors: Int,
    crackedSectors: Int,
    foundKeys: Int
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A2A1A))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Analytics,
                    contentDescription = null,
                    tint = Color(0xFF4CAF50),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Estadísticas del Escaneo",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatisticItem(
                    label = "Bloques",
                    value = "$readableBlocks/$totalBlocks",
                    percentage = if (totalBlocks > 0) (readableBlocks * 100 / totalBlocks) else 0
                )

                StatisticItem(
                    label = "Sectores",
                    value = "$crackedSectors/$totalSectors",
                    percentage = if (totalSectors > 0) (crackedSectors * 100 / totalSectors) else 0
                )

                StatisticItem(
                    label = "Claves",
                    value = foundKeys.toString(),
                    percentage = null
                )
            }
        }
    }
}

@Composable
private fun StatisticItem(
    label: String,
    value: String,
    percentage: Int?
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF4CAF50)
        )
        Text(
            text = label,
            fontSize = 12.sp,
            color = Color.Gray
        )
        percentage?.let {
            Text(
                text = "$it%",
                fontSize = 10.sp,
                color = Color(0xFF81C784)
            )
        }
    }
}