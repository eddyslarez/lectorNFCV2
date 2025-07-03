package com.eddyslarez.lectornfc.presentation.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.eddyslarez.lectornfc.presentation.viewmodel.ExportFormat

@Composable
fun ExportDialog(
    onDismiss: () -> Unit,
    onExport: (ExportFormat) -> Unit
) {
    var selectedFormat by remember { mutableStateOf(ExportFormat.TXT) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2A2A))
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                // Título
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.FileDownload,
                        contentDescription = null,
                        tint = Color(0xFF4CAF50),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Exportar Datos",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                Text(
                    text = "Selecciona el formato de exportación:",
                    fontSize = 14.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // Opciones de formato
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ExportFormatOption(
                        format = ExportFormat.TXT,
                        icon = Icons.Default.Description,
                        title = "Texto Plano (.txt)",
                        description = "Formato legible para humanos",
                        isSelected = selectedFormat == ExportFormat.TXT,
                        onSelect = { selectedFormat = ExportFormat.TXT }
                    )

                    ExportFormatOption(
                        format = ExportFormat.JSON,
                        icon = Icons.Default.Code,
                        title = "JSON (.json)",
                        description = "Formato estructurado para aplicaciones",
                        isSelected = selectedFormat == ExportFormat.JSON,
                        onSelect = { selectedFormat = ExportFormat.JSON }
                    )

                    ExportFormatOption(
                        format = ExportFormat.CSV,
                        icon = Icons.Default.TableChart,
                        title = "CSV (.csv)",
                        description = "Compatible con Excel y hojas de cálculo",
                        isSelected = selectedFormat == ExportFormat.CSV,
                        onSelect = { selectedFormat = ExportFormat.CSV }
                    )

                    ExportFormatOption(
                        format = ExportFormat.XML,
                        icon = Icons.Default.DataObject,
                        title = "XML (.xml)",
                        description = "Formato estándar para intercambio de datos",
                        isSelected = selectedFormat == ExportFormat.XML,
                        onSelect = { selectedFormat = ExportFormat.XML }
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Botones de acción
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color.Gray
                        )
                    ) {
                        Text("Cancelar")
                    }

                    Button(
                        onClick = { onExport(selectedFormat) },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF4CAF50)
                        )
                    ) {
                        Text("Exportar")
                    }
                }
            }
        }
    }
}

@Composable
private fun ExportFormatOption(
    format: ExportFormat,
    icon: ImageVector,
    title: String,
    description: String,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(
                selected = isSelected,
                onClick = onSelect
            ),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) Color(0xFF1A3A1A) else Color(0xFF3A3A3A)
        )
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                selected = isSelected,
                onClick = onSelect,
                colors = RadioButtonDefaults.colors(
                    selectedColor = Color(0xFF4CAF50)
                )
            )

            Spacer(modifier = Modifier.width(8.dp))

            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) Color(0xFF4CAF50) else Color.Gray,
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White
                )
                Text(
                    text = description,
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }
        }
    }
}
