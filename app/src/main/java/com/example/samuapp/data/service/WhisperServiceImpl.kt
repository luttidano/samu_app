package com.example.samuapp.data.service

import android.content.Context
import android.util.Log
import com.example.samuapp.data.repository.ModeloRepository
import com.example.samuapp.domain.service.WhisperService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.*
import java.io.File

class WhisperServiceImpl(
    private val context: Context,
    private val modeloRepository: ModeloRepository = ModeloRepository(context),
    val audioRecorderService: AudioRecorderService = AudioRecorderService(context)
) : WhisperService {

    companion object {
        private const val TAG = "WhisperService"
        private const val INITIAL_PROMPT =
            "Atencion prehospitalaria SAMU urgencias: " +
            "Paciente con hipoglicemia, hiperglicemia, edema agudo de pulmón (EPA)" +
            "síndrome coronario agudo, infarto agudo al miocardio (IAM), disnea" +
            "convulsión, status convulsivo, crisis convulsiva, epilepsia " +
            " paro cardiorrespiratorio. Signos vitales, peso en kilos, sexo masculino o femenino"
    }

    private val jsonParser = Json { ignoreUnknownKeys = true }

    override suspend fun preCargarModelo() {
        withContext(Dispatchers.IO) {
            val startTime = System.currentTimeMillis()
            try {
                val modelPath = modeloRepository.obtenerRutaModelo()
                val elapsed = System.currentTimeMillis() - startTime
                Log.d(TAG, "Preparación del modelo completada en $elapsed ms ($modelPath)")
            } catch (error: Exception) {
                val elapsed = System.currentTimeMillis() - startTime
                Log.e(TAG, "Error al precargar el modelo Whisper después de $elapsed ms: ${error.message}", error)
            }
        }
    }

    override fun solicitarPermiso(): Boolean {
        return audioRecorderService.tienePermiso()
    }

    override suspend fun iniciarGrabacion(): File {
        return audioRecorderService.iniciarGrabacion()
    }

    override suspend fun detenerYTranscribir(): String = withContext(Dispatchers.IO) {
        val duracionGrabacion = audioRecorderService.detenerGrabacion()
        Log.d(TAG, "Duración de audio grabado: $duracionGrabacion ms")

        val audioFile = audioRecorderService.obtenerArchivoAudio()
            ?: throw IllegalStateException("No se encontró el archivo de audio grabado")

        if (!audioFile.exists() || audioFile.length() <= 44) {
            throw IllegalStateException("El archivo de audio está vacío o corrupto")
        }

        val modelPath = modeloRepository.obtenerRutaModelo()

        val requestObj = buildJsonObject {
            put("@type", "getTextFromWavFile")
            put("model", modelPath)
            put("audio", audioFile.absolutePath)
            put("language", "es")
            put("threads", 4)
            put("is_verbose", false)
            put("is_translate", false)
            put("is_special_tokens", false)
            put("is_no_timestamps", true)
            put("split_on_word", false)
            put("diarize", false)
            put("keep_model_loaded", true)
            put("initial_prompt", INITIAL_PROMPT)
        }

        val startTime = System.currentTimeMillis()
        try {
            val rawResponse = WhisperNativeBridge.request(requestObj.toString())
            val elapsed = System.currentTimeMillis() - startTime

            val responseObj = jsonParser.parseToJsonElement(rawResponse).jsonObject
            val type = responseObj["@type"]?.jsonPrimitive?.contentOrNull

            if (type == "error") {
                val errorMsg = responseObj["message"]?.jsonPrimitive?.contentOrNull ?: "Error desconocido"
                if (errorMsg.contains("aborted", ignoreCase = true)) {
                    Log.d(TAG, "Transcripción interrumpida limpiamente por abort_callback en C++")
                    return@withContext ""
                }
                throw IllegalStateException("Error nativo en Whisper: $errorMsg")
            }

            val text = responseObj["text"]?.jsonPrimitive?.contentOrNull?.trim() ?: ""

            Log.d(TAG, "Transcripcion completada en $elapsed ms")
            Log.d(TAG, "Texto transcrito por Whisper: $text")

            text
        } catch (e: Exception) {
            val elapsed = System.currentTimeMillis() - startTime
            if (e.message?.contains("aborted", ignoreCase = true) == true) {
                Log.d(TAG, "Transcripción interrumpida limpiamente por abort_callback")
                return@withContext ""
            }
            Log.e(TAG, "Error durante la transcripción tras $elapsed ms: ${e.message}", e)
            throw e
        }
    }

    override suspend fun cancelarTranscripcion() {
        withContext(Dispatchers.IO) {
            val startTime = System.currentTimeMillis()
            try {
                audioRecorderService.cancelarGrabacion()
            } catch (e: Exception) {
                Log.e(TAG, "Error al cancelar audio recorder: ${e.message}")
            }

            try {
                WhisperNativeBridge.abort()
            } catch (e: Exception) {
                Log.e(TAG, "Error al abortar whisper: ${e.message}")
            }

            val elapsed = System.currentTimeMillis() - startTime
            Log.d(TAG, "Cancelación y liberación de Whisper completada en $elapsed ms")
        }
    }

    override suspend fun liberarModelo() {
        withContext(Dispatchers.IO) {
            try {
                val requestObj = buildJsonObject {
                    put("@type", "releaseModel")
                }
                WhisperNativeBridge.request(requestObj.toString())
                Log.d(TAG, "Modelo Whisper liberado de memoria")
            } catch (e: Exception) {
                Log.e(TAG, "Error al liberar modelo: ${e.message}")
            }
        }
    }

    override fun estaGrabando(): Boolean {
        return audioRecorderService.estaGrabando()
    }
}
