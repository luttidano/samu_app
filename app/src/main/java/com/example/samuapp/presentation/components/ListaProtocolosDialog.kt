package com.example.samuapp.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.window.Dialog
import com.example.samuapp.domain.model.ProtocoloIndice

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import com.example.samuapp.ui.theme.LocalDarkTheme

@Composable
fun ListaProtocolosDialog(
    protocolos: List<ProtocoloIndice>,
    onDismiss: () -> Unit,
    onSeleccionarProtocolo: (ProtocoloIndice) -> Unit
) {
    var query by rememberSaveable { mutableStateOf("") }

    val filtrados = remember(query, protocolos) {
        if (query.isBlank()) protocolos
        else {
            val q = query.lowercase().trim()
            protocolos.filter { p ->
                p.nombre.lowercase().contains(q) ||
                        p.categoria.lowercase().contains(q) ||
                        p.tagsBusqueda.any { it.lowercase().contains(q) }
            }
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier.padding(18.dp)
            ) {
                // Encabezado
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.MenuBook,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Protocolos Clínicos",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Text(
                            text = "Paso a paso y mapa completo de actuación",
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Cerrar")
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Buscador de protocolos
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text("Buscar protocolo o palabra clave...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Estado de la lista para el cálculo del scrollbar
                val listState = rememberLazyListState()
                val isDark = LocalDarkTheme.current
                val scrollbarTrackColor = if (isDark) Color(0xFF334155).copy(alpha = 0.5f) else Color(0xFFE2E8F0)
                val scrollbarThumbColor = MaterialTheme.colorScheme.primary

                // Lista de protocolos con Scrollbar visible restringido a la zona de lista
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 380.dp)
                        .listaScrollbar(
                            state = listState,
                            totalItems = filtrados.size,
                            thumbColor = scrollbarThumbColor,
                            trackColor = scrollbarTrackColor,
                            barWidth = 6.dp,
                            paddingEnd = 2.dp
                        )
                ) {
                    if (filtrados.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 28.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "No se encontraron protocolos para \"$query\"",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else {
                        items(filtrados) { p ->
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .padding(end = 10.dp) // Margen derecho para no solapar el scrollbar
                                    .clickable { onSeleccionarProtocolo(p) },
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Text(
                                        text = p.nombre,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = p.categoria,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Modificador para dibujar un scrollbar visible con pista (track) y corredera (thumb)
 * que nunca desaparece a mitad del desplazamiento y tiene movimiento fluido sub-píxel.
 */
private fun Modifier.listaScrollbar(
    state: LazyListState,
    totalItems: Int,
    thumbColor: Color,
    trackColor: Color,
    barWidth: androidx.compose.ui.unit.Dp = 6.dp,
    paddingEnd: androidx.compose.ui.unit.Dp = 2.dp
): Modifier = this.drawWithContent {
    drawContent()

    val visibleItems = state.layoutInfo.visibleItemsInfo
    if (visibleItems.isEmpty()) return@drawWithContent

    // Se mantiene visible siempre que la lista pueda scrollear en cualquier dirección
    val canScroll = state.canScrollForward || state.canScrollBackward || totalItems > visibleItems.size
    if (!canScroll) return@drawWithContent

    val totalHeight = size.height
    val avgItemHeight = visibleItems.sumOf { it.size }.toFloat() / visibleItems.size
    val estimatedTotalHeight = avgItemHeight * totalItems
    val viewportHeight = (state.layoutInfo.viewportEndOffset - state.layoutInfo.viewportStartOffset)
        .toFloat().coerceAtLeast(1f)

    // Altura del thumb estable y proporcional (nunca salta bruscamente)
    val viewportRatio = (viewportHeight / estimatedTotalHeight.coerceAtLeast(viewportHeight)).coerceIn(0.18f, 1f)
    val thumbHeight = (totalHeight * viewportRatio).coerceIn(36.dp.toPx(), totalHeight)

    // Desplazamiento progresivo píxel a píxel continuo
    val currentOffset = (state.firstVisibleItemIndex * avgItemHeight) + state.firstVisibleItemScrollOffset
    val maxOffset = (estimatedTotalHeight - viewportHeight).coerceAtLeast(1f)
    val scrollProgress = (currentOffset / maxOffset).coerceIn(0f, 1f)

    val thumbOffsetY = scrollProgress * (totalHeight - thumbHeight)

    val barWidthPx = barWidth.toPx()
    val paddingEndPx = paddingEnd.toPx()
    val barX = size.width - barWidthPx - paddingEndPx
    val cornerRadius = CornerRadius(barWidthPx / 2f, barWidthPx / 2f)

    // Pista continua de fondo (Track)
    drawRoundRect(
        color = trackColor,
        topLeft = Offset(barX, 0f),
        size = Size(barWidthPx, totalHeight),
        cornerRadius = cornerRadius
    )

    // Píldora móvil del Scrollbar (Thumb)
    drawRoundRect(
        color = thumbColor,
        topLeft = Offset(barX, thumbOffsetY),
        size = Size(barWidthPx, thumbHeight),
        cornerRadius = cornerRadius
    )
}
