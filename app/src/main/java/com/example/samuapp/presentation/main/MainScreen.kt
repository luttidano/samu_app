package com.example.samuapp.presentation.main

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.AltRoute
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.outlined.Cancel
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.samuapp.domain.model.DatosPaciente
import com.example.samuapp.domain.model.ProtocoloIndice
import com.example.samuapp.presentation.components.BarraNavegacionInferior
import com.example.samuapp.presentation.components.BotonDictadoVoz
import com.example.samuapp.presentation.components.BuscarPacienteRunDialog
import com.example.samuapp.presentation.components.ConfirmacionDatosDialog
import com.example.samuapp.presentation.components.IndicadorCargaCircular
import com.example.samuapp.presentation.components.ListaProtocolosDialog
import com.example.samuapp.presentation.components.TarjetaAlertasProtocolo
import com.example.samuapp.presentation.components.TarjetaPreparacionProtocolo
import kotlinx.coroutines.launch

private val DatosPacienteNullableSaver = Saver<DatosPaciente?, Any>(
    save = { datos ->
        if (datos == null) {
            "__NULL__"
        } else {
            mapOf(
                "urgencia" to datos.urgencia,
                "sexo" to datos.sexo,
                "pesoKg" to datos.pesoKg,
                "esValido" to datos.esValido,
                "requiereConfirmacion" to datos.requiereConfirmacion,
                "motivos" to ArrayList(datos.motivos),
                "confirmadoPorUsuario" to datos.confirmadoPorUsuario
            )
        }
    },
    restore = { saved ->
        if (saved == "__NULL__") {
            null
        } else {
            @Suppress("UNCHECKED_CAST")
            val map = saved as Map<String, Any?>
            DatosPaciente(
                urgencia = map["urgencia"] as? String,
                sexo = map["sexo"] as? String,
                pesoKg = (map["pesoKg"] as? Number)?.toDouble(),
                esValido = (map["esValido"] as? Boolean) ?: false,
                requiereConfirmacion = (map["requiereConfirmacion"] as? Boolean) ?: true,
                motivos = (map["motivos"] as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
                confirmadoPorUsuario = (map["confirmadoPorUsuario"] as? Boolean) ?: false
            )
        }
    }
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: ProtocoloViewModel,
    vozViewModel: VozViewModel,
    indices: List<ProtocoloIndice>,
    onVerProtocoloFlujo: () -> Unit,
    onAbrirConfiguracion: () -> Unit,
    onSeleccionarProtocoloCatalogo: (ProtocoloIndice) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val vozUiState by vozViewModel.uiState.collectAsState()
    val hayResultado = !uiState.isLoading && uiState.protocoloActivo != null

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var mostrarDialogoValidacion by rememberSaveable { mutableStateOf(false) }
    var esValidacionManual by rememberSaveable { mutableStateOf(false) }
    var datosParaValidar by rememberSaveable(stateSaver = DatosPacienteNullableSaver) { mutableStateOf<DatosPaciente?>(null) }
    var mostrarCatalogo by rememberSaveable { mutableStateOf(false) }
    var mostrarDialogoBuscarRun by rememberSaveable { mutableStateOf(false) }

    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            vozViewModel.iniciarGrabacion()
        } else {
            scope.launch {
                snackbarHostState.showSnackbar("Se necesita permiso para usar el micrófono")
            }
        }
    }

    // Al recibir datos parseados por voz, abrir automáticamente el diálogo de confirmación
    LaunchedEffect(vozUiState.datosPaciente) {
        val datos = vozUiState.datosPaciente
        if (datos != null && !vozUiState.isProcessing) {
            datosParaValidar = datos
            esValidacionManual = false
            mostrarDialogoValidacion = true
            vozViewModel.consumirDatosPaciente()
        }
    }

    // Mostrar errores de voz si ocurren
    LaunchedEffect(vozUiState.errorMessage) {
        val error = vozUiState.errorMessage
        if (error != null) {
            snackbarHostState.showSnackbar(error)
            vozViewModel.limpiarError()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "VITA - asistente SAMU",
                        fontWeight = FontWeight.Bold,
                        fontSize = 19.sp
                    )
                },
                navigationIcon = {
                    if (hayResultado) {
                        IconButton(
                            onClick = { viewModel.reiniciar() }
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Volver a pantalla principal"
                            )
                        }
                    } else {
                        IconButton(onClick = onAbrirConfiguracion) {
                            Icon(
                                imageVector = Icons.Outlined.Settings,
                                contentDescription = "Configuración"
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.inversePrimary,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        bottomBar = {
            if (!hayResultado && !vozUiState.isProcessing && !uiState.isLoading) {
                BarraNavegacionInferior(
                    onAbrirProtocolos = { mostrarCatalogo = true },
                    onBuscarPacienteRun = { mostrarDialogoBuscarRun = true }
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (uiState.isLoading) {
                IndicadorCargaCircular(
                    mensaje = "Cargando protocolo...",
                    modifier = Modifier.align(Alignment.Center)
                )
            } else if (vozUiState.isProcessing) {
                // Vista idéntica a Flutter durante la transcripción y análisis de voz
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(48.dp),
                        strokeWidth = 4.dp
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Text(
                        text = "Transcribiendo y analizando voz...",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.W500,
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    OutlinedButton(
                        onClick = { vozViewModel.cancelarEntradaVoz() },
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color.Red
                        ),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.Red),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Cancel,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = Color.Red
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Cancelar entrada de voz",
                            fontWeight = FontWeight.Bold,
                            color = Color.Red
                        )
                    }
                }
            } else if (hayResultado) {
                // Vista con resultado activo (Preparación, Alertas y Botón de Flujo)
                val protocolo = uiState.protocoloActivo!!

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp)
                ) {
                    TarjetaPreparacionProtocolo(
                        protocolo = protocolo,
                        dosisPorNodo = uiState.dosisPorNodo
                    )

                    if (protocolo.alertasGenerales.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        TarjetaAlertasProtocolo(alertas = protocolo.alertasGenerales)
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Botón destacado idéntico a Flutter
                    Button(
                        onClick = onVerProtocoloFlujo,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        ),
                        contentPadding = PaddingValues(vertical = 14.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.AltRoute, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Ver Protocolo de Actuación",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                }
            } else {
                // Vista inicial idéntica a Flutter (Botón de voz central + Ingresar datos manualmente)
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    BotonDictadoVoz(
                        isRecording = vozUiState.isRecording,
                        isProcessing = vozUiState.isProcessing,
                        onIniciarGrabacion = {
                            if (vozViewModel.tienePermisoMicrofono()) {
                                vozViewModel.iniciarGrabacion()
                            } else {
                                micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            }
                        },
                        onDetenerYTranscribir = {
                            vozViewModel.detenerYTranscribir()
                        },
                        onCancelarGrabacion = {
                            vozViewModel.cancelarEntradaVoz()
                        },
                        onTapCortoAviso = {
                            scope.launch {
                                snackbarHostState.showSnackbar(
                                    message = "Mantén presionado para hablar o desliza ↑ para fijar.",
                                    duration = SnackbarDuration.Short
                                )
                            }
                        }
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    OutlinedButton(
                        onClick = {
                            datosParaValidar = DatosPaciente(
                                urgencia = null,
                                sexo = "masculino",
                                pesoKg = null,
                                esValido = false,
                                requiereConfirmacion = true
                            )
                            esValidacionManual = true
                            mostrarDialogoValidacion = true
                        },
                        enabled = !vozUiState.isProcessing,
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Keyboard,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Ingresar datos manualmente",
                            fontSize = 14.sp
                        )
                    }
                }
            }

            // Diálogo de Validar / Ingresar Datos Clínicos
            if (mostrarDialogoValidacion) {
                ConfirmacionDatosDialog(
                    datosIniciales = datosParaValidar ?: DatosPaciente(
                        urgencia = null,
                        sexo = "masculino",
                        pesoKg = null,
                        esValido = false,
                        requiereConfirmacion = true
                    ),
                    opcionesProtocolos = indices,
                    esEntradaManual = esValidacionManual,
                    onDismiss = {
                        mostrarDialogoValidacion = false
                        datosParaValidar = null
                    },
                    onConfirmar = { datosConfirmados ->
                        mostrarDialogoValidacion = false
                        datosParaValidar = null
                        viewModel.procesarPacienteConfirmado(datosConfirmados)
                    }
                )
            }

            // Diálogo de Catálogo de Protocolos Clínicos
            if (mostrarCatalogo) {
                ListaProtocolosDialog(
                    protocolos = indices,
                    onDismiss = { mostrarCatalogo = false },
                    onSeleccionarProtocolo = { seleccionado ->
                        mostrarCatalogo = false
                        onSeleccionarProtocoloCatalogo(seleccionado)
                    }
                )
            }

            // Diálogo de Búsqueda de Paciente por RUN chileno
            if (mostrarDialogoBuscarRun) {
                BuscarPacienteRunDialog(
                    onDismiss = { mostrarDialogoBuscarRun = false },
                    onBuscar = { runBuscado ->
                        scope.launch {
                            snackbarHostState.showSnackbar(
                                message = "Búsqueda de antecedentes para RUN: $runBuscado",
                                duration = SnackbarDuration.Short
                            )
                        }
                    }
                )
            }
        }
    }
}
