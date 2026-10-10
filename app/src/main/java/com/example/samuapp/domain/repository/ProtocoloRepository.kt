package com.example.samuapp.domain.repository

import com.example.samuapp.domain.model.Protocolo
import com.example.samuapp.domain.model.ProtocoloIndice

interface ProtocoloRepository {
    suspend fun obtenerIndice(): List<ProtocoloIndice>
    suspend fun buscarPorUrgencia(urgencia: String): Protocolo?
    suspend fun cargarProtocoloPorRuta(ruta: String): Protocolo
    suspend fun cargarProtocolos(): List<Protocolo>
}
