package com.example.samuapp.presentation.main

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.AltRoute
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.samuapp.domain.model.DatosPaciente
import com.example.samuapp.domain.model.ProtocoloIndice
import com.example.samuapp.presentation.components.BotonDictadoVoz
import com.example.samuapp.presentation.components.ConfirmacionDatosDialog
import com.example.samuapp.presentation.components.ListaProtocolosDialog
import com.example.samuapp.presentation.components.TarjetaAlertasProtocolo
import com.example.samuapp.presentation.components.TarjetaPreparacionProtocolo

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: ProtocoloViewModel,
    indices: List<ProtocoloIndice>,
    onVerProtocoloFlujo: () -> Unit,
    onAbrirConfiguracion: () -> Unit,
    onSeleccionarProtocoloCatalogo: (ProtocoloIndice) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val hayResultado = !uiState.isLoading && uiState.protocoloActivo != null

    var mostrarDialogoValidacion by remember { mutableStateOf(false) }
    var mostrarCatalogo by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Protocolos SAMU - Asistente",
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
        floatingActionButtonPosition = FabPosition.Start,
        floatingActionButton = {
            if (!hayResultado && !uiState.isLoading) {
                ExtendedFloatingActionButton(
                    onClick = { mostrarCatalogo = true },
                    icon = { Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = null) },
                    text = {
                        Text(
                            text = "Protocolos",
                            fontWeight = FontWeight.Bold
                        )
                    },
                    elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 3.dp),
                    shape = RoundedCornerShape(16.dp)
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
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
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
                        onClick = {
                            // Al tocar el botón de dictado abre el diálogo para validar datos clínicos
                            mostrarDialogoValidacion = true
                        }
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    OutlinedButton(
                        onClick = { mostrarDialogoValidacion = true },
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

            // Diálogo de Validar Datos Clínicos
            if (mostrarDialogoValidacion) {
                ConfirmacionDatosDialog(
                    datosIniciales = uiState.datosConfirmados ?: DatosPaciente(
                        urgencia = null,
                        sexo = "masculino",
                        pesoKg = null,
                        esValido = false,
                        requiereConfirmacion = true
                    ),
                    opcionesProtocolos = indices,
                    onDismiss = { mostrarDialogoValidacion = false },
                    onConfirmar = { datosConfirmados ->
                        mostrarDialogoValidacion = false
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
        }
    }
}
