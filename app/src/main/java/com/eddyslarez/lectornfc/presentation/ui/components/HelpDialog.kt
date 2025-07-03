package com.eddyslarez.lectornfc.presentation.ui.components


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
import androidx.compose.ui.window.Dialog
import com.eddyslarez.lectornfc.presentation.viewmodel.HelpTopic

@Composable
fun HelpDialog(
    topic: HelpTopic,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.8f)
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
                        imageVector = Icons.Default.Help,
                        contentDescription = null,
                        tint = Color(0xFF2196F3),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = getHelpTitle(topic),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                // Contenido scrolleable
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                ) {
                    HelpContent(topic = topic)
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Botón de cerrar
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF2196F3)
                    )
                ) {
                    Text("Entendido")
                }
            }
        }
    }
}

@Composable
private fun HelpContent(topic: HelpTopic) {
    when (topic) {
        HelpTopic.READING -> ReadingHelp()
        HelpTopic.WRITING -> WritingHelp()
        HelpTopic.CRACKING -> CrackingHelp()
        HelpTopic.DICTIONARY_ATTACK -> DictionaryAttackHelp()
        HelpTopic.HARDNESTED_ATTACK -> HardnestedAttackHelp()
        HelpTopic.NONCE_ATTACK -> NonceAttackHelp()
        HelpTopic.MKF32_ATTACK -> MKF32AttackHelp()
        HelpTopic.EXPORT_DATA -> ExportDataHelp()
        HelpTopic.SECURITY_TIPS -> SecurityTipsHelp()
    }
}

@Composable
private fun ReadingHelp() {
    HelpSection(
        title = "📖 Modo Lectura",
        content = """
            El modo lectura permite extraer información de tarjetas Mifare Classic.
            
            **Cómo usar:**
            1. Selecciona el modo "LEER"
            2. Acerca la tarjeta al dispositivo
            3. La aplicación intentará leer todos los sectores
            4. Los datos se mostrarán en formato hexadecimal
            
            **Características:**
            • Lectura automática de todos los sectores
            • Detección de claves conocidas
            • Visualización detallada de bloques
            • Identificación de sectores de trailer
            
            **Consejos:**
            • Mantén la tarjeta estable durante la lectura
            • Algunos sectores pueden requerir claves específicas
            • Los datos se guardan automáticamente para escritura posterior
        """.trimIndent()
    )
}

@Composable
private fun WritingHelp() {
    HelpSection(
        title = "✏️ Modo Escritura",
        content = """
            El modo escritura permite copiar datos previamente leídos a otra tarjeta.
            
            **Cómo usar:**
            1. Primero lee una tarjeta en modo lectura
            2. Selecciona el modo "ESCRIBIR"
            3. Acerca la tarjeta destino al dispositivo
            4. Los datos se escribirán automáticamente
            
            **Importante:**
            • Solo se escriben bloques de datos, no trailers
            • Se requieren las claves correctas para escribir
            • El proceso es irreversible
            
            **Precauciones:**
            ⚠️ La escritura puede dañar la tarjeta si no se hace correctamente
            ⚠️ Asegúrate de tener una copia de seguridad
            ⚠️ No interrumpas el proceso de escritura
        """.trimIndent()
    )
}

@Composable
private fun CrackingHelp() {
    HelpSection(
        title = "🔓 Modo Crack",
        content = """
            El modo crack utiliza técnicas avanzadas para descifrar claves de acceso.
            
            **Métodos disponibles:**
            • **Diccionario**: Prueba claves conocidas
            • **Nonce**: Analiza patrones de comunicación
            • **Hardnested**: Ataque criptográfico avanzado
            • **MKF32**: Especializado en sistemas rusos
            • **Combinado**: Usa todos los métodos
            
            **Proceso:**
            1. Selecciona el método de ataque
            2. Acerca la tarjeta al dispositivo
            3. El ataque se ejecuta automáticamente
            4. Las claves encontradas se muestran en tiempo real
            
            **Tiempo estimado:**
            • Diccionario: 1-5 minutos
            • Nonce: 2-10 minutos
            • Hardnested: 5-30 minutos
            • MKF32: 1-3 minutos
            • Combinado: 10-45 minutos
        """.trimIndent()
    )
}

@Composable
private fun DictionaryAttackHelp() {
    HelpSection(
        title = "📚 Ataque de Diccionario",
        content = """
            Prueba claves conocidas y comunes contra la tarjeta.
            
            **Diccionarios incluidos:**
            • Claves por defecto de fabricantes
            • Claves de sistemas de transporte público
            • Claves de sistemas de domófonos rusos
            • Claves de sistemas hoteleros
            • Patrones numéricos comunes
            
            **Ventajas:**
            ✅ Rápido y eficiente
            ✅ Alta tasa de éxito en sistemas mal configurados
            ✅ No requiere conocimientos técnicos
            
            **Limitaciones:**
            ❌ Solo funciona con claves conocidas
            ❌ Inefectivo contra claves personalizadas
            
            **Consejos:**
            • Es el primer método recomendado
            • Funciona especialmente bien con domófonos
            • Combínalo con otros métodos para mejores resultados
        """.trimIndent()
    )
}

@Composable
private fun HardnestedAttackHelp() {
    HelpSection(
        title = "🔥 Ataque Hardnested",
        content = """
            Técnica criptográfica avanzada que explota debilidades en el cifrado Crypto-1.
            
            **Cómo funciona:**
            1. Requiere al menos una clave conocida
            2. Analiza la comunicación entre lector y tarjeta
            3. Utiliza correlaciones criptográficas
            4. Deriva claves de otros sectores
            
            **Requisitos:**
            • Al menos una clave conocida (obtenida por diccionario)
            • Tarjeta compatible con Crypto-1
            • Tiempo de procesamiento considerable
            
            **Efectividad:**
            • Muy alta contra tarjetas vulnerables
            • Puede recuperar todas las claves
            • Funciona incluso con claves aleatorias
            
            **Proceso:**
            1. Ejecuta primero un ataque de diccionario
            2. Si encuentra al menos una clave, usa Hardnested
            3. El algoritmo derivará las claves restantes
            
            ⚠️ Este ataque puede tomar mucho tiempo
        """.trimIndent()
    )
}

@Composable
private fun NonceAttackHelp() {
    HelpSection(
        title = "🎲 Ataque de Nonce",
        content = """
            Analiza los números aleatorios (nonces) generados por la tarjeta.
            
            **Principio:**
            • Las tarjetas usan generadores de números aleatorios
            • Algunos generadores tienen patrones predecibles
            • El análisis estadístico puede revelar las claves
            
            **Técnicas utilizadas:**
            • Análisis de frecuencia
            • Detección de patrones temporales
            • Correlación estadística
            • Análisis de entropía
            
            **Efectivo contra:**
            • Tarjetas con generadores débiles
            • Sistemas con implementaciones defectuosas
            • Clones de baja calidad
            
            **Limitaciones:**
            • Requiere múltiples lecturas
            • No funciona con generadores seguros
            • Puede dar falsos positivos
            
            **Indicadores de éxito:**
            • Patrones repetitivos en nonces
            • Baja entropía en los datos
            • Correlaciones temporales
        """.trimIndent()
    )
}

@Composable
private fun MKF32AttackHelp() {
    HelpSection(
        title = "🔑 Ataque MKF32",
        content = """
            Algoritmo especializado para sistemas de domófonos rusos.
            
            **Sistemas compatibles:**
            • Vizit
            • Cyfral
            • Метаком (Metakom)
            • БВД (BVD)
            • Otros sistemas rusos
            
            **Algoritmos incluidos:**
            • **Enhanced**: Versión mejorada del MKF32
            • **Russian Domophone**: Específico para domófonos
            • **Statistical**: Basado en análisis estadístico
            • **Adaptive**: Selecciona automáticamente el mejor
            • **Cryptographic**: Usa funciones hash seguras
            
            **Cómo funciona:**
            1. Analiza el UID de la tarjeta
            2. Aplica transformaciones específicas
            3. Genera claves candidatas
            4. Prueba las claves generadas
            
            **Ventajas:**
            ✅ Muy efectivo contra sistemas rusos
            ✅ Rápido (1-3 minutos)
            ✅ Alta tasa de éxito
            
            **Consejos:**
            • Ideal para tarjetas de domófonos
            • Combina múltiples algoritmos
            • Funciona incluso con claves "aleatorias"
        """.trimIndent()
    )
}

@Composable
private fun ExportDataHelp() {
    HelpSection(
        title = "📤 Exportar Datos",
        content = """
            Guarda los datos leídos en diferentes formatos para análisis posterior.
            
            **Formatos disponibles:**
            
            **TXT (Texto Plano)**
            • Fácil de leer para humanos
            • Compatible con cualquier editor
            • Ideal para documentación
            
            **JSON (JavaScript Object Notation)**
            • Formato estructurado
            • Compatible con aplicaciones web
            • Fácil de procesar programáticamente
            
            **CSV (Comma Separated Values)**
            • Compatible con Excel
            • Ideal para análisis estadístico
            • Formato tabular
            
            **XML (eXtensible Markup Language)**
            • Estándar para intercambio de datos
            • Compatible con muchas aplicaciones
            • Estructura jerárquica
            
            **Información incluida:**
            • Datos de todos los bloques
            • Claves encontradas
            • Información del UID
            • Timestamp del escaneo
            • Estadísticas del proceso
            
            **Compartir:**
            • Email
            • WhatsApp
            • Telegram
            • Guardar en archivo
        """.trimIndent()
    )
}

@Composable
private fun SecurityTipsHelp() {
    HelpSection(
        title = "🛡️ Consejos de Seguridad",
        content = """
            **Uso Ético y Legal:**
            ⚠️ Solo usa esta herramienta en tarjetas de tu propiedad
            ⚠️ Respeta las leyes locales sobre seguridad informática
            ⚠️ No uses para actividades ilegales
            
            **Protección de Datos:**
            • Los datos se almacenan localmente
            • No se envía información a servidores externos
            • Borra los datos después del análisis
            • Usa contraseñas seguras para archivos exportados
            
            **Mejores Prácticas:**
            • Mantén la aplicación actualizada
            • Usa en entornos controlados
            • Documenta tus hallazgos
            • Reporta vulnerabilidades responsablemente
            
            **Protección contra Ataques:**
            • Usa claves aleatorias de 6 bytes
            • Cambia claves por defecto
            • Implementa autenticación adicional
            • Monitorea accesos no autorizados
            
            **Detección de Vulnerabilidades:**
            • Prueba regularmente tus sistemas
            • Usa múltiples métodos de ataque
            • Documenta las debilidades encontradas
            • Implementa contramedidas apropiadas
            
            **Responsabilidad:**
            El usuario es responsable del uso de esta herramienta.
            Úsala solo para propósitos legítimos de seguridad.
        """.trimIndent()
    )
}

@Composable
private fun HelpSection(
    title: String,
    content: String
) {
    Column {
        Text(
            text = title,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF4CAF50),
            modifier = Modifier.padding(bottom = 12.dp)
        )

        Text(
            text = content,
            fontSize = 14.sp,
            color = Color.White,
            lineHeight = 20.sp
        )
    }
}

private fun getHelpTitle(topic: HelpTopic): String {
    return when (topic) {
        HelpTopic.READING -> "Ayuda - Lectura"
        HelpTopic.WRITING -> "Ayuda - Escritura"
        HelpTopic.CRACKING -> "Ayuda - Cracking"
        HelpTopic.DICTIONARY_ATTACK -> "Ayuda - Ataque Diccionario"
        HelpTopic.HARDNESTED_ATTACK -> "Ayuda - Ataque Hardnested"
        HelpTopic.NONCE_ATTACK -> "Ayuda - Ataque Nonce"
        HelpTopic.MKF32_ATTACK -> "Ayuda - Ataque MKF32"
        HelpTopic.EXPORT_DATA -> "Ayuda - Exportar Datos"
        HelpTopic.SECURITY_TIPS -> "Consejos de Seguridad"
    }
}
