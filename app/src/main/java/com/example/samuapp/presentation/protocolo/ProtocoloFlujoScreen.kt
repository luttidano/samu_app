package com.example.samuapp.presentation.protocolo

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.example.samuapp.domain.model.Protocolo

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProtocoloFlujoScreen(
    protocolo: Protocolo,
    nodoInicialId: String? = null,
    onVolver: () -> Unit
) {
    var pestañaSeleccionada by remember { mutableIntStateOf(0) }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Text(
                            text = protocolo.nombre,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onVolver) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Regresar"
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.inversePrimary,
                        titleContentColor = MaterialTheme.colorScheme.onSurface,
                        navigationIconContentColor = MaterialTheme.colorScheme.onSurface
                    )
                )

                // Pestañas superiores (Paso a Paso Interactivo y Mapa del Flujo)
                TabRow(
                    selectedTabIndex = pestañaSeleccionada,
                    containerColor = MaterialTheme.colorScheme.surface
                ) {
                    Tab(
                        selected = pestañaSeleccionada == 0,
                        onClick = { pestañaSeleccionada = 0 },
                        text = { Text("Paso a Paso Interactivo") },
                        icon = { Icon(Icons.Default.TouchApp, contentDescription = null) }
                    )
                    Tab(
                        selected = pestañaSeleccionada == 1,
                        onClick = { pestañaSeleccionada = 1 },
                        text = { Text("Mapa del Flujo") },
                        icon = { Icon(Icons.Default.AccountTree, contentDescription = null) }
                    )
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (pestañaSeleccionada) {
                0 -> {
                    VistaPasoAPaso(
                        protocolo = protocolo,
                        nodoInicialId = nodoInicialId,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                1 -> {
                    VistaDiagramaCompleto(
                        protocolo = protocolo,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}
