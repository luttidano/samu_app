package com.example.samuapp.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.example.samuapp.domain.repository.PreferenciasRepository
import com.example.samuapp.ui.theme.ModoTema
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class PreferenciasRepositoryImpl(
    private val dataStore: DataStore<Preferences>
) : PreferenciasRepository {
    companion object {
        private val KEY_MODO_TEMA = stringPreferencesKey("modo_tema")
    }

    override val modoTemaFlow: Flow<ModoTema> = dataStore.data
        .map { preferences ->
            val nombreModo = preferences[KEY_MODO_TEMA] ?: ModoTema.SISTEMA.name
            try {
                ModoTema.valueOf(nombreModo)
            } catch (e: Exception) {
                ModoTema.SISTEMA
            }
        }

    override suspend fun guardarModoTema(modoTema: ModoTema) {
        dataStore.edit { preferences ->
            preferences[KEY_MODO_TEMA] = modoTema.name
        }
    }
}
