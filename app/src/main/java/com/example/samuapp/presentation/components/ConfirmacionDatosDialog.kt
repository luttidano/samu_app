package com.example.samuapp.presentation.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.saveable.rememberSaveable
import com.example.samuapp.domain.model.DatosPaciente
import com.example.samuapp.domain.model.ProtocoloIndice

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfirmacionDatosDialog(
    datosIniciales: DatosPaciente,
    opcionesProtocolos: List<ProtocoloIndice>,
    esEntradaManual: Boolean = false,
    onDismiss: () -> Unit,
    onConfirmar: (DatosPaciente) -> Unit
) {
    var protocoloInicial = remember(datosIniciales.urgencia, opcionesProtocolos) {
        val query = (datosIniciales.urgencia ?: "").trim().lowercase()
        if (query.isEmpty()) null
        else opcionesProtocolos.find { p ->
            p.id.lowercase() == query ||
                p.nombre.lowercase() == query ||
                p.nombre.lowercase().contains(query) ||
                p.tagsBusqueda.any { it.lowercase() == query || it.lowercase().contains(query) }
        }
    }
    
    var urgenciaSeleccionada by rememberSaveable { 
        mutableStateOf(protocoloInicial?.nombre ?: (datosIniciales.urgencia ?: "")) 
    }
    var textoUrgencia by rememberSaveable {
        mutableStateOf(protocoloInicial?.nombre ?: (datosIniciales.urgencia ?: ""))
    }

    var sexoSeleccionado by rememberSaveable { mutableStateOf(datosIniciales.sexo ?: "masculino") }
    var pesoTexto by rememberSaveable {
        mutableStateOf(datosIniciales.pesoKg?.let { "%.0f".format(it) } ?: "")
    }

    var menuUrgenciaExpandido by rememberSaveable { mutableStateOf(false) }
    var menuSexoExpandido by rememberSaveable { mutableStateOf(false) }

    // Filtrar opciones en tiempo real según lo que escribe el usuario (nombre, id o tags)
    val opcionesFiltradas = remember(textoUrgencia, opcionesProtocolos) {
        val query = textoUrgencia.lowercase().trim()
        if (query.isEmpty()) {
            opcionesProtocolos
        } else {
            opcionesProtocolos.filter { p ->
                p.nombre.lowercase().contains(query) ||
                        p.id.lowercase().contains(query) ||
                        p.tagsBusqueda.any { it.lowercase().contains(query) }
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(16.dp),
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.HealthAndSafety,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (esEntradaManual) "Ingresar datos clínicos" else "Validar Datos Clínicos",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = if (esEntradaManual) {
                        "Complete los datos del paciente para continuar:"
                    } else {
                        "Verifique o edite los datos dictados antes de continuar:"
                    },
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Selector Urgencia / Protocolo (CON FILTRADO EN TIEMPO REAL)
                Text(
                    text = "Urgencia / Protocolo:",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp
                )
                Spacer(modifier = Modifier.height(6.dp))

                ExposedDropdownMenuBox(
                    expanded = menuUrgenciaExpandido,
                    onExpandedChange = { menuUrgenciaExpandido = it }
                ) {
                    OutlinedTextField(
                        value = textoUrgencia,
                        onValueChange = { nuevoTexto ->
                            textoUrgencia = nuevoTexto
                            menuUrgenciaExpandido = true
                            // Si borra o cambia, intentamos resolver coincidencia
                            val match = opcionesProtocolos.find {
                                it.nombre.equals(nuevoTexto.trim(), ignoreCase = true) ||
                                        it.id.equals(nuevoTexto.trim(), ignoreCase = true)
                            }
                            if (match != null) {
                                urgenciaSeleccionada = match.id
                            }
                        },
                        placeholder = { Text("Escriba o seleccione protocolo...") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = menuUrgenciaExpandido) },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryEditable)
                    )

                    if (opcionesFiltradas.isNotEmpty()) {
                        ExposedDropdownMenu(
                            expanded = menuUrgenciaExpandido,
                            onDismissRequest = { menuUrgenciaExpandido = false }
                        ) {
                            opcionesFiltradas.forEach { item ->
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text(item.nombre, fontWeight = FontWeight.Medium)
                                            Text(
                                                item.categoria,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    },
                                    onClick = {
                                        urgenciaSeleccionada = item.id
                                        textoUrgencia = item.nombre
                                        menuUrgenciaExpandido = false
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Selector Sexo
                Text(
                    text = "Sexo del paciente:",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp
                )
                Spacer(modifier = Modifier.height(6.dp))

                ExposedDropdownMenuBox(
                    expanded = menuSexoExpandido,
                    onExpandedChange = { menuSexoExpandido = !menuSexoExpandido }
                ) {
                    OutlinedTextField(
                        value = if (sexoSeleccionado.equals("femenino", ignoreCase = true)) "Femenino" else "Masculino",
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = menuSexoExpandido) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    )

                    ExposedDropdownMenu(
                        expanded = menuSexoExpandido,
                        onDismissRequest = { menuSexoExpandido = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Masculino") },
                            onClick = {
                                sexoSeleccionado = "masculino"
                                menuSexoExpandido = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Femenino") },
                            onClick = {
                                sexoSeleccionado = "femenino"
                                menuSexoExpandido = false
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Campo Peso (Kg)
                Text(
                    text = "Peso del paciente (Kg):",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = pesoTexto,
                    onValueChange = { nuevo ->
                        if (nuevo.all { it.isDigit() || it == '.' || it == ',' }) {
                            pesoTexto = nuevo
                        }
                    },
                    placeholder = { Text("Ej: 70") },
                    suffix = { Text("kg") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val peso = pesoTexto.replace(',', '.').toDoubleOrNull()

                    // Resolver urgencia final considerando texto escrito o selección
                    val query = textoUrgencia.trim().lowercase()
                    val coincidencia = opcionesProtocolos.find { p ->
                        p.id.lowercase() == query ||
                                p.nombre.lowercase() == query ||
                                p.nombre.lowercase().contains(query) ||
                                p.tagsBusqueda.any { it.lowercase().contains(query) }
                    }

                    val urgenciaFinal = coincidencia?.id ?: urgenciaSeleccionada.takeIf { it.isNotEmpty() }

                    if (urgenciaFinal != null && peso != null && peso > 0) {
                        onConfirmar(
                            datosIniciales.copy(
                                urgencia = urgenciaFinal,
                                sexo = sexoSeleccionado,
                                pesoKg = peso,
                                esValido = true,
                                confirmadoPorUsuario = true
                            )
                        )
                    }
                },
                enabled = textoUrgencia.isNotBlank() && (pesoTexto.replace(',', '.').toDoubleOrNull() ?: 0.0) > 0
            ) {
                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Aceptar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}
