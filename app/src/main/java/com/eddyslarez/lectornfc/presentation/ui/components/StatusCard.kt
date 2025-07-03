package com.eddyslarez.lectornfc.presentation.ui.components

import android.util.Log
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
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
import com.eddyslarez.lectornfc.data.models.OperationMode

@Composable
fun StatusCard(
    status: String,
    progress: String,
    currentSector: Int,
    totalSectors: Int,
    crackedSectors: Int,
    operationMode: OperationMode
) {
    Log.d("estadodelsistema", status)
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A2A1A))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                StatusIcon(operationMode = operationMode)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Estado del Sistema",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            // Estado principal
            Text(
                text = status,
                fontSize = 14.sp,
                color = Color.White,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            // Progreso detallado
            if (progress.isNotEmpty()) {
                Text(
                    text = progress,
                    fontSize = 12.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            // Barra de progreso y estadísticas
            if (totalSectors > 0) {
                ProgressSection(
                    currentSector = currentSector,
                    totalSectors = totalSectors,
                    crackedSectors = crackedSectors,
                    operationMode = operationMode
                )
            }
        }
    }
}

@Composable
private fun StatusIcon(operationMode: OperationMode) {
    val infiniteTransition = rememberInfiniteTransition(label = "status_icon")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    val (icon, color) = when (operationMode) {
        OperationMode.READ -> Icons.Default.Visibility to Color(0xFF4CAF50)
        OperationMode.WRITE -> Icons.Default.Edit to Color(0xFF2196F3)
        OperationMode.CRACK -> Icons.Default.Security to Color(0xFFFF5722)
    }

    Icon(
        imageVector = icon,
        contentDescription = null,
        tint = color.copy(alpha = alpha),
        modifier = Modifier.size(24.dp)
    )
}

@Composable
private fun ProgressSection(
    currentSector: Int,
    totalSectors: Int,
    crackedSectors: Int,
    operationMode: OperationMode
) {
    Column {
        // Barra de progreso principal
        val progress = if (totalSectors > 0) currentSector.toFloat() / totalSectors.toFloat() else 0f

        LinearProgressIndicator(
            progress = progress,
            modifier = Modifier.fillMaxWidth(),
            color = when (operationMode) {
                OperationMode.READ -> Color(0xFF4CAF50)
                OperationMode.WRITE -> Color(0xFF2196F3)
                OperationMode.CRACK -> Color(0xFFFF5722)
            },
            trackColor = Color(0xFF424242)
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Estadísticas detalladas
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Sector: $currentSector/$totalSectors",
                fontSize = 12.sp,
                color = Color.Gray
            )

            if (operationMode == OperationMode.CRACK && crackedSectors > 0) {
                Text(
                    text = "🔓 Crackeados: $crackedSectors",
                    fontSize = 12.sp,
                    color = Color(0xFF4CAF50),
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Porcentaje de progreso
        if (totalSectors > 0) {
            val percentage = (progress * 100).toInt()
            Text(
                text = "$percentage% completado",
                fontSize = 10.sp,
                color = Color.Gray,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}
