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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.ParagraphStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextIndent
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.samuapp.domain.model.DiagramaProtocolo
import com.example.samuapp.domain.model.ElementoDiagrama
import com.example.samuapp.domain.model.Protocolo
import com.example.samuapp.ui.theme.LocalDarkTheme
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

    val isDark = LocalDarkTheme.current
    val canvasBgColor = if (isDark) MaterialTheme.colorScheme.background else Color(0xFFF8FAFC)
    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current

    val (layoutList, totalHeight) = rememberDiagramaLayout(diagrama, protocolo, density, textMeasurer)
    val layoutMap = remember(layoutList) { layoutList.associateBy { it.elemento.id } }

    val diagWidthPx = with(density) { diagrama.width.dp.toPx() }
    val diagHeightPx = with(density) { totalHeight.toPx() }

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
                .requiredSize(width = diagrama.width.dp, height = totalHeight)
        ) {
            // Capa 1: Dibujar conexiones, flechas y etiquetas
            Canvas(modifier = Modifier.fillMaxSize()) {
                val lineColor = Color(0xFF2B6CB0)

                for (conexion in diagrama.conexiones) {
                    val desdeLayout = layoutMap[conexion.desde]
                    val hastaLayout = layoutMap[conexion.hasta]

                    if (desdeLayout == null || hastaLayout == null) continue

                    val start = obtenerPuntoAncla(layoutMap, conexion.desde, conexion.salida)
                    val end = obtenerPuntoAncla(
                        layoutMap = layoutMap,
                        elementoId = conexion.hasta,
                        posicion = conexion.entrada,
                        referenciaY = if (conexion.salida == "derecha" || conexion.salida == "izquierda") start.y else null
                    )

                    val esPcr = protocolo.id.contains("pcr", ignoreCase = true)

                    dibujarConexion(
                        start = start,
                        end = end,
                        salida = conexion.salida,
                        entrada = conexion.entrada,
                        lineColor = lineColor,
                        esPcr = esPcr
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

            // Capa 2: Elementos del diagrama adaptados dinámicamente al texto y sin solaparse
            for (layout in layoutList) {
                Box(
                    modifier = Modifier
                        .offset(x = layout.x, y = layout.y)
                        .size(width = layout.width, height = layout.height)
                ) {
                    ElementoDiagramaCard(
                        elemento = layout.elemento,
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

private fun esConceptoElemento(elemento: ElementoDiagrama, texto: String): Boolean {
    if (elemento.tipo == "opcion") return true

    val conceptosIds = setOf(
        "criterios_sospecha", "abcde", "iam_sin_sdst", "iam_con_sdst",
        "activar_codigo_iam", "hipo", "hiper", "glicemia", "glicemia_si", "glicemia_no",
        "mantiene_opcion_si", "mantiene_opcion_no", "signos_congestion_estable",
        "consultar_epa"
    )
    if (elemento.id in conceptosIds) return true

    val trimmed = texto.trim()
    if (trimmed.startsWith("¿") && trimmed.endsWith("?")) return true

    val tieneListado = texto.contains("▪") || 
        texto.contains("•") || 
        texto.contains("✓") || 
        texto.contains("\n-") || 
        Regex("""(?m)^\s*\d+[\.\)]\s+""").containsMatchIn(texto)

    return !tieneListado && texto.lines().size <= 3
}

private fun construirAnnotatedDiagramaString(
    text: String,
    fontSizeSp: androidx.compose.ui.unit.TextUnit,
    color: Color,
    defaultFontWeight: FontWeight,
    textAlign: TextAlign
): androidx.compose.ui.text.AnnotatedString {
    return buildAnnotatedString {
        val lines = text.split("\n")
        lines.forEachIndexed { index, rawLine ->
            val trimmed = rawLine.trim()
            if (trimmed.isEmpty()) {
                withStyle(ParagraphStyle(textAlign = textAlign)) {
                    append("")
                }
            } else {
                val numMatch = Regex("""^(\d+[\.\)])\s*(.*)$""").find(trimmed)
                val bulletMatch = Regex("""^([▪•✓\-\*✔☑])\s*(.*)$""").find(trimmed)

                val isSubItem = rawLine.startsWith("  ") || rawLine.startsWith("\t")
                val baseIndent = if (isSubItem) (fontSizeSp.value * 1.5f).sp else 0.sp
                val restIndent = if (isSubItem) (fontSizeSp.value * 2.8f).sp else (fontSizeSp.value * 1.35f).sp

                if (numMatch != null) {
                    val prefix = numMatch.groupValues[1]
                    val content = numMatch.groupValues[2]
                    withStyle(
                        ParagraphStyle(
                            textAlign = TextAlign.Start,
                            textIndent = TextIndent(firstLine = baseIndent, restLine = restIndent)
                        )
                    ) {
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = color)) {
                            append("$prefix ")
                        }
                        withStyle(SpanStyle(fontWeight = defaultFontWeight, color = color)) {
                            append(content)
                        }
                    }
                } else if (bulletMatch != null) {
                    val prefix = bulletMatch.groupValues[1]
                    val content = bulletMatch.groupValues[2]
                    withStyle(
                        ParagraphStyle(
                            textAlign = TextAlign.Start,
                            textIndent = TextIndent(firstLine = baseIndent, restLine = restIndent)
                        )
                    ) {
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = color)) {
                            append("$prefix ")
                        }
                        withStyle(SpanStyle(fontWeight = defaultFontWeight, color = color)) {
                            append(content)
                        }
                    }
                } else {
                    val isQuestion = trimmed.startsWith("¿") && trimmed.endsWith("?")
                    val isHeader = trimmed.endsWith(":") || trimmed.startsWith("OBJETIVO") || trimmed.startsWith("*Indicación")
                    val isCenteredLine = textAlign == TextAlign.Center || isQuestion
                    val lineAlign = if (isCenteredLine) TextAlign.Center else textAlign
                    val isLineBold = isHeader || isQuestion || defaultFontWeight == FontWeight.Bold

                    withStyle(
                        ParagraphStyle(
                            textAlign = lineAlign,
                            textIndent = TextIndent(firstLine = baseIndent, restLine = baseIndent)
                        )
                    ) {
                        withStyle(
                            SpanStyle(
                                fontWeight = if (isLineBold) FontWeight.Bold else defaultFontWeight,
                                color = color
                            )
                        ) {
                            append(trimmed)
                        }
                    }
                }
            }
            if (index < lines.size - 1) {
                append("\n")
            }
        }
    }
}

@Composable
private fun DiagramaText(
    text: String,
    color: Color,
    targetFontSize: Double,
    fontWeight: FontWeight,
    textAlign: TextAlign = TextAlign.Start,
    modifier: Modifier = Modifier
) {
    val fontSizeSp = (targetFontSize.coerceAtLeast(13.0) + 0.5).sp

    val annotatedText = remember(text, fontSizeSp, color, textAlign, fontWeight) {
        construirAnnotatedDiagramaString(
            text = text,
            fontSizeSp = fontSizeSp,
            color = color,
            defaultFontWeight = fontWeight,
            textAlign = textAlign
        )
    }

    Text(
        text = annotatedText,
        fontSize = fontSizeSp,
        lineHeight = (fontSizeSp.value * 1.38f).sp,
        textAlign = textAlign,
        softWrap = true,
        modifier = modifier
    )
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
        "opcion" -> if (isDark) Color(0xFFEF5350) else Color(0xFFC53030)
        "amarillo" -> Color(0xFF1A202C)
        "salmon" -> if (isDark) Color(0xFFFFEAE4) else Color(0xFF381005)
        "celeste" -> if (isDark) Color(0xFFEBF8FF) else Color(0xFF0A2540)
        "alerta" -> if (isDark) Color(0xFFFFE082) else Color(0xFF5D4037)
        else -> if (isDark) Color(0xFFF1F5F9) else Color(0xFF0F172A)
    }

    val esConcepto = esConceptoElemento(elemento, texto)
    val alineacionContenedor = if (esConcepto) Alignment.Center else Alignment.TopStart
    val alineacionTexto = if (esConcepto) TextAlign.Center else TextAlign.Start

    when (elemento.tipo) {
        "opcion" -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .background(if (isDark) Color(0xFF1E293B) else Color.White)
                    .border(2.5.dp, if (isDark) Color(0xFFEF5350) else Color(0xFFC53030), CircleShape)
                    .padding(4.dp),
                contentAlignment = Alignment.Center
            ) {
                DiagramaText(
                    text = texto,
                    color = colorTexto,
                    targetFontSize = elemento.fontSize,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            }
        }

        "amarillo" -> {
            val yellowGradient = if (elemento.id == "abcde") {
                Brush.verticalGradient(listOf(Color(0xFFFFD54F), Color(0xFFF57F17)))
            } else {
                Brush.verticalGradient(listOf(Color(0xFFFFF176), Color(0xFFFBC02D)))
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .shadow(elevation = 2.dp, shape = RoundedCornerShape(10.dp))
                    .clip(RoundedCornerShape(10.dp))
                    .background(yellowGradient)
                    .border(1.5.dp, Color(0xFFE65100), RoundedCornerShape(10.dp))
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                contentAlignment = alineacionContenedor
            ) {
                DiagramaText(
                    text = texto,
                    color = colorTexto,
                    targetFontSize = elemento.fontSize,
                    fontWeight = FontWeight.Bold,
                    textAlign = alineacionTexto,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        "salmon" -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .shadow(elevation = 2.dp, shape = RoundedCornerShape(10.dp))
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (isDark) Color(0xFF782E1E) else Color(0xFFE78267))
                    .border(1.5.dp, if (isDark) Color(0xFFA0422D) else Color(0xFFC65D43), RoundedCornerShape(10.dp))
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                DiagramaText(
                    text = texto,
                    color = colorTexto,
                    targetFontSize = elemento.fontSize,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        "celeste" -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .shadow(elevation = 2.dp, shape = RoundedCornerShape(10.dp))
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (isDark) Color(0xFF1E3A5F) else Color(0xFF75B5EA))
                    .border(2.dp, if (isDark) Color(0xFF63B3ED) else Color(0xFF2B6CB0), RoundedCornerShape(10.dp))
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                contentAlignment = alineacionContenedor
            ) {
                DiagramaText(
                    text = texto,
                    color = colorTexto,
                    targetFontSize = elemento.fontSize,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = alineacionTexto,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        "alerta" -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .shadow(elevation = 2.dp, shape = RoundedCornerShape(10.dp))
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (isDark) Color(0xFF5A4108) else Color(0xFFFFECB3))
                    .border(1.5.dp, Color(0xFFFFA000), RoundedCornerShape(10.dp))
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                contentAlignment = alineacionContenedor
            ) {
                DiagramaText(
                    text = texto,
                    color = colorTexto,
                    targetFontSize = elemento.fontSize,
                    fontWeight = FontWeight.Bold,
                    textAlign = alineacionTexto,
                    modifier = Modifier.fillMaxWidth()
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
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                DiagramaText(
                    text = texto,
                    color = colorTexto,
                    targetFontSize = elemento.fontSize,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        else -> { // 'azul' o default
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .shadow(elevation = 1.dp, shape = RoundedCornerShape(10.dp))
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (isDark) Color(0xFF1E293B) else Color.White)
                    .border(2.5.dp, Color(0xFF2B6CB0), RoundedCornerShape(10.dp))
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                contentAlignment = alineacionContenedor
            ) {
                DiagramaText(
                    text = texto,
                    color = colorTexto,
                    targetFontSize = elemento.fontSize,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = alineacionTexto,
                    modifier = Modifier.fillMaxWidth()
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

private data class ElementoLayout(
    val elemento: ElementoDiagrama,
    val x: Dp,
    val y: Dp,
    val width: Dp,
    val height: Dp
)

@Composable
private fun rememberDiagramaLayout(
    diagrama: DiagramaProtocolo,
    protocolo: Protocolo,
    density: androidx.compose.ui.unit.Density,
    textMeasurer: TextMeasurer
): Pair<List<ElementoLayout>, Dp> {
    return remember(diagrama, protocolo, density) {
        val factorY = 1.35f
        val paddingHorizontalPx = with(density) { 44.dp.toPx() }
        val paddingVerticalPx = with(density) { 32.dp.toPx() }
        val verticalBufferPx = with(density) { 42.dp.toPx() }

        // Paso 1: Medir la altura requerida por cada elemento garantizando legibilidad total
        val alturasRequeridas = diagrama.elementos.associate { elem ->
            val texto = obtenerTextoElemento(elem, protocolo)
            val fontSizeSp = (elem.fontSize.coerceAtLeast(13.0) + 0.5).sp
            val anchoTextoPx = (with(density) { elem.width.dp.toPx() } - paddingHorizontalPx).coerceAtLeast(40f)

            val esConcepto = esConceptoElemento(elem, texto)
            val alineacion = if (esConcepto) TextAlign.Center else TextAlign.Start

            val annotatedText = construirAnnotatedDiagramaString(
                text = texto,
                fontSizeSp = fontSizeSp,
                color = Color.Black,
                defaultFontWeight = FontWeight.SemiBold,
                textAlign = alineacion
            )

            val textResult = textMeasurer.measure(
                text = annotatedText,
                style = TextStyle(
                    fontSize = fontSizeSp,
                    lineHeight = (fontSizeSp.value * 1.38f).sp
                ),
                constraints = Constraints(maxWidth = anchoTextoPx.toInt())
            )

            val numLines = texto.lines().size
            val extraMultilineBufferPx = if (numLines > 3) with(density) { (numLines * 4).dp.toPx() } else 0f

            val alturaMinimaPx = with(density) { elem.height.dp.toPx() }
            val alturaCalculadaPx = textResult.size.height + paddingVerticalPx + verticalBufferPx + extraMultilineBufferPx
            val alturaFinalPx = maxOf(alturaMinimaPx, alturaCalculadaPx)
            elem.id to with(density) { alturaFinalPx.toDp() }
        }

        // Paso 2: Escalamiento proporcional de base
        val posicionesY = mutableMapOf<String, Dp>()
        for (elem in diagrama.elementos) {
            posicionesY[elem.id] = (elem.y * factorY).dp
        }

        // Paso 3: Relajación iterativa multidireccional hasta convergencia (garantía anti-solapamiento)
        val minBoxGap = 36.dp
        val minArrowGap = 44.dp
        val minArrowGapWithLabel = 54.dp

        var iteracion = 0
        var huboCambio = true
        while (huboCambio && iteracion < 50) {
            huboCambio = false
            iteracion++

            // Regla A: Conexiones de flujo directo hacia abajo (las flechas crecen en proporción)
            for (conexion in diagrama.conexiones) {
                val desde = diagrama.elementos.find { it.id == conexion.desde }
                val hasta = diagrama.elementos.find { it.id == conexion.hasta }
                if (desde != null && hasta != null) {
                    val esHaciaAbajo = if (hasta.y == desde.y) {
                        conexion.salida == "abajo" || conexion.entrada == "arriba"
                    } else {
                        conexion.salida != "derecha" && conexion.salida != "izquierda" && hasta.y > desde.y
                    }
                    if (esHaciaAbajo) {
                        val yDesde = posicionesY[desde.id] ?: (desde.y * factorY).dp
                        val hDesde = alturasRequeridas[desde.id] ?: desde.height.dp
                        val yHasta = posicionesY[hasta.id] ?: (hasta.y * factorY).dp
                        val gapRequerido = if (!conexion.etiqueta.isNullOrEmpty()) {
                            minArrowGapWithLabel
                        } else if (desde.tipo == "opcion" || hasta.tipo == "opcion") {
                            20.dp
                        } else {
                            minArrowGap
                        }
                        val yMinima = yDesde + hDesde + gapRequerido
                        if (yHasta < yMinima) {
                            posicionesY[hasta.id] = yMinima
                            huboCambio = true
                        }
                    }
                }
            }

            // Regla B: Separación espacial entre cuadros que comparten columna o franja horizontal
            for (i in 0 until diagrama.elementos.size) {
                val elemA = diagrama.elementos[i]
                val yA = posicionesY[elemA.id] ?: (elemA.y * factorY).dp
                val hA = alturasRequeridas[elemA.id] ?: elemA.height.dp

                for (j in 0 until diagrama.elementos.size) {
                    if (i == j) continue
                    val elemB = diagrama.elementos[j]

                    // Si elemB estaba originalmente debajo de elemA en el diagrama
                    if (elemB.y > elemA.y) {
                        val solapanX = elemB.x < (elemA.x + elemA.width) && (elemB.x + elemB.width) > elemA.x
                        if (solapanX) {
                            val yB = posicionesY[elemB.id] ?: (elemB.y * factorY).dp
                            val gap = if (elemA.tipo == "opcion" || elemB.tipo == "opcion") 20.dp else minBoxGap
                            val yMinima = yA + hA + gap
                            if (yB < yMinima) {
                                posicionesY[elemB.id] = yMinima
                                huboCambio = true
                            }
                        }
                    }
                }
            }

            // Regla C: Mantener alineadas únicamente bifurcaciones hermanas directas (mismo padre y mismo Y original)
            val gruposPorYOriginal = diagrama.elementos.groupBy { it.y }
            for ((_, grupo) in gruposPorYOriginal) {
                if (grupo.size > 1) {
                    val padresPorElemento = grupo.map { elem ->
                        diagrama.conexiones.filter { it.hasta == elem.id && it.salida != "izquierda" && it.salida != "derecha" }.map { it.desde }.toSet()
                    }
                    val compartenPadre = padresPorElemento.all { it.isNotEmpty() } && padresPorElemento.reduce { acc, s -> acc.intersect(s) }.isNotEmpty()
                    if (!compartenPadre) continue

                    val maxY = grupo.map { posicionesY[it.id] ?: (it.y * factorY).dp }.maxOrNull() ?: continue
                    for (elem in grupo) {
                        val currentY = posicionesY[elem.id] ?: (elem.y * factorY).dp
                        if (currentY < maxY) {
                            posicionesY[elem.id] = maxY
                            huboCambio = true
                        }
                    }
                }
            }

            // Regla D: Alineación vertical estricta para opciones de salida lateral (mantiene SI/NO centrados con su pregunta)
            for (conexion in diagrama.conexiones) {
                if (conexion.salida == "derecha" || conexion.salida == "izquierda") {
                    val desde = diagrama.elementos.find { it.id == conexion.desde }
                    val hasta = diagrama.elementos.find { it.id == conexion.hasta }
                    if (desde != null && hasta != null && hasta.tipo == "opcion") {
                        val yDesde = posicionesY[desde.id] ?: (desde.y * factorY).dp
                        val hDesde = alturasRequeridas[desde.id] ?: desde.height.dp
                        val hHasta = alturasRequeridas[hasta.id] ?: hasta.height.dp
                        val yAlineado = yDesde + (hDesde - hHasta) / 2
                        if (posicionesY[hasta.id] != yAlineado) {
                            posicionesY[hasta.id] = yAlineado
                            huboCambio = true
                        }
                    }
                }
            }

            // Regla E (Adaptación a protocolo PCR): Bajar preguntas de reevaluación (preg_desfib_r1 / r2)
            // para que su 'SI' se alinee con el choque (descarga_2 / 3) exclusivamente en PCR
            val esPcr = protocolo.id.contains("pcr", ignoreCase = true)
            if (esPcr) {
                for (conexion in diagrama.conexiones) {
                    if (conexion.salida == "izquierda" && conexion.entrada == "derecha") {
                        val opcion = diagrama.elementos.find { it.id == conexion.desde }
                        val destino = diagrama.elementos.find { it.id == conexion.hasta }
                        if (opcion != null && destino != null && opcion.tipo == "opcion") {
                            val padreConexion = diagrama.conexiones.find { it.hasta == opcion.id && it.salida == "izquierda" }
                            val padre = if (padreConexion != null) diagrama.elementos.find { it.id == padreConexion.desde } else null
                            if (padre != null) {
                                val yDestino = posicionesY[destino.id] ?: (destino.y * factorY).dp
                                val hDestino = alturasRequeridas[destino.id] ?: destino.height.dp
                                val hPadre = alturasRequeridas[padre.id] ?: padre.height.dp
                                val yDeseadoPadre = yDestino + (hDestino - hPadre) / 2
                                val yActualPadre = posicionesY[padre.id] ?: (padre.y * factorY).dp
                                if (yDeseadoPadre > yActualPadre) {
                                    posicionesY[padre.id] = yDeseadoPadre
                                    huboCambio = true
                                }
                            }
                        }
                    }
                }
            }
        }

        // Construir la lista final de layout
        val listaLayout = diagrama.elementos.map { elem ->
            ElementoLayout(
                elemento = elem,
                x = elem.x.dp,
                y = posicionesY[elem.id] ?: (elem.y * factorY).dp,
                width = elem.width.dp,
                height = alturasRequeridas[elem.id] ?: elem.height.dp
            )
        }

        val alturaMaxima = (listaLayout.maxOfOrNull { it.y + it.height } ?: (diagrama.height * factorY).dp) + 120.dp
        val alturaTotal = maxOf((diagrama.height * factorY).dp, alturaMaxima)

        Pair(listaLayout, alturaTotal)
    }
}

private fun DrawScope.obtenerPuntoAncla(
    layoutMap: Map<String, ElementoLayout>,
    elementoId: String,
    posicion: String,
    referenciaY: Float? = null
): Offset {
    val layout = layoutMap[elementoId] ?: return Offset.Zero
    val x = layout.x.toPx()
    val y = layout.y.toPx()
    val w = layout.width.toPx()
    val h = layout.height.toPx()

    return when (posicion) {
        "izquierda" -> {
            val yTarget = if (referenciaY != null && referenciaY >= (y + 16.dp.toPx()) && referenciaY <= (y + h - 16.dp.toPx())) {
                referenciaY
            } else {
                y + h / 2f
            }
            Offset(x, yTarget)
        }
        "derecha" -> {
            val yTarget = if (referenciaY != null && referenciaY >= (y + 16.dp.toPx()) && referenciaY <= (y + h - 16.dp.toPx())) {
                referenciaY
            } else {
                y + h / 2f
            }
            Offset(x + w, yTarget)
        }
        "arriba" -> Offset(x + w / 2f, y)
        "abajo" -> Offset(x + w / 2f, y + h)
        else -> Offset(x + w / 2f, y + h)
    }
}

private fun DrawScope.dibujarConexion(
    start: Offset, 
    end: Offset, 
    salida: String = "abajo",
    entrada: String = "arriba",
    lineColor: Color,
    esPcr: Boolean = false
) {
    val strokeWidth = 2.5.dp.toPx()
    val path = Path()

    val esBucleIzquierdo = salida == "izquierda" && entrada == "izquierda"
    val esBucleDerecho = (salida == "derecha" || salida == "arriba") && entrada == "derecha" && end.y < start.y
    // Adaptación a protocolo PCR: bypass por el espacio libre inferior
    val esByPassInferior = esPcr && salida == "abajo" && entrada == "abajo"
    val arrowAngle: Float
    if (esBucleIzquierdo) {
        // Bucle perimetral por el margen izquierdo (retorno a L1)
        val xOuter = (minOf(start.x, end.x) - 35.dp.toPx()).coerceAtLeast(15.dp.toPx())
        path.moveTo(start.x, start.y)
        path.lineTo(xOuter, start.y)
        path.lineTo(xOuter, end.y)
        path.lineTo(end.x, end.y)
        arrowAngle = -90f
    } else if (esBucleDerecho) {
        // Bucle perimetral por el margen derecho (retorno a Asistolia)
        val xOuter = maxOf(start.x, end.x) + 35.dp.toPx()
        path.moveTo(start.x, start.y)
        path.lineTo(xOuter, start.y)
        path.lineTo(xOuter, end.y)
        path.lineTo(end.x, end.y)
        arrowAngle = 90f
    } else if (esByPassInferior) {  
        // Bypass por el espacio libre inferior (conecta opc_no_l2 con el retorno derecho por debajo de las preguntas)
        val yBelow = maxOf(start.y, end.y) + 35.dp.toPx()
        path.moveTo(start.x, start.y)
        path.lineTo(start.x, yBelow)
        path.lineTo(end.x, yBelow)
        path.lineTo(end.x, end.y)
        arrowAngle = 180f // Flecha vertical hacia arriba apuntando a la base
    } else if (abs(start.y - end.y) < 16f) {
        // Línea horizontal completamente recta
        path.moveTo(start.x, start.y)
        path.lineTo(end.x, end.y)
        arrowAngle = if (end.x >= start.x) -90f else 90f
    } else if (abs(start.x - end.x) < 16f) {
        // Línea vertical completamente recta
        path.moveTo(start.x, start.y)
        path.lineTo(end.x, end.y)
        arrowAngle = if (end.y >= start.y) 0f else 180f
    } else if (salida == "derecha" || salida == "izquierda") {
        // Salida lateral: avanza horizontalmente primero hacia middleX
        // Adaptación a protocolo PCR: usamos el corredor libre (start.x - 25 dp) para no solapar la opción NO izquierda
        val middleX = if (esPcr && salida == "izquierda" && entrada == "derecha" && end.y < start.y) {
            start.x - 25.dp.toPx()
        } else {
            (start.x + end.x) / 2f
        }
        path.moveTo(start.x, start.y)
        path.lineTo(middleX, start.y)
        path.lineTo(middleX, end.y)
        path.lineTo(end.x, end.y)
        arrowAngle = if (end.x >= middleX) -90f else 90f
    } else {
        // Salida vertical: avanza verticalmente primero hacia middleY
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

    dibujarFlecha(point = end, angleDegrees = arrowAngle, color = lineColor)
}

private fun DrawScope.dibujarFlecha(point: Offset, angleDegrees: Float, color: Color) {
    val arrowLength = 14.dp.toPx()
    val halfWidth = 7.dp.toPx()

    val arrowPath = Path().apply {
        moveTo(0f, 0f)
        lineTo(-halfWidth, -arrowLength)
        lineTo(halfWidth, -arrowLength)
        close()
    }

    withTransform({
        translate(point.x, point.y)
        rotate(degrees = angleDegrees, pivot = Offset.Zero)
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

    val textColor = if (isDark) Color(0xFFF1F5F9) else Color(0xFF0F172A)
    val labelBgColor = if (isDark) Color(0xFF1E293B) else Color.White
    val labelBorderColor = if (isDark) Color(0xFF475569) else Color(0xFFCBD5E1)

    val textResult = textMeasurer.measure(
        text = texto,
        style = TextStyle(
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = textColor
        )
    )

    val padX = 10f
    val padY = 6f
    val rectWidth = textResult.size.width + padX * 2
    val rectHeight = textResult.size.height + padY * 2
    val topLeft = Offset(midpoint.x - rectWidth / 2f, midpoint.y - rectHeight / 2f)

    // Fondo del label
    drawRoundRect(
        color = labelBgColor,
        topLeft = topLeft,
        size = Size(rectWidth, rectHeight),
        cornerRadius = CornerRadius(6f, 6f)
    )

    drawRoundRect(
        color = labelBorderColor,
        topLeft = topLeft,
        size = Size(rectWidth, rectHeight),
        cornerRadius = CornerRadius(6f, 6f),
        style = Stroke(width = 1.5f)
    )

    // Texto
    drawText(
        textLayoutResult = textResult,
        topLeft = Offset(midpoint.x - textResult.size.width / 2f, midpoint.y - textResult.size.height / 2f)
    )
}
