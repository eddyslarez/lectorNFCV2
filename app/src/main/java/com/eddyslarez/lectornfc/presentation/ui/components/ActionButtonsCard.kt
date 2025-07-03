package com.eddyslarez.lectornfc.presentation.ui.components

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

@Composable
fun ActionButtonsCard(
    isOperationInProgress: Boolean,
    hasData: Boolean,
    onCancel: () -> Unit,
    onClear: () -> Unit,
    onExport: () -> Unit,
    onHelp: () -> Unit
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
                    imageVector = Icons.Default.Build,
                    contentDescription = null,
                    tint = Color(0xFF4CAF50),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Acciones",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            // Primera fila de botones
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ActionButton(
                    text = "❌ CANCELAR",
                    onClick = onCancel,
                    enabled = isOperationInProgress,
                    backgroundColor = Color(0xFFFF5722),
                    modifier = Modifier.weight(1f)
                )

                ActionButton(
                    text = "🗑️ LIMPIAR",
                    onClick = onClear,
                    enabled = !isOperationInProgress,
                    backgroundColor = Color(0xFF757575),
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Segunda fila de botones
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ActionButton(
                    text = "📤 EXPORTAR",
                    onClick = onExport,
                    enabled = hasData && !isOperationInProgress,
                    backgroundColor = Color(0xFF4CAF50),
                    modifier = Modifier.weight(1f)
                )

                ActionButton(
                    text = "❓ AYUDA",
                    onClick = onHelp,
                    enabled = true,
                    backgroundColor = Color(0xFF2196F3),
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun ActionButton(
    text: String,
    onClick: () -> Unit,
    enabled: Boolean,
    backgroundColor: Color,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        colors = ButtonDefaults.buttonColors(
            containerColor = backgroundColor,
            disabledContainerColor = Color(0xFF424242)
        ),
        modifier = modifier
    ) {
        Text(
            text = text,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
