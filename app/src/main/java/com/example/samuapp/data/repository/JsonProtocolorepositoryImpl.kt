package com.example.samuapp.data.repository

import android.content.Context
import com.example.samuapp.domain.model.Protocolo
import com.example.samuapp.domain.model.ProtocoloIndice
import com.example.samuapp.domain.repository.ProtocoloRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

class JsonProtocoloRepositoryImpl(
    private val context: Context
) : ProtocoloRepository {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    private var indiceCache: List<ProtocoloIndice>? = null

    override suspend fun obtenerIndice(): List<ProtocoloIndice> = withContext(Dispatchers.IO) {
        indiceCache?.let { return@withContext it }

        val contenido = context.assets.open("data/indice_protocolos.json")
            .bufferedReader().use { it.readText() }

        val lista = json.decodeFromString<List<ProtocoloIndice>>(contenido)
        indiceCache = lista
        lista
    }

    override suspend fun buscarPorUrgencia(urgencia: String): Protocolo? = withContext(Dispatchers.IO) {
        val termino = normalizar(urgencia)
        val indice = obtenerIndice()

        for (item in indice) {
            if (item.id.equals(urgencia, ignoreCase = true) || normalizar(item.id) == termino) {
                return@withContext cargarProtocoloPorRuta(item.archivo)
            }

            val coincide = item.tagsBusqueda.any { tag ->
                val tagNorm = normalizar(tag)
                tagNorm.contains(termino) || termino.contains(tagNorm)
            }

            if (coincide) {
                return@withContext cargarProtocoloPorRuta(item.archivo)
            }
        }
        null
    }

    override suspend fun cargarProtocoloPorRuta(ruta: String): Protocolo = withContext(Dispatchers.IO) {
        // En Flutter la ruta era 'assets/data/proto_xxx.json', en Android abrimos 'data/proto_xxx.json'
        val rutaRelativa = ruta.removePrefix("assets/")
        val contenido = context.assets.open(rutaRelativa).bufferedReader().use { it.readText() }
        json.decodeFromString<Protocolo>(contenido)
    }

    override suspend fun cargarProtocolos(): List<Protocolo> = withContext(Dispatchers.IO) {
        val indice = obtenerIndice()
        indice.map { cargarProtocoloPorRuta(it.archivo) }
    }

    private fun normalizar(valor: String): String {
        return valor
            .lowercase()
            .replace('_', ' ')
            .replace('á', 'a')
            .replace('é', 'e')
            .replace('í', 'i')
            .replace('ó', 'o')
            .replace('ú', 'u')
            .replace('ñ', 'n')
            .trim()
    }
}
