package com.example.samuapp.presentation.protocolo

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.samuapp.domain.model.DiagramaProtocolo
import com.example.samuapp.domain.model.ElementoDiagrama
import com.example.samuapp.domain.model.Protocolo
import kotlin.math.abs
import kotlin.math.atan2

@Composable
fun VistaDiagramaCompleto(
    protocolo: Protocolo,
    modifier: Modifier = Modifier
) {
    val diagrama = protocolo.diagrama

    if (diagrama.elementos.isEmpty()) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Este protocolo aún no tiene diagrama configurado.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    val isDark = isSystemInDarkTheme()
    val canvasBgColor = if (isDark) MaterialTheme.colorScheme.background else Color(0xFFF5F5F5)
    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current

    val diagWidthPx = with(density) { diagrama.width.dp.toPx() }
    val diagHeightPx = with(density) { diagrama.height.dp.toPx() }

    var scale by remember { mutableFloatStateOf(0.85f) }
    var translation by remember { mutableStateOf(Offset.Zero) }
    var viewportSize by remember { mutableStateOf(IntSize.Zero) }
    var isInitialized by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .clipToBounds()
            .background(canvasBgColor)
            .onSizeChanged { size ->
                viewportSize = size
                if (!isInitialized && size.width > 0 && size.height > 0) {
                    val fitScale = (size.width.toFloat() / diagWidthPx).coerceIn(0.2f, 1.0f)
                    scale = fitScale
                    val initialX = (size.width.toFloat() - diagWidthPx * fitScale) / 2f
                    val initialY = with(density) { 20.dp.toPx() }
                    translation = Offset(initialX, initialY)
                    isInitialized = true
                }
            }
            .pointerInput(Unit) {
                detectTransformGestures { centroid, pan, zoom, _ ->
                    val oldScale = scale
                    val newScale = (scale * zoom).coerceIn(0.2f, 4f)
                    val zoomFactor = newScale / oldScale
                    // Física focal exacta: el punto bajo los dedos permanece 100% inmóvil en pantalla
                    translation = (translation - centroid) * zoomFactor + centroid + pan
                    scale = newScale
                }
            }
    ) {
        // El contenedor del lienzo se posiciona exactamente en TopStart (0, 0) sin ser restringido ni centrado por el padre
        Box(
            modifier = Modifier
                .wrapContentSize(align = Alignment.TopStart, unbounded = true)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    translationX = translation.x
                    translationY = translation.y
                    transformOrigin = TransformOrigin(0f, 0f)
                }
                .requiredSize(width = diagrama.width.dp, height = diagrama.height.dp)
        ) {
            // Capa 1: Dibujar conexiones, flechas y etiquetas
            Canvas(modifier = Modifier.fillMaxSize()) {
                val lineColor = Color(0xFF416FC0)

                for (conexion in diagrama.conexiones) {
                    val desde = diagrama.elementos.find { it.id == conexion.desde }
                    val hasta = diagrama.elementos.find { it.id == conexion.hasta }

                    if (desde == null || hasta == null) continue

                    val start = obtenerPuntoAncla(desde, conexion.salida)
                    val end = obtenerPuntoAncla(hasta, conexion.entrada)

                    dibujarConexion(
                        start = start,
                        end = end,
                        lineColor = lineColor
                    )

                    if (!conexion.etiqueta.isNullOrEmpty()) {
                        dibujarEtiqueta(
                            texto = conexion.etiqueta,
                            start = start,
                            end = end,
                            canvasColor = canvasBgColor,
                            isDark = isDark,
                            textMeasurer = textMeasurer
                        )
                    }
                }
            }

            // Capa 2: Elementos del diagrama posicionados por coordenadas absolutas
            for (elemento in diagrama.elementos) {
                Box(
                    modifier = Modifier
                        .offset(x = elemento.x.dp, y = elemento.y.dp)
                        .size(width = elemento.width.dp, height = elemento.height.dp)
                ) {
                    ElementoDiagramaCard(
                        elemento = elemento,
                        protocolo = protocolo,
                        canvasBgColor = canvasBgColor,
                        isDark = isDark
                    )
                }
            }
        }

        // Botones flotantes de Zoom (+, -, Reset) en la esquina inferior derecha
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SmallFloatingActionButton(
                onClick = {
                    val center = Offset(viewportSize.width / 2f, viewportSize.height / 2f)
                    val oldScale = scale
                    val newScale = (scale * 1.25f).coerceIn(0.2f, 4f)
                    val zoomFactor = newScale / oldScale
                    translation = (translation - center) * zoomFactor + center
                    scale = newScale
                },
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
            ) {
                Icon(Icons.Default.Add, contentDescription = "Acercar")
            }

            SmallFloatingActionButton(
                onClick = {
                    val center = Offset(viewportSize.width / 2f, viewportSize.height / 2f)
                    val oldScale = scale
                    val newScale = (scale * 0.8f).coerceIn(0.2f, 4f)
                    val zoomFactor = newScale / oldScale
                    translation = (translation - center) * zoomFactor + center
                    scale = newScale
                },
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
            ) {
                Icon(Icons.Default.Remove, contentDescription = "Alejar")
            }

            SmallFloatingActionButton(
                onClick = {
                    if (diagWidthPx > 0f && viewportSize.width > 0) {
                        val fitScale = (viewportSize.width.toFloat() / diagWidthPx).coerceIn(0.2f, 1.0f)
                        scale = fitScale
                        val initialX = (viewportSize.width.toFloat() - diagWidthPx * fitScale) / 2f
                        val initialY = with(density) { 20.dp.toPx() }
                        translation = Offset(initialX, initialY)
                    }
                },
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
            ) {
                Icon(Icons.Default.CenterFocusStrong, contentDescription = "Centrar")
            }
        }
    }
}

@Composable
private fun ElementoDiagramaCard(
    elemento: ElementoDiagrama,
    protocolo: Protocolo,
    canvasBgColor: Color,
    isDark: Boolean
) {
    val texto = obtenerTextoElemento(elemento, protocolo)
    val colorTexto = when (elemento.tipo) {
        "opcion" -> if (isDark) Color(0xFFEF5350) else Color(0xFFD32F2F)
        "amarillo" -> Color(0xFF212121)
        "alerta" -> if (isDark) Color(0xFFFFE082) else Color(0xFF5D4037)
        else -> if (isDark) Color.White else Color(0xFF212121)
    }

    when (elemento.tipo) {
        "opcion" -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .background(canvasBgColor)
                    .border(2.5.dp, if (isDark) Color(0xFFEF5350) else Color(0xFFD32F2F), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = texto,
                    color = colorTexto,
                    fontSize = elemento.fontSize.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(6.dp)
                )
            }
        }

        "amarillo" -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .shadow(elevation = 2.dp, shape = RoundedCornerShape(8.dp))
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFFFDD835))
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = texto,
                    color = colorTexto,
                    fontSize = elemento.fontSize.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center
                )
            }
        }

        "alerta" -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isDark) Color(0xFF4A3400) else Color(0xFFFFE59A))
                    .border(1.5.dp, Color(0xFFFFA000), RoundedCornerShape(8.dp))
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = texto,
                    color = colorTexto,
                    fontSize = elemento.fontSize.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center
                )
            }
        }

        "titulo" -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (isDark) Color(0xFF1E293B) else Color(0xFFE0E0E0))
                    .border(1.dp, if (isDark) Color(0xFF475569) else Color(0xFF9E9E9E), RoundedCornerShape(6.dp))
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = texto,
                    color = colorTexto,
                    fontSize = elemento.fontSize.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            }
        }

        else -> { // 'azul' o default
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isDark) Color(0xFF1E293B) else Color.White)
                    .border(3.dp, Color(0xFF416FC0), RoundedCornerShape(8.dp))
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = texto,
                    color = colorTexto,
                    fontSize = elemento.fontSize.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

private fun obtenerTextoElemento(elemento: ElementoDiagrama, protocolo: Protocolo): String {
    if (!elemento.texto.isNullOrBlank()) return elemento.texto

    if (!elemento.nodoId.isNullOrBlank()) {
        val nodo = protocolo.nodos[elemento.nodoId]
        if (nodo != null) {
            val partes = mutableListOf(nodo.texto)
            if (!nodo.instruccionExtra.isNullOrBlank()) {
                partes.add(nodo.instruccionExtra)
            }
            if (nodo.tipo == "droga" && !nodo.nombreDroga.isNullOrBlank()) {
                partes.add(
                    "${nodo.nombreDroga}\nDosis: ${nodo.dosisFormula ?: "No especificada"} ${nodo.unidad ?: ""}\nVía: ${nodo.via ?: "No especificada"}"
                )
            }
            return partes.joinToString("\n")
        }
    }

    return ""
}

private fun DrawScope.obtenerPuntoAncla(elemento: ElementoDiagrama, posicion: String): Offset {
    val x = elemento.x.dp.toPx()
    val y = elemento.y.dp.toPx()
    val w = elemento.width.dp.toPx()
    val h = elemento.height.dp.toPx()

    return when (posicion) {
        "izquierda" -> Offset(x, y + h / 2f)
        "derecha" -> Offset(x + w, y + h / 2f)
        "arriba" -> Offset(x + w / 2f, y)
        "abajo" -> Offset(x + w / 2f, y + h)
        else -> Offset(x + w / 2f, y + h)
    }
}

private fun DrawScope.dibujarConexion(start: Offset, end: Offset, lineColor: Color) {
    val strokeWidth = 3.dp.toPx()
    val path = Path()
    val horizontal = abs(start.y - end.y) < 20f

    val arrowAngle: Float
    if (horizontal) {
        path.moveTo(start.x, start.y)
        path.lineTo(end.x, end.y)
        arrowAngle = if (end.x >= start.x) 90f else -90f
    } else {
        val middleY = (start.y + end.y) / 2f
        path.moveTo(start.x, start.y)
        path.lineTo(start.x, middleY)
        path.lineTo(end.x, middleY)
        path.lineTo(end.x, end.y)
        arrowAngle = if (end.y >= middleY) 0f else 180f
    }

    drawPath(
        path = path,
        color = lineColor,
        style = Stroke(width = strokeWidth)
    )

    // Dibujar la punta de flecha
    dibujarFlecha(point = end, angleDegrees = arrowAngle, color = lineColor)
}

private fun DrawScope.dibujarFlecha(point: Offset, angleDegrees: Float, color: Color) {
    val arrowPath = Path().apply {
        moveTo(0f, 0f)
        lineTo(-8f, -16f)
        lineTo(8f, -16f)
        close()
    }

    withTransform({
        translate(point.x, point.y)
        rotate(degrees = angleDegrees)
    }) {
        drawPath(arrowPath, color = color)
    }
}

private fun DrawScope.dibujarEtiqueta(
    texto: String,
    start: Offset,
    end: Offset,
    canvasColor: Color,
    isDark: Boolean,
    textMeasurer: TextMeasurer
) {
    val isHorizontal = abs(start.y - end.y) < 20f
    val midpoint = if (isHorizontal) {
        Offset((start.x + end.x) / 2f, start.y)
    } else {
        val middleY = (start.y + end.y) / 2f
        Offset((start.x + end.x) / 2f, middleY)
    }

    val textColor = if (isDark) Color.White else Color(0xFF1E293B)
    val textResult = textMeasurer.measure(
        text = texto,
        style = TextStyle(
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = textColor
        )
    )

    val padX = 8f
    val padY = 4f
    val rectWidth = textResult.size.width + padX * 2
    val rectHeight = textResult.size.height + padY * 2

    // Fondo del label
    drawRoundRect(
        color = canvasColor,
        topLeft = Offset(midpoint.x - rectWidth / 2f, midpoint.y - rectHeight / 2f),
        size = Size(rectWidth, rectHeight),
        cornerRadius = CornerRadius(6f, 6f)
    )

    // Texto
    drawText(
        textLayoutResult = textResult,
        topLeft = Offset(midpoint.x - textResult.size.width / 2f, midpoint.y - textResult.size.height / 2f)
    )
}
