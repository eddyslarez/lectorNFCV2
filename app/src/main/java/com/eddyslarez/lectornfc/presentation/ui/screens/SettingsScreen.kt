package com.eddyslarez.lectornfc.presentation.ui.screens


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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.content.Context
import android.content.SharedPreferences
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eddyslarez.lectornfc.presentation.ui.screens.TutorialScreen

@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("mifare_settings", Context.MODE_PRIVATE) }

    var enableVibration by remember { mutableStateOf(prefs.getBoolean("vibration", true)) }
    var enableSound by remember { mutableStateOf(prefs.getBoolean("sound", false)) }
    var autoSave by remember { mutableStateOf(prefs.getBoolean("auto_save", true)) }
    var darkMode by remember { mutableStateOf(prefs.getBoolean("dark_mode", true)) }
    var advancedMode by remember { mutableStateOf(prefs.getBoolean("advanced_mode", false)) }
    var showTutorial by remember { mutableStateOf(false) }

    // Función para guardar configuraciones
    fun saveSettings() {
        prefs.edit().apply {
            putBoolean("vibration", enableVibration)
            putBoolean("sound", enableSound)
            putBoolean("auto_save", autoSave)
            putBoolean("dark_mode", darkMode)
            putBoolean("advanced_mode", advancedMode)
            apply()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
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
                    imageVector = Icons.Default.Settings,
                    contentDescription = null,
                    tint = Color(0xFF4CAF50),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Configuración",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // General Settings
        SettingsSection(title = "General") {
            SettingsSwitch(
                title = "Modo Oscuro",
                description = "Usar tema oscuro en la aplicación",
                icon = Icons.Default.DarkMode,
                checked = darkMode,
                onCheckedChange = {
                    darkMode = it
                    saveSettings()
                }
            )

            SettingsSwitch(
                title = "Vibración",
                description = "Vibrar al detectar tarjetas NFC",
                icon = Icons.Default.Vibration,
                checked = enableVibration,
                onCheckedChange = {
                    enableVibration = it
                    saveSettings()
                }
            )

            SettingsSwitch(
                title = "Sonidos",
                description = "Reproducir sonidos de notificación",
                icon = Icons.Default.VolumeUp,
                checked = enableSound,
                onCheckedChange = {
                    enableSound = it
                    saveSettings()
                }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Scanning Settings
        SettingsSection(title = "Escaneo") {
            SettingsSwitch(
                title = "Guardado Automático",
                description = "Guardar escaneos automáticamente en el historial",
                icon = Icons.Default.Save,
                checked = autoSave,
                onCheckedChange = {
                    autoSave = it
                    saveSettings()
                }
            )

            SettingsSwitch(
                title = "Modo Avanzado",
                description = "Mostrar opciones avanzadas de configuración",
                icon = Icons.Default.Engineering,
                checked = advancedMode,
                onCheckedChange = {
                    advancedMode = it
                    saveSettings()
                }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Help & Info
        SettingsSection(title = "Ayuda e Información") {
            SettingsButton(
                title = "Tutorial",
                description = "Ver tutorial de la aplicación",
                icon = Icons.Default.School,
                onClick = { showTutorial = true }
            )

            SettingsButton(
                title = "Acerca de",
                description = "Información sobre la aplicación",
                icon = Icons.Default.Info,
                onClick = { /* Navigate to about */ }
            )

            SettingsButton(
                title = "Licencias",
                description = "Ver licencias de código abierto",
                icon = Icons.Default.Description,
                onClick = { /* Show licenses */ }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Advanced Settings (only if advanced mode is enabled)
        if (advancedMode) {
            SettingsSection(title = "Configuración Avanzada") {
                SettingsButton(
                    title = "Gestionar Diccionarios",
                    description = "Administrar diccionarios de claves",
                    icon = Icons.Default.MenuBook,
                    onClick = { /* Open dictionary manager */ }
                )

                SettingsButton(
                    title = "Configurar Ataques",
                    description = "Ajustar parámetros de ataques",
                    icon = Icons.Default.Tune,
                    onClick = { /* Open attack config */ }
                )

                SettingsButton(
                    title = "Exportar Configuración",
                    description = "Respaldar configuración actual",
                    icon = Icons.Default.Backup,
                    onClick = { /* Export settings */ }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        // Danger Zone
        SettingsSection(title = "Zona de Peligro") {
            SettingsButton(
                title = "Limpiar Historial",
                description = "Eliminar todos los escaneos guardados",
                icon = Icons.Default.DeleteSweep,
                onClick = { /* Clear history */ },
                textColor = Color(0xFFFF5722)
            )

            SettingsButton(
                title = "Restablecer Configuración",
                description = "Volver a la configuración por defecto",
                icon = Icons.Default.RestartAlt,
                onClick = {
                    // Restablecer configuraciones
                    enableVibration = true
                    enableSound = false
                    autoSave = true
                    darkMode = true
                    advancedMode = false
                    saveSettings()
                },
                textColor = Color(0xFFFF5722)
            )
        }
    }

    // Tutorial Dialog
    if (showTutorial) {
        TutorialScreen(
            onFinish = { showTutorial = false }
        )
    }
}

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2A2A))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF4CAF50),
                modifier = Modifier.padding(bottom = 12.dp)
            )
            content()
        }
    }
}

@Composable
private fun SettingsSwitch(
    title: String,
    description: String,
    icon: ImageVector,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color(0xFF4CAF50),
            modifier = Modifier.size(24.dp)
        )

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White
            )
            Text(
                text = description,
                fontSize = 14.sp,
                color = Color.Gray
            )
        }

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color(0xFF4CAF50),
                checkedTrackColor = Color(0xFF4CAF50).copy(alpha = 0.5f)
            )
        )
    }
}

@Composable
private fun SettingsButton(
    title: String,
    description: String,
    icon: ImageVector,
    onClick: () -> Unit,
    textColor: Color = Color.White
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF3A3A3A)),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (textColor == Color.White) Color(0xFF4CAF50) else textColor,
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = textColor
                )
                Text(
                    text = description,
                    fontSize = 14.sp,
                    color = if (textColor == Color.White) Color.Gray else textColor.copy(alpha = 0.7f)
                )
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = Color.Gray,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}