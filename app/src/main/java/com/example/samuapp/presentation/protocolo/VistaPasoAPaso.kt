package com.example.samuapp.presentation.protocolo

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.samuapp.domain.model.NodoProtocolo
import com.example.samuapp.domain.model.Protocolo
import com.example.samuapp.ui.theme.LocalDarkTheme

import androidx.compose.runtime.saveable.rememberSaveable

@Composable
fun VistaPasoAPaso(
    protocolo: Protocolo,
    nodoInicialId: String? = null,
    modifier: Modifier = Modifier
) {
    var nodoActualId by rememberSaveable(protocolo.id) {
        mutableStateOf(nodoInicialId ?: protocolo.nodoInicial)
    }
    val historialNodos = remember(protocolo.id) { mutableStateListOf<String>() }

    val nodo: NodoProtocolo? = protocolo.nodos[nodoActualId]
    val isDark = LocalDarkTheme.current

    if (nodo == null) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Nodo '$nodoActualId' no encontrado en el protocolo")
        }
        return
    }

    val tipoColor = obtenerColorTipo(nodo.tipo, isDark)

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Barra de progreso y controles de navegación
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (historialNodos.isNotEmpty()) {
                IconButton(onClick = {
                    if (historialNodos.isNotEmpty()) {
                        nodoActualId = historialNodos.removeLast()
                    }
                }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Paso anterior")
                }
            }

            Text(
                text = "Paso ${historialNodos.size + 1}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )

            IconButton(onClick = {
                historialNodos.clear()
                nodoActualId = nodoInicialId ?: protocolo.nodoInicial
            }) {
                Icon(Icons.Default.Refresh, contentDescription = "Reiniciar flujo")
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Tarjeta del Nodo Actual
        Card(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp)
            ) {
                // Badge del tipo de nodo
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(tipoColor.copy(alpha = if (isDark) 0.25f else 0.15f))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = nodo.tipo.uppercase(),
                        color = tipoColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Texto principal del nodo con soporte para listas indexadas
                TextoProtocoloIndexado(
                    texto = nodo.texto,
                    isDark = isDark
                )

                // Instrucción clínica extra (recuadro ámbar)
                if (!nodo.instruccionExtra.isNullOrEmpty()) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isDark) Color(0xFF332600) else Color(0xFFFFF8E1))
                            .border(1.dp, if (isDark) Color(0xFFFFA000) else Color(0xFFFFD54F), RoundedCornerShape(8.dp))
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = if (isDark) Color(0xFFFFD54F) else Color(0xFFFF8F00)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = nodo.instruccionExtra,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isDark) Color(0xFFFFECB3) else Color(0xFFE65100)
                        )
                    }
                }

                // Tarjeta de Medicamento / Droga (recuadro rojo)
                if (nodo.tipo == "droga") {
                    Spacer(modifier = Modifier.height(16.dp))
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isDark) Color(0xFF3B1212) else Color(0xFFFFEBEE))
                            .border(1.dp, if (isDark) Color(0xFFC62828) else Color(0xFFFFCDD2), RoundedCornerShape(12.dp))
                            .padding(14.dp)
                    ) {
                        Text(
                            text = "💊 ${nodo.nombreDroga ?: "Medicamento"}",
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) Color(0xFFEF9A9A) else Color(0xFFC62828)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Dosis: ${nodo.dosisFormula} ${nodo.unidad ?: ""}",
                            color = if (isDark) Color(0xFFFFCDD2) else Color(0xFFB71C1C),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "Vía: ${nodo.via ?: ""} | ${nodo.presentacion ?: ""}",
                            color = if (isDark) Color(0xFFFFCDD2) else Color(0xFFB71C1C),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Opciones de Decisión o Botón de Continuación
        if (nodo.tipo == "pregunta" && nodo.opciones.isNotEmpty()) {
            nodo.opciones.forEach { opcion ->
                Button(
                    onClick = {
                        if (protocolo.nodos.containsKey(opcion.siguienteNodo)) {
                            historialNodos.add(nodoActualId)
                            nodoActualId = opcion.siguienteNodo
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = opcion.texto,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
            }
        } else if (nodo.siguienteNodo != null) {
            Button(
                onClick = {
                    if (protocolo.nodos.containsKey(nodo.siguienteNodo)) {
                        historialNodos.add(nodoActualId)
                        nodoActualId = nodo.siguienteNodo
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Siguiente paso",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }
        } else {
            // Fin del protocolo
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isDark) Color(0xFF0F3017) else Color(0xFFE8F5E9))
                    .border(1.dp, if (isDark) Color(0xFF2E7D32) else Color(0xFFA5D6A7), RoundedCornerShape(12.dp))
                    .padding(14.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = if (isDark) Color(0xFF81C784) else Color(0xFF2E7D32)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Fin de la secuencia de actuación",
                    fontWeight = FontWeight.Bold,
                    color = if (isDark) Color(0xFFA5D6A7) else Color(0xFF1B5E20)
                )
            }
        }
    }
}

private fun obtenerColorTipo(tipo: String, isDark: Boolean): Color {
    return if (isDark) {
        when (tipo) {
            "checklist" -> Color(0xFFFFB74D)
            "pregunta" -> Color(0xFF64B5F6)
            "droga" -> Color(0xFFE57373)
            else -> Color(0xFF4DB6AC)
        }
    } else {
        when (tipo) {
            "checklist" -> Color(0xFFE65100)
            "pregunta" -> Color(0xFF1565C0)
            "droga" -> Color(0xFFC62828)
            else -> Color(0xFF00695C)
        }
    }
}

@Composable
private fun TextoProtocoloIndexado(
    texto: String,
    isDark: Boolean,
    modifier: Modifier = Modifier
) {
    val lineas = remember(texto) { texto.split("\n").map { it.trim() }.filter { it.isNotEmpty() } }

    val esLista = lineas.any { linea ->
        linea.matches(Regex("""^(\d+[\.\)]|[▪•✓\-\*✔☑]).*"""))
    }

    if (!esLista) {
        val esPregunta = texto.trim().startsWith("¿") || texto.trim().endsWith("?")
        Text(
            text = texto,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = if (esPregunta) TextAlign.Center else TextAlign.Start,
            modifier = if (esPregunta) modifier.fillMaxWidth() else modifier
        )
        return
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        lineas.forEach { linea ->
            val numMatch = Regex("""^(\d+)[\.\)]\s*(.*)$""").find(linea)
            val bulletMatch = Regex("""^([▪•✓\-\*✔☑])\s*(.*)$""").find(linea)

            when {
                numMatch != null -> {
                    val numero = numMatch.groupValues[1]
                    val contenido = numMatch.groupValues[2]

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9))
                            .border(1.dp, if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0), RoundedCornerShape(10.dp))
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Badge circular con el número del paso
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = numero,
                                color = MaterialTheme.colorScheme.onPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Text(
                            text = contenido,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                bulletMatch != null -> {
                    val contenido = bulletMatch.groupValues[2]

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9))
                            .border(1.dp, if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0), RoundedCornerShape(10.dp))
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Icono distintivo de verificación
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = if (isDark) Color(0xFF4ADE80) else Color(0xFF16A34A),
                            modifier = Modifier.size(24.dp)
                        )

                        Spacer(modifier = Modifier.width(12.dp))

                        Text(
                            text = contenido,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                else -> {
                    // Encabezado o condición (ej: "¿SIGNOS DE INESTABILIDAD...?" o "Manejo:")
                    val esPregunta = linea.trim().startsWith("¿") || linea.trim().endsWith("?")
                    Text(
                        text = linea,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = if (esPregunta) TextAlign.Center else TextAlign.Start,
                        modifier = Modifier.fillMaxWidth().padding(top = 6.dp, bottom = 2.dp)
                    )
                }
            }
        }
    }
}
