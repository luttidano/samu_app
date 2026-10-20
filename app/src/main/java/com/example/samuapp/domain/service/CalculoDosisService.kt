package com.example.samuapp.domain.service

import com.example.samuapp.domain.model.CalculoDosis
import com.example.samuapp.domain.model.NodoDroga
import kotlin.math.ceil

class CalculoDosisService {

    fun calcularDosis(
        pesoKg: Double,
        nodoDroga: NodoDroga,
        alertasProtocolo: List<String> = emptyList()
    ): CalculoDosis {
        if (pesoKg <= 0 || pesoKg > 300) {
            throw IllegalArgumentException("El peso $pesoKg kg está fuera del rango fisiológico seguro")
        }

        val formula = nodoDroga.dosisFormula.trim()
        val unidadRaw = nodoDroga.unidad.trim()
        val viaRaw = nodoDroga.via.trim()
        val esInfusion = unidadRaw.lowercase().contains("/min") || viaRaw.lowercase().contains("bic")

        // 1. Caso Dosis Referencial / Indicación Médica
        val esTextoReferencial = !Regex("""\d""").containsMatchIn(formula) ||
                formula.lowercase().contains("según") ||
                formula.lowercase().contains("indicación")

        if (esTextoReferencial) {
            return CalculoDosis(
                sustancia = nodoDroga.nombreDroga,
                via = nodoDroga.via,
                pesoPacienteKg = pesoKg,
                dosisMinima = 0.0,
                dosisMaxima = 0.0,
                unidadMasa = unidadRaw,
                volumenMinimoMl = 0.0,
                volumenMaximoMl = 0.0,
                unidadVolumen = "mL",
                presentacion = nodoDroga.presentacion,
                instruccionClinica = "$formula (${nodoDroga.nombreDroga})",
                insumosRequeridos = listOf(
                    "${nodoDroga.nombreDroga}: ${nodoDroga.presentacion}",
                    "Vía de administración: ${nodoDroga.via}"
                ),
                alertasSeguridad = buildList {
                    if (nodoDroga.instruccionExtra.isNotEmpty()) add(nodoDroga.instruccionExtra)
                    addAll(alertasProtocolo)
                },
                esReferencial = true,
                tipoEmpaque = "frasco ampolla",
                detalleEmpaque = "Llevar 1 frasco ampolla (dosis según indicación médica)"
            )
        }

        // 2. Extraer factores numéricos
        var minFactor = 0.0
        var maxFactor = 0.0
        val partes = formula.split("-")

        if (partes.size == 2) {
            minFactor = partes[0].trim().replace(',', '.').toDoubleOrNull() ?: 0.0
            maxFactor = partes[1].trim().replace(',', '.').toDoubleOrNull() ?: 0.0
        } else {
            val factor = formula.replace(',', '.').toDoubleOrNull() ?: 0.0
            minFactor = factor
            maxFactor = factor
        }

        // 3. Determinar si multiplica por peso
        val esPorKg = unidadRaw.lowercase().contains("kg") || unidadRaw.lowercase().contains("kilo")
        val dosisMin = if (esPorKg) pesoKg * minFactor else minFactor
        val dosisMax = if (esPorKg) pesoKg * maxFactor else maxFactor
        val unidadMasa = extraerUnidadMasa(unidadRaw)

        // 4. Calcular volumen (mL) y concentración
        val concentracionFactor = obtenerFactorConcentracion(
            nodoDroga.nombreDroga,
            nodoDroga.presentacion
        )

        var volMin = 0.0
        var volMax = 0.0
        if (!esInfusion && concentracionFactor > 0) {
            volMin = dosisMin / concentracionFactor
            volMax = dosisMax / concentracionFactor
        }

        // 5. Cálculo de empaque
        val empaque = calcularEmpaque(
            droga = nodoDroga,
            dosisMax = dosisMax,
            volumenMaxMl = volMax,
            esInfusion = esInfusion
        )

        // 6. Insumos y alertas
        val insumos = generarInsumos(nodoDroga, volMax, empaque)
        val alertas = buildList {
            if (nodoDroga.instruccionExtra.isNotEmpty()) add(nodoDroga.instruccionExtra)
            if (esInfusion) add("Programar infusión continua en BIC según PAM objetivo.")
            addAll(alertasProtocolo)
        }

        return CalculoDosis(
            sustancia = nodoDroga.nombreDroga,
            via = nodoDroga.via,
            pesoPacienteKg = pesoKg,
            dosisMinima = dosisMin,
            dosisMaxima = dosisMax,
            unidadMasa = unidadMasa,
            volumenMinimoMl = volMin,
            volumenMaximoMl = volMax,
            unidadVolumen = "mL",
            presentacion = nodoDroga.presentacion,
            instruccionClinica = if (esInfusion) {
                "${nodoDroga.nombreDroga} a $formula ${nodoDroga.unidad} vía ${nodoDroga.via}"
            } else {
                "${nodoDroga.nombreDroga} ($formula ${nodoDroga.unidad}) vía ${nodoDroga.via}"
            },
            insumosRequeridos = insumos,
            alertasSeguridad = alertas,
            unidadesRequeridas = empaque.unidades,
            tipoEmpaque = empaque.tipo,
            detalleEmpaque = empaque.detalle,
            esInfusion = esInfusion,
            velocidadInfusion = if (esInfusion) "%.2f - %.2f %s".format(dosisMin, dosisMax, unidadRaw) else null
        )
    }

    private fun extraerUnidadMasa(unidadRaw: String): String {
        val u = unidadRaw.lowercase()
        return when {
            u.contains("ug") || u.contains("mcg") || u.contains("microgram") -> "mcg"
            u.contains("gr") || u.contains("g/") || u.contains("g ") -> "g"
            u.contains("mg") -> "mg"
            else -> "unidades"
        }
    }

    private fun obtenerFactorConcentracion(nombreDroga: String, presentacion: String): Double {
        val nom = nombreDroga.lowercase()
        val pres = presentacion.lowercase()
        
        // deteccion de porcentajes
        
        val matchPct = Regex("""(\d+)?:[.,]\d+)?)\s*%""").find(pres)
        if (matchPct != null) {
            val pct = matchPct.groupValues[1].replace(',', '.').toDoubleOrNull() ?: 0.0
            return if (nom.contains("glucosa")) pct / 100.0 else pct * 10.0
        }
        
        // Deduccion universal por fraccion

        val matchFraccion = Regex("""(\d+(?:[.,]\d+)?)\s*(?:mg|g|gr|ug|mcg)\s*/\s*(\d+(?:[.,]\d+)?)?\s*ml""").find(pres)
        if (matchFraccion != null) {
            val masa = matchFraccion.groupValues[1].replace(',', '.').toDoubleOrNull() ?: 1.0
            val vol = matchFraccion.groupValues.getOrNull(2)?.takeIf { it.isNotEmpty() }?.replace(',', '.')?.toDoubleOrNull() ?: 1.0
            if (vol > 0) return masa / vol
        }

        return 1.0
    }

    private fun calcularEmpaque(
        droga: NodoDroga,
        dosisMax: Double,
        volumenMaxMl: Double,
        esInfusion: Boolean
    ): InfoEmpaque {
        if (esInfusion) {
            return InfoEmpaque(1, "ampolla + matraz", "Llevar mínimo 1 ampolla de ${droga.nombreDroga} + 1 Matraz para BIC")
        }
        val pres = droga.presentacion.lowercase()
        val esComprimido = pres.contains("comprimido") || pres.contains("comp") || pres.contains("sublingual")
        val tipo = if (esComprimido) "comprimidos" else "ampollas"
        // Caso especial: Glucosa (su empaque se dosifica por volumen de ampolla de 20 mL)
        if (droga.nombreDroga.lowercase().contains("glucosa") && volumenMaxMl > 0) {
            val ampollas = ceil(volumenMaxMl / 20.0).toInt().coerceAtLeast(1)
            return InfoEmpaque(ampollas, "ampollas", "Llevar mínimo $ampollas ampollas de Glucosa 30% (de 20 mL c/u)")
        }
        // Extracción universal de masa por unidad (ej: 4 mg, 10 mg, 20 mg, 25 mg, 200 mg, 250 mg)
        val matchMasaUnidad = Regex("""(\d+(?:[.,]\d+)?)\s*(?:mg|g|gr|ug|mcg)""").find(pres)
        if (matchMasaUnidad != null && dosisMax > 0) {
            val masaUnidad = matchMasaUnidad.groupValues[1].replace(',', '.').toDoubleOrNull() ?: 0.0
            if (masaUnidad > 0) {
                val cantidad = ceil(dosisMax / masaUnidad).toInt().coerceAtLeast(1)
                val unidadTexto = if (cantidad == 1) tipo.removeSuffix("s") else tipo
                return InfoEmpaque(cantidad, tipo, "Llevar mínimo $cantidad $unidadTexto de ${droga.nombreDroga} (${droga.presentacion})")
            }
        }
        return InfoEmpaque(1, tipo, "Llevar presentación: ${droga.presentacion}")
    }

    private fun generarInsumos(droga: NodoDroga, volMaxMl: Double, empaque: InfoEmpaque): List<String> {
        return buildList {
            add(empaque.detalle)
            val via = droga.via.lowercase()
            when {
                via.contains("bic") -> add("Bomba de Infusión Continua (BIC) + Bajada de bomba")
                via.contains("ev") || via.contains("iv") -> {
                    add("Bránula N° 18G o 20G + Llave de 3 pasos")
                    val tamJeringa = if (volMaxMl > 20) "50 mL" else if (volMaxMl > 5) "20 mL" else "5 mL o 10 mL"
                    add("Jeringa adecuada ($tamJeringa)")
                }
            }
            add("Material de asepsia y fijación (Alcohol + Tegaderm/Tela)")
        }
    }

    private data class InfoEmpaque(
        val unidades: Int,
        val tipo: String,
        val detalle: String
    )
}
