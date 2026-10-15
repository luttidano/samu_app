package com.example.samuapp.data.service

import android.util.Log
import com.sun.jna.Library
import com.sun.jna.Native
import com.sun.jna.Pointer

interface WhisperNativeLibrary : Library {
    fun request(body: String): Pointer?
    fun abort_transcription()

    companion object {
        val INSTANCE: WhisperNativeLibrary by lazy {
            Native.load("whisper", WhisperNativeLibrary::class.java)
        }
    }
}

object WhisperNativeBridge {
    private const val TAG = "WhisperNativeBridge"

    fun request(jsonBody: String): String {
        val ptr = WhisperNativeLibrary.INSTANCE.request(jsonBody)
            ?: throw IllegalStateException("Respuesta nula desde whisper nativo")
        return try {
            ptr.getString(0, "UTF-8")
        } finally {
            Native.free(Pointer.nativeValue(ptr))
        }
    }

    fun abort() {
        try {
            Log.d(TAG, "Llamando a abort_transcription() en librería nativa C++")
            WhisperNativeLibrary.INSTANCE.abort_transcription()
        } catch (e: Exception) {
            Log.e(TAG, "Error al invocar abort_transcription(): ${e.message}", e)
        }
    }
}
