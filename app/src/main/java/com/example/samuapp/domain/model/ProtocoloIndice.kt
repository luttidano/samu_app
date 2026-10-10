package com.example.samuapp.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ProtocoloIndice(
    val id: String,
    val nombre: String,
    val categoria: String = "General",
    val archivo: String,
    @SerialName("tags_busqueda")
    val tagsBusqueda: List<String> = emptyList()
)
