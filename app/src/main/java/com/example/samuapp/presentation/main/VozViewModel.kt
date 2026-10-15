package com.example.samuapp.presentation.main

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.samuapp.domain.model.DatosPaciente
import com.example.samuapp.domain.service.TextoParserService
import com.example.samuapp.domain.service.WhisperService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class VozUiState(
    val isRecording: Boolean = false,
    val isProcessing: Boolean = false,
    val textoTranscrito: String = "",
    val datosPaciente: DatosPaciente? = null,
    val errorMessage: String? = null
)

@HiltViewModel
class VozViewModel @Inject constructor(
    application: Application,
    private val whisperService: WhisperService,
    private val textoParserService: TextoParserService
) : AndroidViewModel(application) {

    companion object {
        private const val TAG = "VozViewModel"
    }

    private val _uiState = MutableStateFlow(VozUiState())
    val uiState: StateFlow<VozUiState> = _uiState.asStateFlow()

    private var sessionTranscriptionId = 0
    private var recordingSessionId = 0
    private var inicioGrabacionJob: Job? = null

    init {
        // Precargar el modelo en segundo plano al iniciar
        viewModelScope.launch {
            whisperService.preCargarModelo()
        }
    }

    fun tienePermisoMicrofono(): Boolean {
        return whisperService.solicitarPermiso()
    }

    fun iniciarGrabacion() {
        inicioGrabacionJob = viewModelScope.launch {
            _uiState.update { it.copy(errorMessage = null) }
            val sessionId = ++recordingSessionId

            try {
                if (!whisperService.solicitarPermiso()) {
                    if (sessionId == recordingSessionId) {
                        _uiState.update {
                            it.copy(
                                errorMessage = "Se necesita permiso para usar el micrófono",
                                isRecording = false
                            )
                        }
                    }
                    return@launch
                }

                whisperService.iniciarGrabacion()

                // Si el usuario soltó o canceló mientras se iniciaba el hardware de audio:
                if (sessionId != recordingSessionId) {
                    Log.d(TAG, "Grabación cancelada durante inicio de audio. Deteniendo recorder.")
                    whisperService.cancelarTranscripcion()
                    _uiState.update { it.copy(isRecording = false) }
                    return@launch
                }

                _uiState.update { it.copy(isRecording = true) }
            } catch (error: Exception) {
                if (sessionId == recordingSessionId) {
                    _uiState.update {
                        it.copy(
                            errorMessage = "No se pudo iniciar la grabación: ${error.message}",
                            isRecording = false
                        )
                    }
                }
            }
        }
    }

    fun detenerYTranscribir() {
        viewModelScope.launch {
            inicioGrabacionJob?.join()

            val estaGrabandoHardware = whisperService.estaGrabando()
            if (!_uiState.value.isRecording && !estaGrabandoHardware) {
                Log.w(TAG, "detenerYTranscribir ignorado: no hay grabación activa")
                return@launch
            }

            _uiState.update {
                it.copy(
                    isRecording = false,
                    isProcessing = true,
                    errorMessage = null
                )
            }
            val currentSession = ++sessionTranscriptionId

            try {
                val texto = whisperService.detenerYTranscribir()

                // Si el usuario canceló mientras transcribía, descartamos el resultado
                if (currentSession != sessionTranscriptionId) {
                    Log.d(TAG, "Transcripción cancelada por el usuario.")
                    return@launch
                }

                Log.d(TAG, "Texto transcrito por Whisper: $texto")

                val startTimeParseo = System.currentTimeMillis()
                val datos = textoParserService.analizar(texto)
                val elapsedParseo = System.currentTimeMillis() - startTimeParseo

                Log.d(TAG, "Tiempo de parseo: $elapsedParseo ms")
                Log.d(
                    TAG,
                    "Datos parseados: urgencia=${datos.urgencia}, sexo=${datos.sexo}, pesoKg=${datos.pesoKg}, valido=${datos.esValido}"
                )

                _uiState.update {
                    it.copy(
                        textoTranscrito = texto,
                        datosPaciente = datos
                    )
                }
            } catch (error: Exception) {
                if (currentSession == sessionTranscriptionId) {
                    _uiState.update {
                        it.copy(errorMessage = "No se pudo transcribir el audio: ${error.message}")
                    }
                }
            } finally {
                if (currentSession == sessionTranscriptionId) {
                    _uiState.update { it.copy(isProcessing = false) }
                }
            }
        }
    }

    fun cancelarEntradaVoz() {
        recordingSessionId++ // Invalida cualquier iniciarGrabacion en curso inmediatamente
        sessionTranscriptionId++ // Invalida cualquier respuesta asíncrona de Whisper
        _uiState.update {
            it.copy(
                isProcessing = false,
                isRecording = false,
                errorMessage = null,
                datosPaciente = null,
                textoTranscrito = ""
            )
        }

        viewModelScope.launch {
            whisperService.cancelarTranscripcion()
        }
    }

    fun consumirDatosPaciente() {
        _uiState.update { it.copy(datosPaciente = null) }
    }

    fun limpiarError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    override fun onCleared() {
        super.onCleared()
        viewModelScope.launch {
            whisperService.liberarModelo()
        }
    }
}
