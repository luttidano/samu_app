package com.example.samuapp.domain.repository

import com.example.samuapp.ui.theme.ModoTema
import kotlinx.coroutines.flow.Flow

interface PreferenciasRepository {
    val modoTemaFlow: Flow<ModoTema>
    suspend fun guardarModoTema(modoTema: ModoTema)
}