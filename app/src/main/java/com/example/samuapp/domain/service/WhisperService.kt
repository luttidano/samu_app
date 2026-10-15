package com.example.samuapp.domain.service

import java.io.File

/**
 * Contrato de Dominio para el servicio de reconocimiento de voz y transcripción (Whisper).
 * No contiene dependencias del SDK de Android ni de la capa de datos (Clean Architecture).
 */
interface WhisperService {
    suspend fun preCargarModelo()
    fun solicitarPermiso(): Boolean
    suspend fun iniciarGrabacion(): File
    suspend fun detenerYTranscribir(): String
    suspend fun cancelarTranscripcion()
    suspend fun liberarModelo()
    fun estaGrabando(): Boolean
}
