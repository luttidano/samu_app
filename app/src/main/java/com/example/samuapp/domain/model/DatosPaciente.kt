package com.example.samuapp.domain.model

data class DatosPaciente(
    val urgencia: String? = null,
    val sexo: String? = null,
    val pesoKg: Double? = null,
    val esValido: Boolean = false,
    val requiereConfirmacion: Boolean = true,
    val motivos: List<String> = emptyList(),
    val confirmadoPorUsuario: Boolean = false
)
