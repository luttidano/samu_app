package com.example.samuapp.presentation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

enum class EstadoBotonVoz {
    INACTIVO,
    PRESIONADO,
    BLOQUEADO
}

@Composable
fun BotonDictadoVoz(
    isRecording: Boolean,
    isProcessing: Boolean,
    onIniciarGrabacion: () -> Unit,
    onDetenerYTranscribir: () -> Unit,
    onCancelarGrabacion: () -> Unit,
    onTapCortoAviso: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current

    val currentOnIniciarGrabacion by rememberUpdatedState(onIniciarGrabacion)
    val currentOnDetenerYTranscribir by rememberUpdatedState(onDetenerYTranscribir)
    val currentOnCancelarGrabacion by rememberUpdatedState(onCancelarGrabacion)
    val currentOnTapCortoAviso by rememberUpdatedState(onTapCortoAviso)

    var estado by remember { mutableStateOf(EstadoBotonVoz.INACTIVO) }
    var dragOffsetY by remember { mutableFloatStateOf(0f) }
    var momentoInicio by remember { mutableLongStateOf(0L) }
    var bloqueadoEnEsteGesto by remember { mutableStateOf(false) }

    val umbralBloqueoPx = with(density) { -80.dp.toPx() }
    val estaGrabando = isRecording || estado != EstadoBotonVoz.INACTIVO

    // Si el estado externo se detiene por completo, sincronizar
    LaunchedEffect(isRecording, isProcessing) {
        if (!isRecording && !isProcessing && estado != EstadoBotonVoz.INACTIVO) {
            estado = EstadoBotonVoz.INACTIVO
            dragOffsetY = 0f
            bloqueadoEnEsteGesto = false
        }
    }

    // Animación de ondas expansivas
    val infiniteTransition = rememberInfiniteTransition(label = "wave_anim")
    val waveProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave_progress"
    )

    // Animación del tamaño del botón central
    val tamanoBoton by animateDpAsState(
        targetValue = if (estaGrabando) 138.dp else 110.dp,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "tamano_boton"
    )

    val colorBoton by animateColorAsState(
        targetValue = if (estaGrabando) Color(0xFFD32F2F) else MaterialTheme.colorScheme.primary,
        animationSpec = tween(durationMillis = 200),
        label = "color_boton"
    )

    val estaEnUmbralBloqueo = dragOffsetY <= umbralBloqueoPx

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Indicador superior dinámico: Desliza para fijar / Candado
        Column(
            modifier = Modifier.height(52.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            AnimatedVisibility(
                visible = estado == EstadoBotonVoz.PRESIONADO,
                enter = fadeIn(tween(150)),
                exit = fadeOut(tween(150))
            ) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = if (estaEnUmbralBloqueo) Color.Red.copy(alpha = 0.15f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (estaEnUmbralBloqueo) Color.Red else MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (estaEnUmbralBloqueo) Icons.Filled.Lock else Icons.Outlined.Lock,
                            contentDescription = null,
                            tint = if (estaEnUmbralBloqueo) Color.Red else MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (estaEnUmbralBloqueo) "¡Suelta para fijar!" else "Desliza ↑ para fijar",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (estaEnUmbralBloqueo) Color.Red else MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            AnimatedVisibility(
                visible = estado == EstadoBotonVoz.BLOQUEADO,
                enter = fadeIn(tween(150)),
                exit = fadeOut(tween(150))
            ) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color.Red.copy(alpha = 0.15f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.Red.copy(alpha = 0.6f)),
                    modifier = Modifier.clickable {
                        estado = EstadoBotonVoz.INACTIVO
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onDetenerYTranscribir()
                    }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Lock,
                            contentDescription = null,
                            tint = Color.Red,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Grabación fijada (Toca para finalizar)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Red
                        )
                    }
                }
            }
        }

        // Área interactiva de 300dp con ondas concéntricas desprendiéndose
        Box(
            modifier = Modifier.size(300.dp),
            contentAlignment = Alignment.Center
        ) {
            // Capa de ondas concéntricas
            if (estaGrabando) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val center = Offset(size.width / 2f, size.height / 2f)
                    val buttonRadius = (138.dp / 2f).toPx()
                    val maxExpansion = 76.dp.toPx()

                    for (i in 0 until 3) {
                        val p = (waveProgress + (i * 0.33f)) % 1.0f
                        // Curva cúbica desacelerada
                        val cubicP = 1.0f - (1.0f - p) * (1.0f - p) * (1.0f - p)
                        val radius = buttonRadius + (cubicP * maxExpansion)
                        val alpha = (1.0f - p).coerceIn(0f, 1f)

                        // 1. Relleno suave
                        drawCircle(
                            color = Color.Red.copy(alpha = alpha * 0.08f),
                            radius = radius,
                            center = center
                        )

                        // 2. Halo difuso
                        drawCircle(
                            color = Color.Red.copy(alpha = alpha * 0.20f),
                            radius = radius,
                            center = center,
                            style = Stroke(width = (5.dp.toPx() * (1f - p * 0.4f)))
                        )

                        // 3. Contorno nítido que viaja
                        drawCircle(
                            color = Color.Red.copy(alpha = (alpha * 0.85f).coerceIn(0f, 1f)),
                            radius = radius,
                            center = center,
                            style = Stroke(width = (3.dp.toPx() * (1f - p * 0.6f)).coerceAtLeast(1.5f))
                        )
                    }
                }
            }

            // Botón central con detección táctil de WhatsApp
            Box(
                modifier = Modifier
                    .size(tamanoBoton)
                    .shadow(
                        elevation = if (estaGrabando) 16.dp else 8.dp,
                        shape = CircleShape,
                        ambientColor = colorBoton.copy(alpha = if (estaGrabando) 0.5f else 0.3f),
                        spotColor = colorBoton.copy(alpha = if (estaGrabando) 0.5f else 0.3f)
                    )
                    .clip(CircleShape)
                    .background(colorBoton)
                    .pointerInput(Unit) {
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            val pointerId = down.id
                            val pointerStartY = down.position.y
                            val startTime = System.currentTimeMillis()

                            if (estado == EstadoBotonVoz.BLOQUEADO) {
                                // En estado fijado, esperar a que levante el dedo de este toque para detener
                                while (true) {
                                    val event = awaitPointerEvent()
                                    val change = event.changes.firstOrNull { it.id == pointerId }
                                    if (change == null || !change.pressed) {
                                        break
                                    }
                                }
                                estado = EstadoBotonVoz.INACTIVO
                                dragOffsetY = 0f
                                bloqueadoEnEsteGesto = false
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                currentOnDetenerYTranscribir()
                                return@awaitEachGesture
                            }

                            // Nuevo toque para iniciar grabación
                            bloqueadoEnEsteGesto = false
                            momentoInicio = startTime
                            estado = EstadoBotonVoz.PRESIONADO
                            dragOffsetY = 0f
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            currentOnIniciarGrabacion()

                            var isCancelled = false

                            while (true) {
                                val event = awaitPointerEvent()
                                val change = event.changes.firstOrNull { it.id == pointerId }

                                if (change == null || !change.pressed) {
                                    // El usuario soltó el dedo
                                    break
                                }

                                if (change.isConsumed) {
                                    isCancelled = true
                                    break
                                }

                                val deltaY = change.position.y - pointerStartY
                                dragOffsetY = deltaY

                                if (estado == EstadoBotonVoz.PRESIONADO && deltaY <= umbralBloqueoPx) {
                                    // Superó el umbral hacia arriba -> BLOQUEAR / FIJAR
                                    estado = EstadoBotonVoz.BLOQUEADO
                                    bloqueadoEnEsteGesto = true
                                    dragOffsetY = 0f
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                }
                            }

                            if (isCancelled) {
                                estado = EstadoBotonVoz.INACTIVO
                                dragOffsetY = 0f
                                bloqueadoEnEsteGesto = false
                                currentOnCancelarGrabacion()
                            } else if (estado == EstadoBotonVoz.BLOQUEADO) {
                                if (bloqueadoEnEsteGesto) {
                                    // Acaba de soltar tras deslizar para fijar: CONTINUA grabando manos libres
                                    bloqueadoEnEsteGesto = false
                                } else {
                                    estado = EstadoBotonVoz.INACTIVO
                                    dragOffsetY = 0f
                                    currentOnDetenerYTranscribir()
                                }
                            } else if (estado == EstadoBotonVoz.PRESIONADO) {
                                val duracionMs = System.currentTimeMillis() - startTime
                                estado = EstadoBotonVoz.INACTIVO
                                dragOffsetY = 0f

                                if (duracionMs < 450) {
                                    // Pulsación corta: descartar con aviso
                                    currentOnCancelarGrabacion()
                                    currentOnTapCortoAviso()
                                } else {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    currentOnDetenerYTranscribir()
                                }
                            }
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                if (isProcessing) {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.size(36.dp),
                        strokeWidth = 3.dp
                    )
                } else {
                    Icon(
                        imageVector = if (estado == EstadoBotonVoz.BLOQUEADO) Icons.Filled.Stop else Icons.Filled.Mic,
                        contentDescription = "Dictar por voz",
                        tint = Color.White,
                        modifier = Modifier.size(if (estaGrabando) 58.dp else 46.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Subtítulo inferior o botón de cancelar audio cuando está fijado
        if (estado == EstadoBotonVoz.BLOQUEADO) {
            TextButton(
                onClick = {
                    estado = EstadoBotonVoz.INACTIVO
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onCancelarGrabacion()
                },
                colors = ButtonDefaults.textButtonColors(contentColor = Color.Red)
            ) {
                Icon(
                    imageVector = Icons.Default.DeleteOutline,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Cancelar audio",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
        } else {
            Text(
                text = when {
                    isProcessing -> "Transcribiendo audio clínico con Whisper..."
                    estaGrabando -> "Grabando audio clínico..."
                    else -> "Mantén presionado para hablar"
                },
                fontSize = 15.sp,
                fontWeight = FontWeight.W500,
                color = if (estaGrabando) Color.Red else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
