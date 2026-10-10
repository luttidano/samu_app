package com.example.samuapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.example.samuapp.data.repository.JsonProtocoloRepositoryImpl
import com.example.samuapp.domain.model.Protocolo
import com.example.samuapp.domain.model.ProtocoloIndice
import com.example.samuapp.presentation.config.ConfiguracionScreen
import com.example.samuapp.presentation.main.MainScreen
import com.example.samuapp.presentation.main.ProtocoloViewModel
import com.example.samuapp.presentation.protocolo.ProtocoloFlujoScreen
import com.example.samuapp.ui.theme.ModoTema
import com.example.samuapp.ui.theme.SamuAppTheme
import kotlinx.coroutines.launch

enum class PantallaActual {
    MAIN,
    CONFIGURACION,
    FLUJO_PROTOCOLO
}

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val repository = JsonProtocoloRepositoryImpl(applicationContext)
        val viewModel = ProtocoloViewModel(repository)

        setContent {
            var modoTema by remember { mutableStateOf(ModoTema.SISTEMA) }

            SamuAppTheme(modoTema = modoTema) {
                val coroutineScope = rememberCoroutineScope()
                val uiState by viewModel.uiState.collectAsState()
                var indices by remember { mutableStateOf<List<ProtocoloIndice>>(emptyList()) }
                var pantallaActual by remember { mutableStateOf(PantallaActual.MAIN) }

                var protocoloFlujoActual by remember { mutableStateOf<Protocolo?>(null) }
                var nodoInicialFlujo by remember { mutableStateOf<String?>(null) }

                LaunchedEffect(Unit) {
                    indices = repository.obtenerIndice()
                }

                // Manejo del botón Atrás de Android (equivalente al PopScope de Flutter)
                BackHandler(enabled = true) {
                    when {
                        pantallaActual != PantallaActual.MAIN -> {
                            pantallaActual = PantallaActual.MAIN
                        }
                        uiState.protocoloActivo != null -> {
                            viewModel.reiniciar()
                        }
                        else -> {
                            finish()
                        }
                    }
                }

                Surface(modifier = Modifier.fillMaxSize()) {
                    when (pantallaActual) {
                        PantallaActual.MAIN -> {
                            MainScreen(
                                viewModel = viewModel,
                                indices = indices,
                                onVerProtocoloFlujo = {
                                    val protocolo = uiState.protocoloActivo
                                    if (protocolo != null) {
                                        val urgencia = uiState.datosConfirmados?.urgencia?.lowercase()
                                        val nodoInicial = when {
                                            urgencia?.contains("hiper") == true -> "pregunta_alt_consciencia_hiper"
                                            urgencia?.contains("hipo") == true -> "pregunta_alt_consciencia_hipo"
                                            else -> null
                                        }
                                        protocoloFlujoActual = protocolo
                                        nodoInicialFlujo = nodoInicial
                                        pantallaActual = PantallaActual.FLUJO_PROTOCOLO
                                    }
                                },
                                onAbrirConfiguracion = {
                                    pantallaActual = PantallaActual.CONFIGURACION
                                },
                                onSeleccionarProtocoloCatalogo = { item ->
                                    coroutineScope.launch {
                                        val proto = repository.cargarProtocoloPorRuta(item.archivo)
                                        val idLower = item.id.lowercase()
                                        val nodoInicial = when {
                                            idLower == "hiperglicemia" -> "pregunta_alt_consciencia_hiper"
                                            idLower == "hipoglicemia" -> "nodo_abcde"
                                            else -> null
                                        }
                                        protocoloFlujoActual = proto
                                        nodoInicialFlujo = nodoInicial
                                        pantallaActual = PantallaActual.FLUJO_PROTOCOLO
                                    }
                                }
                            )
                        }

                        PantallaActual.CONFIGURACION -> {
                            ConfiguracionScreen(
                                modoTemaActual = modoTema,
                                onCambiarModoTema = { nuevoModo ->
                                    modoTema = nuevoModo
                                },
                                onVolver = {
                                    pantallaActual = PantallaActual.MAIN
                                }
                            )
                        }

                        PantallaActual.FLUJO_PROTOCOLO -> {
                            val protocolo = protocoloFlujoActual ?: uiState.protocoloActivo
                            if (protocolo != null) {
                                ProtocoloFlujoScreen(
                                    protocolo = protocolo,
                                    nodoInicialId = nodoInicialFlujo,
                                    onVolver = {
                                        pantallaActual = PantallaActual.MAIN
                                    }
                                )
                            } else {
                                pantallaActual = PantallaActual.MAIN
                            }
                        }
                    }
                }
            }
        }
    }
}
