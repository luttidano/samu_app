package com.example.samuapp.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
data class OpcionPregunta(
    val texto: String,
    @SerialName("siguiente_nodo")
    val siguienteNodo: String
)

@Serializable
data class NodoProtocolo(
    val id: String = "",
    val tipo: String = "procedimiento",
    val texto: String = "",
    @SerialName("siguiente_nodo")
    val siguienteNodo: String? = null,
    @SerialName("instruccion_extra")
    val instruccionExtra: String? = null,
    val opciones: List<OpcionPregunta> = emptyList(),
    @SerialName("nombre_droga")
    val nombreDroga: String? = null,
    @SerialName("dosis_formula")
    val dosisFormula: String? = null,
    val unidad: String? = null,
    val presentacion: String? = null,
    val via: String? = null
)

@Serializable
data class InsumoPreparacion(
    val id: String,
    val nombre: String
)

@Serializable
data class MedicamentoPreparacion(
    @SerialName("nodo_id")
    val nodoId: String,
    val condicion: String = ""
)

@Serializable
data class PreparacionSalida(
    @SerialName("insumos_generales")
    val insumosGenerales: List<InsumoPreparacion> = emptyList(),
    val medicamentos: List<MedicamentoPreparacion> = emptyList()
)

@Serializable
data class ElementoDiagrama(
    val id: String,
    val tipo: String = "azul",
    val x: Double = 0.0,
    val y: Double = 0.0,
    val width: Double = 200.0,
    val height: Double = 80.0,
    val texto: String? = null,
    @SerialName("nodo_id")
    val nodoId: String? = null,
    @SerialName("font_size")
    val fontSize: Double = 14.0
)

@Serializable
data class ConexionDiagrama(
    val desde: String,
    val hasta: String,
    val etiqueta: String? = null,
    val salida: String = "abajo",
    val entrada: String = "arriba"
)

@Serializable
data class DiagramaProtocolo(
    val width: Double = 920.0,
    val height: Double = 1200.0,
    val elementos: List<ElementoDiagrama> = emptyList(),
    val conexiones: List<ConexionDiagrama> = emptyList()
)

data class NodoDroga(
    val id: String,
    val nombreDroga: String,
    val dosisFormula: String,
    val unidad: String,
    val presentacion: String,
    val via: String,
    val instruccionExtra: String
)

@Serializable
data class Protocolo(
    val id: String,
    val nombre: String,
    @SerialName("tags_busqueda")
    val tagsBusqueda: List<String> = emptyList(),
    @SerialName("nodo_inicial")
    val nodoInicial: String = "nodo_abcde",
    val nodos: Map<String, NodoProtocolo> = emptyMap(),
    @SerialName("alertas_generales")
    val alertasGenerales: List<String> = emptyList(),
    @SerialName("preparacion_salida")
    val preparacionSalida: PreparacionSalida = PreparacionSalida(),
    val diagrama: DiagramaProtocolo = DiagramaProtocolo()
) {
    fun obtenerNodoDroga(idNodo: String): NodoDroga? {
        val nodo = nodos[idNodo] ?: return null
        if (nodo.tipo == "droga") {
            return NodoDroga(
                id = idNodo,
                nombreDroga = nodo.nombreDroga ?: "Medicamento",
                dosisFormula = nodo.dosisFormula ?: "",
                unidad = nodo.unidad ?: "",
                presentacion = nodo.presentacion ?: "",
                via = nodo.via ?: "",
                instruccionExtra = nodo.instruccionExtra ?: ""
            )
        }
        return null
    }
}
