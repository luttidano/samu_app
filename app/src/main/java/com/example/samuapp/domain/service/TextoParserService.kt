package com.example.samuapp.domain.service

import com.example.samuapp.domain.model.DatosPaciente

class TextoParserService {

    companion object {
        const val ID_HIPOGLICEMIA = "hipoglicemia"
        const val ID_HIPERGLICEMIA = "hiperglicemia"
        const val ID_EPA = "protocolo_edema_pulmon_agudo_001"
        const val ID_SCA = "protocolo_sindrome_coronario_001"
        const val ID_CONVULSION = "protocolo_convulsion_001"
        const val ID_PCR_NT = "protocolo_pcr_no_traumatico_001"
        
        private val DICCIONARIO_URGENCIAS = mapOf(
            ID_HIPOGLICEMIA to listOf(
                "hipoglicemia", "hipoglucemia", "hipoglisemia", "azucar baja",
                "glicemia baja", "ipoglicemia", "yporglicemia", "y por glissemia",
                "y por glisemia", "y por licemia", "y polisemia"
            ),
            ID_HIPERGLICEMIA to listOf(
                "hiperglicemia", "hiperglucemia", "hiperglisemia", "azucar alta",
                "glicemia alta", "iperglicemia", "yperglicemia", "y perglicemia",
                "ypereglesemia", "iperglissemia"
            ),
            ID_EPA to listOf(
                "edema agudo de pulmon", "edema agudo pulmonar", "edema pulmonar",
                "epa", "insuficiencia cardiaca", "insuficiencia cardiaca aguda"
            ),
            ID_SCA to listOf(
                "sindrome coronario", "sindrome coronario agudo", "coronario agudo",
                "infarto", "iam", "sca", "sdst", "dolor toracico",
                "dolor de pecho", "dolor precordial", "ataque al corazon"
            ),
            ID_CONVULSION to listOf(
                "convulsion", "status convulsivo", "crisis convulsiva", "epilepsia",
                "crisis epiléptica", "convulsionando", "estatos convulsivo", "ataque epilepsia",
                "ataque convulsivo"
            ),
            ID_PCR_NT to listOf(
             "paro", "paro cardiorespiratorio", "paro cardiorrespiratorio", "pcr",
            "paro cardiaco", "en paro", "rcp", "asistolia", "fibrilacion ventricular","taquicardia ventricular", "sin pulso", "inconsciente sin pulso", "paro no traumatico" 
            )
        )
    }

    fun analizar(transcripcion: String): DatosPaciente {
        val texto = normalizar(transcripcion)
        val pesoKg = extraerPeso(texto)
        val sexo = extraerSexo(texto)
        val urgencia = extraerUrgencia(texto)

        val motivos = mutableListOf<String>()

        if (urgencia == null) {
            motivos.add("No se identificó el tipo de urgencia")
        }
        if (sexo == null) {
            motivos.add("No se identificó el sexo del paciente")
        }
        if (pesoKg == null || pesoKg <= 0 || pesoKg > 300) {
            motivos.add("No se identificó el peso o está fuera de rango")
        }

        return DatosPaciente(
            urgencia = urgencia,
            sexo = sexo,
            pesoKg = pesoKg,
            esValido = motivos.isEmpty(),
            requiereConfirmacion = true,
            motivos = motivos
        )
    }

    private fun normalizar(texto: String): String {
        return texto.lowercase()
            .replace('á', 'a')
            .replace('é', 'e')
            .replace('í', 'i')
            .replace('ó', 'o')
            .replace('ú', 'u')
            .replace('ñ', 'n')
            .replace(Regex("""\s+"""), " ")
            .trim()
    }

    private fun extraerSexo(texto: String): String? {
        if (texto.contains("femenino") || texto.contains("femenina") || texto.contains("mujer")) {
            return "femenino"
        }
        if (texto.contains("masculino") || texto.contains("+ culino") ||
            texto.contains("+ culina") || texto.contains("masculina") ||
            texto.contains("hombre") || texto.contains("varon")
        ) {
            return "masculino"
        }
        return null
    }

    private fun extraerPeso(texto: String): Double? {
        val match = Regex("""(\d+(?:[.,]\d+)?)\s*(?:kg|kilos?|kilogramos?)""").find(texto)
            ?: return null

        return match.groupValues[1].replace(',', '.').toDoubleOrNull()
    }

    private fun extraerUrgencia(texto: String): String? {
        val t = texto

        // 1. Coincidencia directa / exacta
        for ((urgenciaId, tokens) in DICCIONARIO_URGENCIAS) {
            val palabrasTexto = t.replace(Regex("""[.,;:!?]"""), "")
                .split(" ")
                .filter { it.isNotEmpty() }

            val coincideExacto = tokens.any { token ->
                if (token.length <= 3) {
                    palabrasTexto.contains(token)
                } else {
                    t.contains(token)
                }
            }

            if (coincideExacto) {
                return urgenciaId
            }
        }

        // 2. Fuzzy-Match por distancia Levenshtein
        for ((urgenciaId, tokens) in DICCIONARIO_URGENCIAS) {
            if (coincideFuzzy(t, tokens, urgenciaId)) {
                return urgenciaId
            }
        }

        return null
    }

    private fun coincideFuzzy(textoDictado: String, tokensObjetivo: List<String>, urgenciaId: String): Boolean {
        val palabras = textoDictado.replace(Regex("""[.,;:!?]"""), "")
            .split(" ")
            .filter { it.isNotEmpty() }

        val candidatos = palabras.toMutableList()
        for (i in 0 until palabras.size - 1) {
            candidatos.add("${palabras[i]} ${palabras[i + 1]}")
        }

        for (candidato in candidatos) {
            for (token in tokensObjetivo) {
                if (token.length <= 3) {
                    if (candidato == token) return true
                    continue
                }

                if (esConflictoHipoHiper(candidato, token)) {
                    continue
                }

                if (candidato.length < 5 || token.length < 5) {
                    if (candidato == token) return true
                    continue
                }

                val maxLen = maxOf(candidato.length, token.length)
                val dist = distanciaLevenshtein(candidato, token)
                val similitud = 1.0 - (dist.toDouble() / maxLen)

                val esPalabraLarga = maxLen >= 10
                val maxDistPermitida = if (esPalabraLarga) 3 else 2
                val minSimilitud = if (esPalabraLarga) 0.72 else 0.78

                if (similitud >= minSimilitud && dist <= maxDistPermitida) {
                    return true
                }
            }
        }
        return false
    }

    private fun esConflictoHipoHiper(a: String, b: String): Boolean {
        val aHipo = a.startsWith("hipo") || a.contains("hipo")
        val aHiper = a.startsWith("hiper") || a.contains("hiper")
        val bHipo = b.startsWith("hipo") || b.contains("hipo")
        val bHiper = b.startsWith("hiper") || b.contains("hiper")

        return (aHipo && bHiper) || (aHiper && bHipo)
    }

    private fun distanciaLevenshtein(s1: String, s2: String): Int {
        if (s1 == s2) return 0
        if (s1.isEmpty()) return s2.length
        if (s2.isEmpty()) return s1.length

        val v0 = IntArray(s2.length + 1) { it }
        val v1 = IntArray(s2.length + 1)

        for (i in s1.indices) {
            v1[0] = i + 1
            for (j in s2.indices) {
                val cost = if (s1[i] == s2[j]) 0 else 1
                val deletion = v0[j + 1] + 1
                val insertion = v1[j] + 1
                val substitution = v0[j] + cost

                v1[j + 1] = minOf(deletion, insertion, substitution)
            }
            for (j in v0.indices) {
                v0[j] = v1[j]
            }
        }
        return v1[s2.length]
    }
}
