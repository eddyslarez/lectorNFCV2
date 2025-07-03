package com.eddyslarez.lectornfc.presentation.ui.screens

import android.content.Context
import android.nfc.tech.MifareClassic
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eddyslarez.lectornfc.presentation.viewmodel.ToolsViewModel
import com.eddyslarez.lectornfc.data.models.BlockData
import kotlinx.coroutines.delay
import org.koin.androidx.compose.koinViewModel

@Composable
fun ToolsScreen(
    viewModel: ToolsViewModel = koinViewModel()
) {
    val currentTool by viewModel.currentTool.collectAsState()
    val toolResult by viewModel.toolResult.collectAsState()
    val isProcessing by viewModel.isProcessing.collectAsState()
    val tools = remember { getAvailableTools() }
    val context = LocalContext.current

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
                    imageVector = Icons.Default.Build,
                    contentDescription = null,
                    tint = Color(0xFF4CAF50),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "Herramientas Avanzadas",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "Utilidades profesionales para análisis NFC",
                        fontSize = 14.sp,
                        color = Color.Gray
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Mostrar herramienta actual o lista de herramientas
        if (currentTool != null) {
            ToolDetailScreen(
                toolId = currentTool!!,
                result = toolResult,
                isProcessing = isProcessing,
                onBack = { viewModel.closeTool() },
                viewModel = viewModel,
                context = context
            )
        } else {
            // Tools grid
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(tools.chunked(2)) { rowTools ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        rowTools.forEach { tool ->
                            ToolCard(
                                tool = tool,
                                onClick = { viewModel.openTool(tool.id) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        // Fill remaining space if odd number of tools
                        if (rowTools.size == 1) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ToolCard(
    tool: Tool,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2A2A)),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = tool.icon,
                contentDescription = null,
                tint = tool.color,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = tool.name,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = tool.description,
                fontSize = 12.sp,
                color = Color.Gray,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ToolDetailScreen(
    toolId: String,
    result: ToolsViewModel.ToolResult?,
    isProcessing: Boolean,
    onBack: () -> Unit,
    viewModel: ToolsViewModel,
    context: Context
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // Back button
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2A2A))
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Volver",
                        tint = Color.White
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = getToolName(toolId),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Tool content based on toolId
        when (toolId) {
            "clone_tool" -> CloneToolContent(viewModel, result, isProcessing, context)
            "format_tool" -> FormatToolContent(viewModel, result, isProcessing, context)
            "key_generator" -> KeyGeneratorContent(viewModel, result, isProcessing)
            "uid_analyzer" -> UIDAnalyzerContent(viewModel, result, isProcessing)
            "hex_converter" -> HexConverterContent(viewModel, result, isProcessing)
            "key_validator" -> KeyValidatorContent(viewModel, result, isProcessing)
            "sector_calculator" -> SectorCalculatorContent(viewModel, result, isProcessing)
            "frequency_analyzer" -> FrequencyAnalyzerContent(viewModel, result, isProcessing)
            "entropy_calculator" -> EntropyCalculatorContent(viewModel, result, isProcessing)
            "dictionary_manager" -> DictionaryManagerContent(viewModel, result, isProcessing)
        }
    }
}

@Composable
private fun CloneToolContent(
    viewModel: ToolsViewModel,
    result: ToolsViewModel.ToolResult?,
    isProcessing: Boolean,
    context: Context
) {
    var step by remember { mutableStateOf(1) }
    var sourceCardData by remember { mutableStateOf<List<BlockData>?>(null) }
    var cloneProgress by remember { mutableStateOf(0f) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2A2A))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "🎯 Herramienta de Clonado",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF4CAF50)
            )

            Spacer(modifier = Modifier.height(16.dp))

            when (step) {
                1 -> {
                    Text(
                        text = "Paso 1: Acerca la tarjeta FUENTE",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Coloca la tarjeta que quieres copiar sobre el lector NFC",
                        fontSize = 14.sp,
                        color = Color.Gray
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    if (isProcessing) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = Color(0xFF4CAF50))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Leyendo tarjeta fuente...", color = Color.White)
                        }
                    } else {
                        Button(
                            onClick = {
                                viewModel.startCloneSourceRead()
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF4CAF50)
                            )
                        ) {
                            Text("Leer Tarjeta Fuente")
                        }
                    }
                }
                2 -> {
                    Text(
                        text = "Paso 2: Acerca la tarjeta DESTINO",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Coloca la tarjeta donde quieres escribir los datos",
                        fontSize = 14.sp,
                        color = Color.Gray
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    if (isProcessing) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            LinearProgressIndicator(
                                progress = cloneProgress,
                                modifier = Modifier.fillMaxWidth(),
                                color = Color(0xFF4CAF50)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Clonando... ${(cloneProgress * 100).toInt()}%", color = Color.White)
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { step = 1 },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color.Gray
                                ),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Volver")
                            }
                            Button(
                                onClick = {
                                    viewModel.startCloneWrite(sourceCardData)
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFFF5722)
                                ),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Iniciar Clonado")
                            }
                        }
                    }
                }
                3 -> {
                    Text(
                        text = "✅ Clonación Completada",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF4CAF50)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "La tarjeta ha sido clonada exitosamente",
                        fontSize = 14.sp,
                        color = Color.Gray
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = {
                            step = 1
                            sourceCardData = null
                            cloneProgress = 0f
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF4CAF50)
                        )
                    ) {
                        Text("Clonar Otra")
                    }
                }
            }

            // Manejar resultados
            when (result) {
                is ToolsViewModel.ToolResult.CloneSourceResult -> {
                    sourceCardData = result.cardData
                    step = 2
                }
                is ToolsViewModel.ToolResult.CloneCompleteResult -> {
                    step = 3
                }
                is ToolsViewModel.ToolResult.CloneProgressResult -> {
                    cloneProgress = result.progress
                }
                is ToolsViewModel.ToolResult.Error -> {
                    Text(
                        text = "Error: ${result.message}",
                        color = Color(0xFFFF5722),
                        fontSize = 14.sp
                    )
                }
                else -> {}
            }
        }
    }
}

@Composable
private fun FormatToolContent(
    viewModel: ToolsViewModel,
    result: ToolsViewModel.ToolResult?,
    isProcessing: Boolean,
    context: Context
) {
    var showConfirmation by remember { mutableStateOf(false) }
    var formatProgress by remember { mutableStateOf(0f) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF3A1A1A))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "🗑️ Herramienta de Formateo",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFFF5722)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Esta herramienta borrará TODOS los datos de la tarjeta",
                fontSize = 14.sp,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "⚠️ ADVERTENCIA: Esta operación es IRREVERSIBLE",
                fontSize = 12.sp,
                color = Color(0xFFFF5722),
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (isProcessing) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    LinearProgressIndicator(
                        progress = formatProgress,
                        modifier = Modifier.fillMaxWidth(),
                        color = Color(0xFFFF5722)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Formateando... ${(formatProgress * 100).toInt()}%", color = Color.White)
                }
            } else if (!showConfirmation) {
                Button(
                    onClick = { showConfirmation = true },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFF5722)
                    )
                ) {
                    Text("Formatear Tarjeta")
                }
            } else {
                Column {
                    Text(
                        text = "¿Estás completamente seguro?",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFF5722)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { showConfirmation = false },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.Gray
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Cancelar")
                        }
                        Button(
                            onClick = {
                                viewModel.startFormat()
                                showConfirmation = false
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFFF5722)
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("SÍ, FORMATEAR")
                        }
                    }
                }
            }

            // Manejar resultados
            when (result) {
                is ToolsViewModel.ToolResult.FormatProgressResult -> {
                    formatProgress = result.progress
                }
                is ToolsViewModel.ToolResult.FormatCompleteResult -> {
                    Text(
                        text = "✅ Formateo completado: ${result.formattedBlocks} bloques formateados",
                        color = Color(0xFF4CAF50),
                        fontSize = 14.sp
                    )
                }
                is ToolsViewModel.ToolResult.Error -> {
                    Text(
                        text = "Error: ${result.message}",
                        color = Color(0xFFFF5722),
                        fontSize = 14.sp
                    )
                }
                else -> {}
            }
        }
    }
}

// Implementar el resto de las herramientas...
@Composable
private fun KeyGeneratorContent(
    viewModel: ToolsViewModel,
    result: ToolsViewModel.ToolResult?,
    isProcessing: Boolean
) {
    var uid by remember { mutableStateOf("") }
    var sector by remember { mutableStateOf("0") }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2A2A))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "🔑 Generador de Claves MKF32",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFFF9800)
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = uid,
                onValueChange = { uid = it },
                label = { Text("UID (hex)") },
                placeholder = { Text("04A1B2C3") },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFFFF9800),
                    focusedLabelColor = Color(0xFFFF9800)
                )
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = sector,
                onValueChange = { sector = it },
                label = { Text("Sector") },
                placeholder = { Text("0") },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFFFF9800),
                    focusedLabelColor = Color(0xFFFF9800)
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    if (uid.isNotEmpty() && sector.isNotEmpty()) {
                        viewModel.generateMKF32Key(uid, sector.toIntOrNull() ?: 0, com.eddyslarez.lectornfc.domain.attacks.MKFKey32.KeyAlgorithm.ENHANCED)
                    }
                },
                enabled = uid.isNotEmpty() && sector.isNotEmpty() && !isProcessing,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFFF9800)
                )
            ) {
                if (isProcessing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = Color.White
                    )
                } else {
                    Text("Generar Clave")
                }
            }

            // Mostrar resultado
            if (result is ToolsViewModel.ToolResult.KeyGeneratorResult) {
                Spacer(modifier = Modifier.height(16.dp))
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1A3A1A))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "Clave Generada:",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF4CAF50)
                        )
                        Text(
                            text = result.key,
                            fontSize = 16.sp,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Algoritmo: ${result.algorithm}",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                        Text(
                            text = "Entropía: ${String.format("%.2f", result.entropy)}",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }
                }
            }
        }
    }
}

// Continuar con las demás herramientas...
@Composable
private fun UIDAnalyzerContent(viewModel: ToolsViewModel, result: ToolsViewModel.ToolResult?, isProcessing: Boolean) {
    var uid by remember { mutableStateOf("") }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2A2A))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("🔍 Analizador de UID", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2196F3))
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = uid,
                onValueChange = { uid = it },
                label = { Text("UID a analizar") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { if (uid.isNotEmpty()) viewModel.analyzeUID(uid) },
                enabled = uid.isNotEmpty() && !isProcessing
            ) {
                Text("Analizar")
            }
        }
    }
}

@Composable
private fun HexConverterContent(viewModel: ToolsViewModel, result: ToolsViewModel.ToolResult?, isProcessing: Boolean) {
    // Implementación del convertidor hex
    Text("Convertidor HEX - En desarrollo")
}

@Composable
private fun KeyValidatorContent(viewModel: ToolsViewModel, result: ToolsViewModel.ToolResult?, isProcessing: Boolean) {
    // Implementación del validador de claves
    Text("Validador de Claves - En desarrollo")
}

@Composable
private fun SectorCalculatorContent(viewModel: ToolsViewModel, result: ToolsViewModel.ToolResult?, isProcessing: Boolean) {
    // Implementación del calculador de sectores
    Text("Calculadora de Sectores - En desarrollo")
}

@Composable
private fun FrequencyAnalyzerContent(viewModel: ToolsViewModel, result: ToolsViewModel.ToolResult?, isProcessing: Boolean) {
    // Implementación del analizador de frecuencias
    Text("Analizador de Frecuencias - En desarrollo")
}

@Composable
private fun EntropyCalculatorContent(viewModel: ToolsViewModel, result: ToolsViewModel.ToolResult?, isProcessing: Boolean) {
    // Implementación del calculador de entropía
    Text("Calculadora de Entropía - En desarrollo")
}

@Composable
private fun DictionaryManagerContent(viewModel: ToolsViewModel, result: ToolsViewModel.ToolResult?, isProcessing: Boolean) {
    LaunchedEffect(Unit) {
        viewModel.getDictionaryStats()
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2A2A))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("📚 Gestor de Diccionarios", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF795548))

            if (result is ToolsViewModel.ToolResult.DictionaryStatsResult) {
                Spacer(modifier = Modifier.height(16.dp))
                Text("Estadísticas de Diccionarios:", fontWeight = FontWeight.Bold, color = Color.White)
                Spacer(modifier = Modifier.height(8.dp))
                Text("Total de claves: ${result.totalKeys}", color = Color.Gray)
                Text("Claves por defecto: ${result.defaultKeys}", color = Color.Gray)
                Text("Claves de transporte: ${result.transportKeys}", color = Color.Gray)
                Text("Claves de acceso: ${result.accessKeys}", color = Color.Gray)
                Text("Claves rusas: ${result.russianKeys}", color = Color.Gray)
            }
        }
    }
}

private fun getToolName(toolId: String): String {
    return when (toolId) {
        "clone_tool" -> "Herramienta de Clonado"
        "format_tool" -> "Herramienta de Formateo"
        "key_generator" -> "Generador de Claves"
        "uid_analyzer" -> "Analizador de UID"
        "hex_converter" -> "Conversor HEX"
        "key_validator" -> "Validador de Claves"
        "sector_calculator" -> "Calculadora de Sectores"
        "frequency_analyzer" -> "Analizador de Frecuencias"
        "entropy_calculator" -> "Calculadora de Entropía"
        "dictionary_manager" -> "Gestor de Diccionarios"
        else -> "Herramienta"
    }
}

data class Tool(
    val id: String,
    val name: String,
    val description: String,
    val icon: ImageVector,
    val color: Color
)

private fun getAvailableTools(): List<Tool> {
    return listOf(
        Tool(
            id = "clone_tool",
            name = "Clonar Tarjeta",
            description = "Clona tarjetas completas",
            icon = Icons.Default.ContentCopy,
            color = Color(0xFF4CAF50)
        ),
        Tool(
            id = "format_tool",
            name = "Formatear",
            description = "Borra todo el contenido",
            icon = Icons.Default.Delete,
            color = Color(0xFFFF5722)
        ),
        Tool(
            id = "key_generator",
            name = "Generador de Claves",
            description = "Genera claves MKF32",
            icon = Icons.Default.Key,
            color = Color(0xFFFF9800)
        ),
        Tool(
            id = "uid_analyzer",
            name = "Analizador UID",
            description = "Analiza UIDs de tarjetas",
            icon = Icons.Default.Analytics,
            color = Color(0xFF2196F3)
        ),
        Tool(
            id = "hex_converter",
            name = "Conversor HEX",
            description = "Convierte entre formatos",
            icon = Icons.Default.Transform,
            color = Color(0xFF9C27B0)
        ),
        Tool(
            id = "key_validator",
            name = "Validador de Claves",
            description = "Valida claves Mifare",
            icon = Icons.Default.VerifiedUser,
            color = Color(0xFF4CAF50)
        ),
        Tool(
            id = "sector_calculator",
            name = "Calculadora Sectores",
            description = "Calcula bloques y sectores",
            icon = Icons.Default.Calculate,
            color = Color(0xFFFF5722)
        ),
        Tool(
            id = "frequency_analyzer",
            name = "Analizador Frecuencia",
            description = "Analiza patrones de datos",
            icon = Icons.Default.GraphicEq,
            color = Color(0xFF00BCD4)
        ),
        Tool(
            id = "entropy_calculator",
            name = "Calculadora Entropía",
            description = "Mide aleatoriedad",
            icon = Icons.Default.Functions,
            color = Color(0xFFE91E63)
        ),
        Tool(
            id = "dictionary_manager",
            name = "Gestor Diccionarios",
            description = "Administra diccionarios",
            icon = Icons.Default.MenuBook,
            color = Color(0xFF795548)
        )
    )
}