package com.example.samuapp.data.repository

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class ModeloRepository(private val context: Context) {

    companion object {
        private const val TAG = "ModeloRepository"
        const val MODELO_FILE_NAME = "ggml-small-q5_1.bin"
        const val MODELO_ASSET_PATH = "model/ggml-small-q5_1.bin"
    }

    suspend fun obtenerRutaModelo(): String = withContext(Dispatchers.IO) {
        val modelsDir = File(context.filesDir, "models")
        if (!modelsDir.exists()) {
            modelsDir.mkdirs()
        }

        val modelFile = File(modelsDir, MODELO_FILE_NAME)
        if (!modelFile.exists() || modelFile.length() == 0L) {
            Log.d(TAG, "Copiando modelo Whisper desde assets a almacenamiento interno...")
            val startTime = System.currentTimeMillis()

            context.assets.open(MODELO_ASSET_PATH).use { input ->
                FileOutputStream(modelFile).use { output ->
                    input.copyTo(output, bufferSize = 64 * 1024)
                }
            }

            val elapsed = System.currentTimeMillis() - startTime
            Log.d(TAG, "Modelo copiado con éxito (${modelFile.length()} bytes) en $elapsed ms")
        }

        modelFile.absolutePath
    }
}
