package com.example.samuapp.domain.model

data class CalculoDosis(
    val sustancia: String,
    val via: String,
    val pesoPacienteKg: Double,
    val dosisMinima: Double,
    val dosisMaxima: Double,
    val unidadMasa: String,
    val volumenMinimoMl: Double,
    val volumenMaximoMl: Double,
    val unidadVolumen: String,
    val presentacion: String,
    val instruccionClinica: String,
    val insumosRequeridos: List<String>,
    val alertasSeguridad: List<String>,
    val unidadesRequeridas: Int? = null,
    val tipoEmpaque: String? = null,
    val detalleEmpaque: String? = null,
    val esInfusion: Boolean = false,
    val velocidadInfusion: String? = null,
    val esReferencial: Boolean = false
) {
    val esDosisFija: Boolean get() = dosisMinima == dosisMaxima
}
