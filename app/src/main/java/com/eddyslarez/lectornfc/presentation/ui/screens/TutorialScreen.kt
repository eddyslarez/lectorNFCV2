package com.eddyslarez.lectornfc.presentation.ui.screens


import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*

import androidx.compose.ui.draw.clip

import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TutorialScreen(
    onFinish: () -> Unit = {}
) {
    val pagerState = rememberPagerState(pageCount = { tutorialPages.size })
    val scope = rememberCoroutineScope()

    // Diálogo a pantalla completa
    Dialog(
        onDismissRequest = onFinish,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF121212)),
            color = Color(0xFF121212)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Top bar
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.School,
                                contentDescription = null,
                                tint = Color(0xFF4CAF50),
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Tutorial Interactivo")
                        }
                    },
                    actions = {
                        TextButton(onClick = onFinish) {
                            Text("Saltar", color = Color(0xFF4CAF50))
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color(0xFF1E1E1E),
                        titleContentColor = Color.White
                    )
                )

                // Content
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.weight(1f)
                ) { page ->
                    TutorialPage(tutorialPages[page])
                }

                // Bottom navigation
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
                    shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        // Page indicators
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            repeat(tutorialPages.size) { index ->
                                val isSelected = index == pagerState.currentPage
                                Box(
                                    modifier = Modifier
                                        .padding(horizontal = 4.dp)
                                        .size(if (isSelected) 12.dp else 8.dp)
                                        .clip(RoundedCornerShape(50))
                                        .background(
                                            if (isSelected) Color(0xFF4CAF50) else Color.Gray
                                        )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Navigation buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Previous button
                            TextButton(
                                onClick = {
                                    scope.launch {
                                        if (pagerState.currentPage > 0) {
                                            pagerState.animateScrollToPage(pagerState.currentPage - 1)
                                        }
                                    }
                                },
                                enabled = pagerState.currentPage > 0
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ArrowBack,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Anterior")
                            }

                            // Page counter
                            Text(
                                text = "${pagerState.currentPage + 1} de ${tutorialPages.size}",
                                fontSize = 14.sp,
                                color = Color.Gray
                            )

                            // Next/Finish button
                            Button(
                                onClick = {
                                    scope.launch {
                                        if (pagerState.currentPage < tutorialPages.size - 1) {
                                            pagerState.animateScrollToPage(pagerState.currentPage + 1)
                                        } else {
                                            onFinish()
                                        }
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF4CAF50)
                                )
                            ) {
                                Text(
                                    if (pagerState.currentPage < tutorialPages.size - 1) "Siguiente" else "Finalizar"
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = if (pagerState.currentPage < tutorialPages.size - 1)
                                        Icons.Default.ArrowForward else Icons.Default.Check,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TutorialPage(page: TutorialPageData) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(32.dp))

        // Icon with animation
        Card(
            modifier = Modifier.size(100.dp),
            colors = CardDefaults.cardColors(
                containerColor = page.color.copy(alpha = 0.1f)
            ),
            shape = RoundedCornerShape(50.dp)
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = page.icon,
                    contentDescription = null,
                    modifier = Modifier.size(50.dp),
                    tint = page.color
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Title
        Text(
            text = page.title,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Description
        Text(
            text = page.description,
            fontSize = 16.sp,
            color = Color.Gray,
            textAlign = TextAlign.Center,
            lineHeight = 24.sp
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Steps
        if (page.steps.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E))
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(bottom = 16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.List,
                            contentDescription = null,
                            tint = page.color,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Pasos a seguir:",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    page.steps.forEachIndexed { index, step ->
                        Row(
                            modifier = Modifier.padding(vertical = 6.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .background(
                                        page.color,
                                        RoundedCornerShape(12.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "${index + 1}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = step,
                                fontSize = 14.sp,
                                color = Color.White,
                                modifier = Modifier.weight(1f),
                                lineHeight = 20.sp
                            )
                        }
                    }
                }
            }
        }

        // Tips
        if (page.tips.isNotEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F2A0F))
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(bottom = 12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lightbulb,
                            contentDescription = null,
                            tint = Color(0xFFFFEB3B),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Consejos útiles:",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    page.tips.forEach { tip ->
                        Row(
                            modifier = Modifier.padding(vertical = 4.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Icon(
                                imageVector = Icons.Default.TipsAndUpdates,
                                contentDescription = null,
                                tint = Color(0xFF4CAF50),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = tip,
                                fontSize = 14.sp,
                                color = Color.White,
                                modifier = Modifier.weight(1f),
                                lineHeight = 18.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

data class TutorialPageData(
    val title: String,
    val description: String,
    val icon: ImageVector,
    val color: Color,
    val steps: List<String> = emptyList(),
    val tips: List<String> = emptyList()
)

private val tutorialPages = listOf(
    TutorialPageData(
        title = "¡Bienvenido a Advanced Mifare Pro!",
        description = "La herramienta más completa y profesional para análisis de seguridad NFC y tarjetas Mifare Classic. Desarrollada por expertos en seguridad digital.",
        icon = Icons.Default.Security,
        color = Color(0xFF4CAF50),
        tips = listOf(
            "Esta aplicación es para uso educativo y auditorías de seguridad",
            "Solo usa en tarjetas de tu propiedad o con autorización",
            "Respeta siempre las leyes locales e internacionales",
            "Reporta vulnerabilidades de manera responsable"
        )
    ),
    TutorialPageData(
        title = "Modo Lectura 📖",
        description = "Lee y analiza el contenido completo de tarjetas Mifare Classic usando diccionarios avanzados con más de 10,000 claves conocidas.",
        icon = Icons.Default.Visibility,
        color = Color(0xFF2196F3),
        steps = listOf(
            "Selecciona el modo 'LEER' en la pantalla principal",
            "Acerca la tarjeta al sensor NFC de tu dispositivo",
            "Mantén la tarjeta estable durante todo el proceso",
            "Espera a que termine el escaneo completo",
            "Revisa los datos encontrados en la visualización"
        ),
        tips = listOf(
            "El proceso puede tomar de 30 segundos a 5 minutos",
            "Los datos se guardan automáticamente en el historial",
            "Puedes exportar los resultados en múltiples formatos",
            "La aplicación muestra el progreso en tiempo real"
        )
    ),
    TutorialPageData(
        title = "Modo Crack 🔓",
        description = "Utiliza algoritmos avanzados de criptoanálisis para descifrar claves de acceso de tarjetas protegidas con métodos profesionales.",
        icon = Icons.Default.Lock,
        color = Color(0xFFFF5722),
        steps = listOf(
            "Selecciona el modo 'CRACK' en la pantalla principal",
            "Elige el método de ataque más apropiado",
            "Acerca la tarjeta al sensor NFC",
            "Observa el progreso en tiempo real",
            "Las claves encontradas se muestran automáticamente"
        ),
        tips = listOf(
            "El ataque combinado es el más efectivo pero lento",
            "MKF32 es muy efectivo en sistemas de domófonos rusos",
            "Hardnested requiere al menos una clave conocida",
            "Algunos ataques pueden tomar hasta 45 minutos"
        )
    ),
    TutorialPageData(
        title = "Métodos de Ataque Avanzados ⚡",
        description = "Diferentes algoritmos especializados para distintos tipos de sistemas, desde ataques de diccionario hasta técnicas criptográficas avanzadas.",
        icon = Icons.Default.Psychology,
        color = Color(0xFF9C27B0),
        steps = listOf(
            "Diccionario: Prueba +10,000 claves conocidas y comunes",
            "MKF32: Algoritmo especializado en sistemas domófonos rusos",
            "Hardnested: Ataque criptográfico que explota debilidades Crypto-1",
            "Análisis Nonce: Detecta patrones en generadores de números aleatorios",
            "Fuerza Bruta: Prueba combinaciones sistemáticamente",
            "Combinado: Ejecuta todos los métodos secuencialmente"
        ),
        tips = listOf(
            "Empieza siempre con el ataque de diccionario",
            "MKF32 funciona excelente con Vizit, Cyfral y Metakom",
            "El análisis de nonce detecta generadores débiles",
            "La fuerza bruta es el último recurso para sectores resistentes"
        )
    ),
    TutorialPageData(
        title = "Herramientas Profesionales 🛠️",
        description = "Suite completa de herramientas especializadas para análisis forense, clonado, formateo y gestión avanzada de tarjetas NFC.",
        icon = Icons.Default.Build,
        color = Color(0xFF00BCD4),
        steps = listOf(
            "Clonador: Copia completa de tarjetas con desencriptación automática",
            "Formateador: Limpia tarjetas de manera segura e irreversible",
            "Generador MKF32: Crea claves para sistemas específicos",
            "Analizador UID: Detecta patrones y vulnerabilidades en identificadores",
            "Conversor HEX: Transforma entre diferentes formatos de datos",
            "Validador: Evalúa la fortaleza criptográfica de claves"
        ),
        tips = listOf(
            "El clonador maneja automáticamente tarjetas encriptadas",
            "Siempre confirma antes de formatear - es irreversible",
            "El generador MKF32 tiene múltiples algoritmos especializados",
            "Usa el analizador UID para detectar clones o patrones débiles"
        )
    ),
    TutorialPageData(
        title = "Exportar y Compartir 📤",
        description = "Guarda y comparte los resultados de tus análisis en múltiples formatos profesionales con metadatos completos y estadísticas detalladas.",
        icon = Icons.Default.Share,
        color = Color(0xFF4CAF50),
        steps = listOf(
            "Presiona el botón 'EXPORTAR' después de un escaneo",
            "Selecciona el formato: TXT, JSON, CSV o XML",
            "Elige el método de compartir: Email, WhatsApp, Telegram o archivo",
            "Los archivos se guardan en Descargas/MifarePro/",
            "Cada exportación incluye metadatos y estadísticas completas"
        ),
        tips = listOf(
            "JSON es ideal para análisis programático y herramientas",
            "TXT es perfecto para documentación y reportes legibles",
            "CSV funciona excelente con Excel y análisis estadístico",
            "XML es estándar para intercambio con otras aplicaciones"
        )
    ),
    TutorialPageData(
        title = "Configuración y Personalización ⚙️",
        description = "Personaliza completamente tu experiencia con modo oscuro, vibración, sonidos, guardado automático y configuraciones avanzadas.",
        icon = Icons.Default.Settings,
        color = Color(0xFF795548),
        steps = listOf(
            "Accede a Configuración desde el menú inferior",
            "Activa vibración para retroalimentación táctil",
            "Habilita sonidos para notificaciones audibles",
            "Configura guardado automático en historial",
            "Modo avanzado desbloquea opciones profesionales",
            "Personaliza diccionarios y parámetros de ataque"
        ),
        tips = listOf(
            "La vibración ayuda a confirmar detección de tarjetas",
            "El modo avanzado incluye configuración de ataques",
            "Puedes exportar y respaldar tu configuración",
            "El historial automático facilita el seguimiento de auditorías"
        )
    ),
    TutorialPageData(
        title = "¡Listo para Ser un Experto! 🎓",
        description = "Ya tienes todo el conocimiento necesario para usar Advanced Mifare Pro como un profesional de la seguridad digital. ¡Comienza tus auditorías!",
        icon = Icons.Default.CheckCircle,
        color = Color(0xFF4CAF50),
        tips = listOf(
            "Practica con diferentes tipos de tarjetas para ganar experiencia",
            "Revisa el historial para analizar patrones en tus auditorías",
            "Usa las herramientas especializadas para análisis forense detallado",
            "Consulta la ayuda contextual si tienes dudas específicas",
            "Mantén la app actualizada para nuevos algoritmos y mejoras",
            "Únete a la comunidad de seguridad para compartir conocimientos",
            "Documenta siempre tus hallazgos para auditorías profesionales"
        )
    )
)