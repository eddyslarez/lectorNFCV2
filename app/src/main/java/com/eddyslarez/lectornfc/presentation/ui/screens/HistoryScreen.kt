package com.eddyslarez.lectornfc.presentation.ui.screens

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.eddyslarez.lectornfc.data.database.entities.ScanHistoryEntity
import com.eddyslarez.lectornfc.presentation.viewmodel.HistoryViewModel
import org.koin.androidx.compose.koinViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    viewModel: HistoryViewModel = koinViewModel()
) {
    val historyItems by viewModel.historyItems.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    var showDeleteDialog by remember { mutableStateOf(false) }
    var itemToDelete by remember { mutableStateOf<ScanHistoryEntity?>(null) }



    LaunchedEffect(historyItems) {
        Log.d("HistoryScreen", "History items count: ${historyItems.size}")
    }

    LaunchedEffect(isLoading) {
        Log.d("HistoryScreen", "Loading state: $isLoading")
    }

    LaunchedEffect(Unit) {
        Log.d("HistoryScreen", "Calling loadHistory()")
        viewModel.loadHistory()
    }
    LaunchedEffect(Unit) {
        viewModel.loadHistory()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Header
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A))
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.History,
                    contentDescription = null,
                    tint = Color(0xFF4CAF50),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Historial de Escaneos",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "${historyItems.size} escaneos guardados",
                        fontSize = 14.sp,
                        color = Color.Gray
                    )
                }
                IconButton(
                    onClick = { viewModel.clearHistory() }
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteSweep,
                        contentDescription = "Limpiar historial",
                        tint = Color(0xFFFF5722)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Content
        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Color(0xFF4CAF50))
            }
        } else if (historyItems.isEmpty()) {
            EmptyHistoryState()
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(historyItems) { item ->
                    HistoryItemCard(
                        item = item,
                        onDelete = {
                            itemToDelete = item
                            showDeleteDialog = true
                        },
                        onExport = { viewModel.exportItem(item) },
                        onView = { viewModel.viewItem(item) }
                    )
                }
            }
        }
    }

    // Delete confirmation dialog
    if (showDeleteDialog && itemToDelete != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Confirmar eliminación") },
            text = { Text("¿Estás seguro de que quieres eliminar este escaneo?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        itemToDelete?.let { viewModel.deleteItem(it) }
                        showDeleteDialog = false
                        itemToDelete = null
                    }
                ) {
                    Text("Eliminar", color = Color(0xFFFF5722))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
private fun HistoryItemCard(
    item: ScanHistoryEntity,
    onDelete: () -> Unit,
    onExport: () -> Unit,
    onView: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2A2A))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header with date and actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = formatDate(item.timestamp),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "UID: ${item.uid}",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }

                Row {
                    IconButton(onClick = onView) {
                        Icon(
                            imageVector = Icons.Default.Visibility,
                            contentDescription = "Ver",
                            tint = Color(0xFF2196F3)
                        )
                    }
                    IconButton(onClick = onExport) {
                        Icon(
                            imageVector = Icons.Default.FileDownload,
                            contentDescription = "Exportar",
                            tint = Color(0xFF4CAF50)
                        )
                    }
                    IconButton(onClick = onDelete) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Eliminar",
                            tint = Color(0xFFFF5722)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Statistics
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                StatItem(
                    label = "Sectores",
                    value = "${item.crackedSectors}/${item.totalSectors}",
                    color = if (item.crackedSectors == item.totalSectors) Color(0xFF4CAF50) else Color(0xFFFF9800)
                )
                StatItem(
                    label = "Bloques",
                    value = item.totalBlocks.toString(),
                    color = Color(0xFF2196F3)
                )
                StatItem(
                    label = "Claves",
                    value = item.foundKeys.toString(),
                    color = Color(0xFF9C27B0)
                )
                StatItem(
                    label = "Método",
                    value = item.attackMethod,
                    color = Color(0xFFFF5722)
                )
            }

            // Success rate bar
            if (item.totalSectors > 0) {
                Spacer(modifier = Modifier.height(8.dp))
                val successRate = item.crackedSectors.toFloat() / item.totalSectors
                LinearProgressIndicator(
                    progress = successRate,
                    modifier = Modifier.fillMaxWidth(),
                    color = when {
                        successRate >= 0.8f -> Color(0xFF4CAF50)
                        successRate >= 0.5f -> Color(0xFFFF9800)
                        else -> Color(0xFFFF5722)
                    }
                )
                Text(
                    text = "Éxito: ${(successRate * 100).toInt()}%",
                    fontSize = 12.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun StatItem(
    label: String,
    value: String,
    color: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = label,
            fontSize = 10.sp,
            color = Color.Gray
        )
    }
}

@Composable
private fun EmptyHistoryState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.HistoryToggleOff,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = Color.Gray
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "No hay escaneos guardados",
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                color = Color.Gray
            )
            Text(
                text = "Los escaneos aparecerán aquí automáticamente",
                fontSize = 14.sp,
                color = Color.Gray.copy(alpha = 0.7f)
            )
        }
    }
}

private fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
