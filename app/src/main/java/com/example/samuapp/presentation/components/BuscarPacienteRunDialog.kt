package com.example.samuapp.presentation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PersonSearch
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog

/**
 * Diálogo de Búsqueda de Paciente por RUN chileno en SISVita.
 * 
 * Diseñado específicamente para evitar cualquier error de escritura en terreno:
 * 1. El teclado permanece 100% numérico (evita el salto automático a letras en HyperOS/Android).
 * 2. La escritura es limpia y predecible; el cursor no salta de posición al escribir.
 * 3. Proporciona acceso directo para el Dígito Verificador '- K' sin cambiar de teclado.
 * 4. Valida en tiempo real con el algoritmo oficial Módulo 11 del Registro Civil.
 * 5. Corrige o autocompleta el Dígito Verificador con un solo toque si el usuario se equivoca.
 */
@Composable
fun BuscarPacienteRunDialog(
    onDismiss: () -> Unit,
    onBuscar: (String) -> Unit
) {
    var textFieldValue by remember { mutableStateOf(TextFieldValue("")) }
    val infoRun = remember(textFieldValue.text) { analizarRun(textFieldValue.text) }

    fun actualizarTexto(nuevoTexto: String) {
        val filtrado = nuevoTexto.filter { it.isDigit() || it == 'K' || it == 'k' || it == '-' }.uppercase()

        val textoFinal: String = if (filtrado.contains("-")) {
            val partes = filtrado.split("-")
            val cuerpoDigits = partes[0].filter { it.isDigit() }.take(8)
            val dvChar = partes.getOrNull(1)?.filter { it.isDigit() || it == 'K' }?.take(1) ?: ""
            if (dvChar.isNotEmpty()) {
                "$cuerpoDigits-$dvChar"
            } else if (nuevoTexto.endsWith("-")) {
                // Si el usuario borró el DV y quedó guion huérfano, lo eliminamos limpiamente
                cuerpoDigits
            } else {
                cuerpoDigits
            }
        } else if (filtrado.endsWith("K")) {
            val cuerpoDigits = filtrado.dropLast(1).filter { it.isDigit() }.take(8)
            if (cuerpoDigits.isNotEmpty()) "$cuerpoDigits-K" else "K"
        } else {
            val soloDigitos = filtrado.filter { it.isDigit() }
            if (soloDigitos.length >= 9) {
                val cuerpo8 = soloDigitos.take(8)
                val dv = soloDigitos.substring(8, 9)
                "$cuerpo8-$dv"
            } else {
                soloDigitos.take(8)
            }
        }

        textFieldValue = TextFieldValue(
            text = textoFinal,
            selection = TextRange(textoFinal.length)
        )
    }

    fun ejecutarBusqueda() {
        val runParaBuscar = if (infoRun.esValido) {
            infoRun.formattedDisplay
        } else if (infoRun.cuerpo.length in 7..8 && infoRun.dvCalculado != null) {
            // Autocompletado inteligente si olvidó el DV al buscar
            "${formatearCuerpoConPuntos(infoRun.cuerpo)}-${infoRun.dvCalculado}"
        } else if (textFieldValue.text.isNotBlank()) {
            textFieldValue.text
        } else {
            return
        }

        onBuscar(runParaBuscar)
        onDismiss()
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            shadowElevation = 10.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Encabezado con Icono
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(52.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.PersonSearch,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Buscar Paciente en SISVita",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Ingrese el RUN chileno para consultar antecedentes o ficha clínica.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 18.sp
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Campo de entrada de RUN con teclado 100% numérico
                OutlinedTextField(
                    value = textFieldValue,
                    onValueChange = { nuevoValor ->
                        actualizarTexto(nuevoValor.text)
                    },
                    label = { Text("RUN del Paciente") },
                    placeholder = { Text("Ej: 12345678-9") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Badge,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    trailingIcon = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(end = 6.dp)
                        ) {
                            // Botón de acceso directo para ingresar '- K' con un solo toque
                            if (infoRun.cuerpo.length in 7..8 && infoRun.dv != "K") {
                                FilledTonalButton(
                                    onClick = {
                                        actualizarTexto("${infoRun.cuerpo}-K")
                                    },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.height(32.dp)
                                ) {
                                    Text(
                                        text = "- K",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                }
                                Spacer(modifier = Modifier.width(4.dp))
                            }

                            if (textFieldValue.text.isNotEmpty()) {
                                IconButton(
                                    onClick = { actualizarTexto("") },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Clear,
                                        contentDescription = "Limpiar campo",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    },
                    singleLine = true,
                    isError = infoRun.dv != null && !infoRun.esValido && infoRun.cuerpo.length >= 7,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Search
                    ),
                    keyboardActions = KeyboardActions(
                        onSearch = { ejecutarBusqueda() }
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Tarjeta de estado y validación Módulo 11 en tiempo real
                PanelEstadoRun(
                    infoRun = infoRun,
                    onAplicarSugerencia = { nuevoRun ->
                        actualizarTexto(nuevoRun)
                    }
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Botones de acción
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Cancelar")
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = { ejecutarBusqueda() },
                        enabled = infoRun.esValido || infoRun.cuerpo.length >= 7,
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Buscar",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

/**
 * Panel inteligente de retroalimentación en tiempo real:
 * - Muestra el formato oficial con puntos (ej: 19.876.543-2).
 * - Explica el estado de la validación.
 * - Ofrece botones de 1-toque para corregir o autocompletar el Dígito Verificador.
 */
@Composable
private fun PanelEstadoRun(
    infoRun: InfoRun,
    onAplicarSugerencia: (String) -> Unit
) {
    when {
        // Estado 1: RUN Válido
        infoRun.esValido -> {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFFE8F5E9),
                border = BorderStroke(1.dp, Color(0xFFA5D6A7)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = Color(0xFF2E7D32),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = infoRun.formattedDisplay,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = Color(0xFF1B5E20)
                        )
                        Text(
                            text = "RUN válido verificado con Módulo 11",
                            fontSize = 12.sp,
                            color = Color(0xFF2E7D32)
                        )
                    }
                }
            }
        }

        // Estado 2: Error en Dígito Verificador (ofrece corrección inmediata con 1 toque)
        infoRun.dv != null && !infoRun.esValido && infoRun.cuerpo.length >= 7 && infoRun.dvCalculado != null -> {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.ErrorOutline,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Dígito verificador no coincide",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "Para ${formatearCuerpoConPuntos(infoRun.cuerpo)} el DV correcto es ${infoRun.dvCalculado} (ingresó ${infoRun.dv}).",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        lineHeight = 16.sp
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedButton(
                        onClick = {
                            onAplicarSugerencia("${infoRun.cuerpo}-${infoRun.dvCalculado}")
                        },
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoFixHigh,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Corregir a ${formatearCuerpoConPuntos(infoRun.cuerpo)}-${infoRun.dvCalculado}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        // Estado 3: Cuerpo de 8 dígitos ingresado (esperando DV)
        infoRun.cuerpo.length == 8 && infoRun.dv == null && infoRun.dvCalculado != null -> {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "${formatearCuerpoConPuntos(infoRun.cuerpo)}-?",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Text(
                            text = "DV correspondiente: ${infoRun.dvCalculado}",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                        )
                    }

                    FilledTonalButton(
                        onClick = {
                            onAplicarSugerencia("${infoRun.cuerpo}-${infoRun.dvCalculado}")
                        },
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Text(
                            text = "Agregar -${infoRun.dvCalculado}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Estado 4: Cuerpo de 7 dígitos ingresado (adultos mayores o continuar escribiendo)
        infoRun.cuerpo.length == 7 && infoRun.dv == null && infoRun.dvCalculado != null -> {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "${formatearCuerpoConPuntos(infoRun.cuerpo)} (7 dígitos)",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        FilledTonalButton(
                            onClick = {
                                onAplicarSugerencia("${infoRun.cuerpo}-${infoRun.dvCalculado}")
                            },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.height(30.dp)
                        ) {
                            Text(
                                text = "Usar DV -${infoRun.dvCalculado}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "Si el RUN tiene 8 dígitos, continúe escribiendo en el teclado.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    )
                }
            }
        }

        // Estado 5: Escribiendo cuerpo inicial (1 a 6 dígitos)
        infoRun.cuerpo.isNotEmpty() -> {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "RUN: ${formatearCuerpoConPuntos(infoRun.cuerpo)} (escribiendo números...)",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Estado 6: Vacío inicial
        else -> {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Use el teclado numérico. El guion y la verificación se calculan automáticamente.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 16.sp
                    )
                }
            }
        }
    }
}

/**
 * Representa el estado analizado de un RUN chileno.
 */
data class InfoRun(
    val cuerpo: String,
    val dv: String?,
    val dvCalculado: String?,
    val esValido: Boolean,
    val formattedDisplay: String
)

/**
 * Analiza un texto de entrada y extrae la información del RUN con validación Módulo 11.
 */
private fun analizarRun(texto: String): InfoRun {
    val clean = texto.replace(".", "").replace(" ", "").uppercase()
    if (clean.isEmpty()) {
        return InfoRun("", null, null, false, "")
    }

    var cuerpo = ""
    var dv: String? = null

    if (clean.contains("-")) {
        val partes = clean.split("-")
        cuerpo = partes[0].filter { it.isDigit() }.take(8)
        dv = partes.getOrNull(1)?.filter { it.isDigit() || it == 'K' }?.take(1)
    } else if (clean.endsWith("K")) {
        cuerpo = clean.dropLast(1).filter { it.isDigit() }.take(8)
        dv = "K"
    } else {
        val soloDigitos = clean.filter { it.isDigit() }
        if (soloDigitos.length >= 9) {
            cuerpo = soloDigitos.take(8)
            dv = soloDigitos.substring(8, 9)
        } else {
            cuerpo = soloDigitos.take(8)
            dv = null
        }
    }

    val cuerpoInt = cuerpo.toIntOrNull()
    val dvCalculado = if (cuerpoInt != null && cuerpo.length >= 7) {
        calcularDvModulo11(cuerpoInt)
    } else {
        null
    }

    val esValido = if (cuerpoInt != null && dv != null && dvCalculado != null) {
        dv.uppercase() == dvCalculado
    } else {
        false
    }

    val formattedDisplay = buildString {
        if (cuerpo.isNotEmpty()) {
            append(formatearCuerpoConPuntos(cuerpo))
            if (dv != null) {
                append("-$dv")
            }
        }
    }

    return InfoRun(
        cuerpo = cuerpo,
        dv = dv,
        dvCalculado = dvCalculado,
        esValido = esValido,
        formattedDisplay = formattedDisplay
    )
}

/**
 * Algoritmo oficial de Módulo 11 del Servicio de Registro Civil e Identificación de Chile.
 */
private fun calcularDvModulo11(cuerpo: Int): String {
    var suma = 0
    var multiplicador = 2
    var temp = cuerpo
    while (temp > 0) {
        val digito = temp % 10
        suma += digito * multiplicador
        multiplicador = if (multiplicador == 7) 2 else multiplicador + 1
        temp /= 10
    }
    val resto = suma % 11
    val resultado = 11 - resto
    return when (resultado) {
        11 -> "0"
        10 -> "K"
        else -> resultado.toString()
    }
}

/**
 * Formatea los dígitos del cuerpo con separador de miles '.' de derecha a izquierda.
 */
private fun formatearCuerpoConPuntos(cuerpoStr: String): String {
    if (cuerpoStr.isEmpty()) return ""
    val reversed = cuerpoStr.reversed()
    return reversed.chunked(3).joinToString(".").reversed()
}
