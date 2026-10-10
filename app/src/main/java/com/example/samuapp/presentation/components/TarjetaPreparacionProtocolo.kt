package com.example.samuapp.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.samuapp.domain.model.CalculoDosis
import com.example.samuapp.domain.model.Protocolo

@Composable
fun TarjetaPreparacionProtocolo(
    protocolo: Protocolo,
    dosisPorNodo: Map<String, CalculoDosis>,
    modifier: Modifier = Modifier
) {
    val preparacion = protocolo.preparacionSalida
    val totalItems = preparacion.insumosGenerales.size + preparacion.medicamentos.size

    if (totalItems == 0) return

    val marcados = remember(protocolo.id) { mutableStateMapOf<String, Boolean>() }
    val marcadosCount = marcados.values.count { it }
    val completo = marcadosCount == totalItems
    val progreso = if (totalItems > 0) marcadosCount.toFloat() / totalItems.toFloat() else 0f

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(
                if (completo) Color(0xFF66BB6A)
                else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
            )
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Encabezado principal
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            if (completo) Color(0xFF2E7D32).copy(alpha = 0.18f)
                            else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (completo) Icons.Default.CheckCircle else Icons.Default.Inventory2,
                        contentDescription = null,
                        tint = if (completo) Color(0xFF2E7D32) else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Checklist antes de salir",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Text(
                        text = if (completo) "Todo verificado para despacho"
                        else "$marcadosCount de $totalItems elementos verificados",
                        style = MaterialTheme.typography.bodySmall,
                        fontSize = 12.5.sp,
                        color = if (completo) Color(0xFF2E7D32) else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = if (completo) FontWeight.SemiBold else FontWeight.Normal
                    )
                }

                // Badge de contador
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (completo) Color(0xFFE8F5E9)
                            else MaterialTheme.colorScheme.surfaceVariant
                        )
                        .border(
                            0.8.dp,
                            if (completo) Color(0xFF81C784) else MaterialTheme.colorScheme.outlineVariant,
                            RoundedCornerShape(12.dp)
                        )
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "$marcadosCount / $totalItems",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (completo) Color(0xFF1B5E20) else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Barra de progreso
            LinearProgressIndicator(
                progress = { progreso },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(7.dp)
                    .clip(RoundedCornerShape(6.dp)),
                color = if (completo) Color(0xFF2E7D32) else MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Sección 1: Medicamentos y Dosis Calculadas
            if (preparacion.medicamentos.isNotEmpty()) {
                Text(
                    text = "Medicamentos y Dosis Calculadas",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))

                preparacion.medicamentos.forEach { med ->
                    val calculo = dosisPorNodo[med.nodoId]
                    val isChecked = marcados[med.nodoId] == true

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { marcados[med.nodoId] = !isChecked }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = isChecked,
                            onCheckedChange = { marcados[med.nodoId] = it }
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = calculo?.sustancia ?: med.nodoId,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                            if (calculo != null) {
                                Text(
                                    text = calculo.detalleEmpaque ?: calculo.instruccionClinica,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "Vía: ${calculo.via} | Dosis: ${calculo.instruccionClinica}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Sección 2: Insumos Generales
            if (preparacion.insumosGenerales.isNotEmpty()) {
                Text(
                    text = "Insumos Generales",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))

                preparacion.insumosGenerales.forEach { insumo ->
                    val isChecked = marcados[insumo.id] == true

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { marcados[insumo.id] = !isChecked }
                            .padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = isChecked,
                            onCheckedChange = { marcados[insumo.id] = it }
                        )
                        Text(
                            text = insumo.nombre,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
    }
}
