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
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.samuapp.domain.model.ProtocoloIndice
import com.example.samuapp.domain.repository.PreferenciasRepository
import com.example.samuapp.domain.repository.ProtocoloRepository
import com.example.samuapp.presentation.config.ConfiguracionScreen
import com.example.samuapp.presentation.main.MainScreen
import com.example.samuapp.presentation.main.ProtocoloViewModel
import com.example.samuapp.presentation.main.VozViewModel
import com.example.samuapp.presentation.protocolo.ProtocoloFlujoScreen
import com.example.samuapp.ui.theme.ModoTema
import com.example.samuapp.ui.theme.SamuAppTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

object Destinos {
    const val MAIN = "main"
    const val CONFIGURACION = "configuracion"
    const val FLUJO_PROTOCOLO = "flujo_protocolo"
}

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var preferenciasRepository: PreferenciasRepository

    @Inject
    lateinit var protocoloRepository: ProtocoloRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            // Obtenemos el modo de tema guardado en DataStore reactivamente con Flow
            val modoTema by preferenciasRepository.modoTemaFlow.collectAsState(initial = ModoTema.SISTEMA)

            SamuAppTheme(modoTema = modoTema) {
                // Hilt inyecta automáticamente los ViewModels con su ciclo de vida correcto
                val viewModel: ProtocoloViewModel = hiltViewModel()
                val vozViewModel: VozViewModel = hiltViewModel()

                val navController = rememberNavController()
                val coroutineScope = rememberCoroutineScope()
                val uiState by viewModel.uiState.collectAsState()
                val vozUiState by vozViewModel.uiState.collectAsState()
                var indices by remember { mutableStateOf<List<ProtocoloIndice>>(emptyList()) }

                LaunchedEffect(Unit) {
                    indices = protocoloRepository.obtenerIndice()
                }

                BackHandler(enabled = true) {
                    when {
                        vozUiState.isProcessing -> {
                            vozViewModel.cancelarEntradaVoz()
                        }
                        navController.previousBackStackEntry != null -> {
                            navController.popBackStack()
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
                    NavHost(
                        navController = navController,
                        startDestination = Destinos.MAIN
                    ) {
                        composable(Destinos.MAIN) {
                            MainScreen(
                                viewModel = viewModel,
                                vozViewModel = vozViewModel,
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
                                        viewModel.seleccionarParaFlujo(protocolo, nodoInicial)
                                        navController.navigate(Destinos.FLUJO_PROTOCOLO)
                                    }
                                },
                                onAbrirConfiguracion = {
                                    navController.navigate(Destinos.CONFIGURACION)
                                },
                                onSeleccionarProtocoloCatalogo = { item ->
                                    coroutineScope.launch {
                                        val proto = protocoloRepository.cargarProtocoloPorRuta(item.archivo)
                                        val idLower = item.id.lowercase()
                                        val nodoInicial = when {
                                            idLower == "hiperglicemia" -> "pregunta_alt_consciencia_hiper"
                                            idLower == "hipoglicemia" -> "nodo_abcde"
                                            else -> null
                                        }
                                        viewModel.seleccionarParaFlujo(proto, nodoInicial)
                                        navController.navigate(Destinos.FLUJO_PROTOCOLO)
                                    }
                                }
                            )
                        }

                        composable(Destinos.CONFIGURACION) {
                            ConfiguracionScreen(
                                modoTemaActual = modoTema,
                                onCambiarModoTema = { nuevoModo ->
                                    coroutineScope.launch {
                                        preferenciasRepository.guardarModoTema(nuevoModo)
                                    }
                                },
                                onVolver = {
                                    navController.popBackStack()
                                }
                            )
                        }

                        composable(Destinos.FLUJO_PROTOCOLO) {
                            val protocolo = uiState.protocoloFlujo ?: uiState.protocoloActivo
                            if (protocolo != null) {
                                ProtocoloFlujoScreen(
                                    protocolo = protocolo,
                                    nodoInicialId = uiState.nodoInicialFlujo,
                                    onVolver = {
                                        navController.popBackStack()
                                    }
                                )
                            } else {
                                navController.popBackStack()
                            }
                        }
                    }
                }
            }
        }
    }
}
