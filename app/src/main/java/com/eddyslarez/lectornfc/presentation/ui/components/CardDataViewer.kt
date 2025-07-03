package com.eddyslarez.lectornfc.presentation.ui.components


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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eddyslarez.lectornfc.data.models.BlockData

@Composable
fun CardDataViewer(
    cardData: List<BlockData>,
    crackedSectors: Set<Int>,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A2A))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Storage,
                    contentDescription = null,
                    tint = Color(0xFF4CAF50),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Datos de la Tarjeta",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = "${cardData.count { it.cracked }}/${cardData.size} bloques",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(cardData.chunked(4)) { sectorBlocks ->
                    if (sectorBlocks.isNotEmpty()) {
                        SectorCard(
                            sectorBlocks = sectorBlocks,
                            isCracked = crackedSectors.contains(sectorBlocks.first().sector)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SectorCard(
    sectorBlocks: List<BlockData>,
    isCracked: Boolean
) {
    val sectorNum = sectorBlocks.first().sector

    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (isCracked) Color(0xFF1A3A1A) else Color(0xFF3A1A1A)
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                Icon(
                    imageVector = if (isCracked) Icons.Default.LockOpen else Icons.Default.Lock,
                    contentDescription = null,
                    tint = if (isCracked) Color(0xFF4CAF50) else Color(0xFFFF5722),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Sector $sectorNum",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isCracked) Color(0xFF4CAF50) else Color(0xFFFF5722)
                )
                Spacer(modifier = Modifier.weight(1f))
                if (isCracked) {
                    val readableBlocks = sectorBlocks.count { it.cracked }
                    Text(
                        text = "$readableBlocks/${sectorBlocks.size}",
                        fontSize = 12.sp,
                        color = Color(0xFF81C784)
                    )
                }
            }

            sectorBlocks.forEach { block ->
                BlockRow(block = block)
            }
        }
    }
}

@Composable
private fun BlockRow(block: BlockData) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 2.dp)
    ) {
        // Indicador de estado del bloque
        Icon(
            imageVector = when {
                block.cracked -> Icons.Default.CheckCircle
                block.error != null -> Icons.Default.Error
                else -> Icons.Default.RadioButtonUnchecked
            },
            contentDescription = null,
            tint = when {
                block.cracked -> Color(0xFF4CAF50)
                block.error != null -> Color(0xFFFF5722)
                else -> Color.Gray
            },
            modifier = Modifier.size(12.dp)
        )

        Spacer(modifier = Modifier.width(8.dp))

        // Número de bloque
        Text(
            text = "B${block.block}:",
            fontSize = 10.sp,
            color = Color.Gray,
            modifier = Modifier.width(32.dp)
        )

        Spacer(modifier = Modifier.width(4.dp))

        // Datos del bloque
        Text(
            text = block.dataAsHex(),
            fontSize = 9.sp,
            color = if (block.cracked) Color.White else Color.Gray,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.weight(1f)
        )

        // Indicador de trailer
        if (block.isTrailer) {
            Text(
                text = "T",
                fontSize = 8.sp,
                color = Color(0xFFFF9800),
                fontWeight = FontWeight.Bold
            )
        }
    }

    // Mostrar información de la clave usada si está disponible
    if (block.keyUsed != null && block.cracked) {
        Row(
            modifier = Modifier.padding(start = 20.dp, top = 2.dp)
        ) {
            Text(
                text = "Key ${block.keyType}: ",
                fontSize = 8.sp,
                color = Color.Gray
            )
            Text(
                text = block.keyUsed.joinToString("") { "%02X".format(it) },
                fontSize = 8.sp,
                color = Color(0xFF81C784),
                fontFamily = FontFamily.Monospace
            )
        }
    }
}
