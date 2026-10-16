package com.example.samuapp.presentation.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.samuapp.domain.model.*
import com.example.samuapp.domain.repository.ProtocoloRepository
import com.example.samuapp.domain.service.CalculoDosisService
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

data class ProtocoloUiState(
    val protocoloActivo: Protocolo? = null,
    val calculoDosis: CalculoDosis? = null,
    val dosisPorNodo: Map<String, CalculoDosis> = emptyMap(),
    val datosConfirmados: DatosPaciente? = null,
    val protocoloFlujo: Protocolo? = null,
    val nodoInicialFlujo: String? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class ProtocoloViewModel @Inject constructor(
    private val protocoloRepository: ProtocoloRepository,
    private val calculoDosisService: CalculoDosisService
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProtocoloUiState())
    val uiState: StateFlow<ProtocoloUiState> = _uiState.asStateFlow()

    fun procesarPacienteConfirmado(datos: DatosPaciente) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    error = null,
                    protocoloActivo = null,
                    calculoDosis = null,
                    datosConfirmados = datos
                )
            }

            try {
                if (datos.urgencia == null || datos.pesoKg == null) {
                    throw IllegalArgumentException("Se requiere urgencia y peso")
                }

                val protocolo = protocoloRepository.buscarPorUrgencia(datos.urgencia)
                    ?: throw IllegalStateException("No existe un protocolo para ${datos.urgencia}")

                val dosisMap = mutableMapOf<String, CalculoDosis>()

                // Calcular para medicamentos de preparación de salida
                for (med in protocolo.preparacionSalida.medicamentos) {
                    val nodoDroga = protocolo.obtenerNodoDroga(med.nodoId)
                    if (nodoDroga != null) {
                        dosisMap[med.nodoId] = calculoDosisService.calcularDosis(
                            pesoKg = datos.pesoKg,
                            nodoDroga = nodoDroga,
                            alertasProtocolo = protocolo.alertasGenerales
                        )
                    }
                }

                // Si no hay en salida, buscar en los nodos de tipo droga
                if (dosisMap.isEmpty()) {
                    for ((id, nodo) in protocolo.nodos) {
                        if (nodo.tipo == "droga") {
                            val nodoDroga = protocolo.obtenerNodoDroga(id)
                            if (nodoDroga != null) {
                                dosisMap[id] = calculoDosisService.calcularDosis(
                                    pesoKg = datos.pesoKg,
                                    nodoDroga = nodoDroga,
                                    alertasProtocolo = protocolo.alertasGenerales
                                )
                            }
                        }
                    }
                }

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        protocoloActivo = protocolo,
                        dosisPorNodo = dosisMap,
                        calculoDosis = dosisMap.values.firstOrNull()
                    )
                }

            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "Error al calcular la dosis: ${e.message}"
                    )
                }
            }
        }
    }

    fun seleccionarParaFlujo(protocolo: Protocolo, nodoInicial: String? = null) {
        _uiState.update {
            it.copy(
                protocoloFlujo = protocolo,
                nodoInicialFlujo = nodoInicial
            )
        }
    }

    fun setCargando(cargando: Boolean) {
        _uiState.update { it.copy(isLoading = cargando) }
    }

    fun reiniciar() {
        _uiState.value = ProtocoloUiState()
    }
}
