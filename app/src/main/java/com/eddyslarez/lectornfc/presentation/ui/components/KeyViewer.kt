package com.eddyslarez.lectornfc.presentation.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eddyslarez.lectornfc.data.models.KeyPair

@Composable
fun KeyViewer(
    foundKeys: Map<Int, KeyPair>,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A3A1A))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Key,
                    contentDescription = null,
                    tint = Color(0xFF4CAF50),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Claves Encontradas",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = "${foundKeys.size} sectores",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(foundKeys.toList().sortedBy { it.first }) { (sector, keyPair) ->
                    KeyCard(
                        sector = sector,
                        keyPair = keyPair,
                        onCopyKey = { key, type ->
                            copyToClipboard(context, key, "Clave $type del Sector $sector")
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun KeyCard(
    sector: Int,
    keyPair: KeyPair,
    onCopyKey: (String, String) -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2A4A2A))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = null,
                    tint = Color(0xFF4CAF50),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Sector $sector",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.weight(1f))

                // Indicador de tipos de clave disponibles
                Row {
                    if (keyPair.keyA != null) {
                        Text(
                            text = "A",
                            fontSize = 10.sp,
                            color = Color(0xFF4CAF50),
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                    if (keyPair.keyB != null) {
                        Text(
                            text = "B",
                            fontSize = 10.sp,
                            color = Color(0xFF2196F3),
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            // Mostrar Key A si está disponible
            keyPair.keyA?.let { key ->
                KeyRow(
                    label = "Key A",
                    key = key,
                    color = Color(0xFF4CAF50),
                    onCopy = { onCopyKey(it, "A") }
                )
            }

            // Mostrar Key B si está disponible
            keyPair.keyB?.let { key ->
                KeyRow(
                    label = "Key B",
                    key = key,
                    color = Color(0xFF2196F3),
                    onCopy = { onCopyKey(it, "B") }
                )
            }
        }
    }
}

@Composable
private fun KeyRow(
    label: String,
    key: ByteArray,
    color: Color,
    onCopy: (String) -> Unit
) {
    val keyString = key.joinToString(" ") { "%02X".format(it) }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 2.dp)
    ) {
        Text(
            text = "$label:",
            fontSize = 12.sp,
            color = color,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.width(50.dp)
        )

        Spacer(modifier = Modifier.width(8.dp))

        Text(
            text = keyString,
            fontSize = 12.sp,
            color = Color.White,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.weight(1f)
        )

        // Botón para copiar la clave
        IconButton(
            onClick = { onCopy(keyString) },
            modifier = Modifier.size(24.dp)
        ) {
            Icon(
                imageVector = Icons.Default.ContentCopy,
                contentDescription = "Copiar clave",
                tint = Color.Gray,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

private fun copyToClipboard(context: Context, text: String, label: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText(label, text)
    clipboard.setPrimaryClip(clip)
    Toast.makeText(context, "Clave copiada al portapapeles", Toast.LENGTH_SHORT).show()
}