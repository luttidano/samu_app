package com.example.samuapp.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import com.example.samuapp.data.repository.JsonProtocoloRepositoryImpl
import com.example.samuapp.data.repository.PreferenciasRepositoryImpl
import com.example.samuapp.data.service.WhisperServiceImpl
import com.example.samuapp.domain.repository.PreferenciasRepository
import com.example.samuapp.domain.repository.ProtocoloRepository
import com.example.samuapp.domain.service.CalculoDosisService
import com.example.samuapp.domain.service.TextoParserService
import com.example.samuapp.domain.service.WhisperService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDataStore(
        @ApplicationContext context: Context
    ): DataStore<Preferences> {
        return PreferenceDataStoreFactory.create(
            produceFile = { context.preferencesDataStoreFile("samu_preferencias") }
        )
    }

    @Provides
    @Singleton
    fun provideProtocoloRepository(
        @ApplicationContext context: Context
    ): ProtocoloRepository {
        return JsonProtocoloRepositoryImpl(context)
    }

    @Provides
    @Singleton
    fun providePreferenciasRepository(
        dataStore: DataStore<Preferences>
    ): PreferenciasRepository {
        return PreferenciasRepositoryImpl(dataStore)
    }

    @Provides
    @Singleton
    fun provideWhisperService(
        @ApplicationContext context: Context
    ): WhisperService {
        return WhisperServiceImpl(context)
    }

    @Provides
    @Singleton
    fun provideCalculoDosisService(): CalculoDosisService {
        return CalculoDosisService()
    }

    @Provides
    @Singleton
    fun provideTextoParserService(): TextoParserService {
        return TextoParserService()
    }
}
