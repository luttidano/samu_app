package com.example.samuapp.data.service

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import androidx.core.content.ContextCompat
import kotlinx.coroutines.*
import java.io.File
import java.io.FileOutputStream
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder

class AudioRecorderService(private val context: Context) {

    companion object {
        private const val TAG = "AudioRecorderService"
        const val SAMPLE_RATE = 16000
        const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
        const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
        const val CHANNELS = 1
        const val BITS_PER_SAMPLE = 16
    }

    private var audioRecord: AudioRecord? = null
    private var recordingJob: Job? = null
    private var outputFile: File? = null
    private var startTimeMs: Long = 0L

    @Volatile
    private var isRecording = false

    fun tienePermiso(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }

    @SuppressLint("MissingPermission")
    suspend fun iniciarGrabacion(): File = withContext(Dispatchers.IO) {
        if (!tienePermiso()) {
            throw SecurityException("Permiso de grabación de audio no concedido")
        }

        if (isRecording) {
            cancelarGrabacion()
        }

        val bufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT)
            .coerceAtLeast(SAMPLE_RATE * 2)

        val record = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            SAMPLE_RATE,
            CHANNEL_CONFIG,
            AUDIO_FORMAT,
            bufferSize
        )

        if (record.state != AudioRecord.STATE_INITIALIZED) {
            record.release()
            throw IllegalStateException("No se pudo inicializar AudioRecord")
        }

        val file = File(context.cacheDir, "grabacion_samu.wav")
        if (file.exists()) {
            file.delete()
        }

        outputFile = file
        audioRecord = record
        isRecording = true
        startTimeMs = System.currentTimeMillis()

        record.startRecording()

        recordingJob = CoroutineScope(Dispatchers.IO).launch {
            val buffer = ByteArray(bufferSize)
            FileOutputStream(file).use { os ->
                // Escribir cabecera WAV de 44 bytes con datos vacíos que se actualizarán al finalizar
                val emptyHeader = ByteArray(44)
                os.write(emptyHeader)

                while (isActive && isRecording) {
                    val read = record.read(buffer, 0, buffer.size)
                    if (read > 0) {
                        os.write(buffer, 0, read)
                    }
                }
            }
        }

        Log.d(TAG, "Grabación iniciada en: ${file.absolutePath}")
        file
    }

    suspend fun detenerGrabacion(): Long = withContext(Dispatchers.IO) {
        if (!isRecording) return@withContext 0L

        isRecording = false
        val durationMs = System.currentTimeMillis() - startTimeMs

        try {
            audioRecord?.stop()
            audioRecord?.release()
        } catch (e: Exception) {
            Log.e(TAG, "Error al detener AudioRecord: ${e.message}")
        } finally {
            audioRecord = null
        }

        recordingJob?.cancelAndJoin()
        recordingJob = null

        val file = outputFile
        if (file != null && file.exists()) {
            actualizarCabeceraWav(file)
            Log.d(TAG, "Grabación finalizada. Duración: $durationMs ms, Tamaño: ${file.length()} bytes")
        }

        durationMs
    }

    suspend fun cancelarGrabacion() = withContext(Dispatchers.IO) {
        isRecording = false
        try {
            audioRecord?.stop()
            audioRecord?.release()
        } catch (_: Exception) {}
        audioRecord = null

        recordingJob?.cancelAndJoin()
        recordingJob = null

        outputFile?.let { file ->
            if (file.exists()) {
                file.delete()
            }
        }
        outputFile = null
        Log.d(TAG, "Grabación cancelada y archivo eliminado")
    }

    fun estaGrabando(): Boolean = isRecording

    fun obtenerArchivoAudio(): File? = outputFile

    private fun actualizarCabeceraWav(wavFile: File) {
        val totalAudioLen = wavFile.length() - 44
        val totalDataLen = totalAudioLen + 36
        val byteRate = SAMPLE_RATE * CHANNELS * (BITS_PER_SAMPLE / 8)

        val header = ByteBuffer.allocate(44).apply {
            order(ByteOrder.LITTLE_ENDIAN)
            // RIFF chunk
            put("RIFF".toByteArray(Charsets.US_ASCII))
            putInt(totalDataLen.toInt())
            put("WAVE".toByteArray(Charsets.US_ASCII))

            // fmt sub-chunk
            put("fmt ".toByteArray(Charsets.US_ASCII))
            putInt(16) // Subchunk1Size (16 para PCM)
            putShort(1.toShort()) // AudioFormat (1 para PCM)
            putShort(CHANNELS.toShort())
            putInt(SAMPLE_RATE)
            putInt(byteRate)
            putShort((CHANNELS * (BITS_PER_SAMPLE / 8)).toShort()) // BlockAlign
            putShort(BITS_PER_SAMPLE.toShort())

            // data sub-chunk
            put("data".toByteArray(Charsets.US_ASCII))
            putInt(totalAudioLen.toInt())
        }.array()

        RandomAccessFile(wavFile, "rw").use { raf ->
            raf.seek(0)
            raf.write(header)
        }
    }
}
