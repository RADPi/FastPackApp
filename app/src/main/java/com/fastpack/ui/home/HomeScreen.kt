package com.fastpack.ui.home // O el paquete que corresponda

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel


@ExperimentalMaterial3Api
@Composable
fun HomeScreen(
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    // Mostrar Snackbar si hay un error
    uiState.errorMessage?.let { message ->
        LaunchedEffect(message) {
            snackbarHostState.showSnackbar(message)
            viewModel.errorMessageShown() // Limpiar el mensaje después de mostrarlo
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
//        topBar = {
//            // Puedes agregar un TopAppBar aquí si lo necesitas
//             TopAppBar(title = { Text("Análisis de Envíos") }, colors =
//                 TopAppBarDefaults.topAppBarColors(
//                     containerColor = MaterialTheme.colorScheme.primary,
//                     titleContentColor = Color.White
//                 ))
//        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(10.dp),
            contentAlignment = Alignment.Center
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator()
            } else if (uiState.analysisResult.isNotEmpty()) {
                ShipmentAnalysisContent(analysis = uiState.analysisResult)
            } else if (uiState.errorMessage == null) {
                // Estado vacío pero sin error (podría ser que no haya datos)
                Text("No hay datos de análisis disponibles.")
            }
        }
    }
}

@Composable
fun ShipmentAnalysisContent(analysis: Map<String, Int>) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Resumen de Envíos Pendientes",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(bottom = 20.dp)
        )

        AnalysisCard("Total de Envíos", analysis["TotalEnvios"] ?: 0)

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Envíos Flex",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            AnalysisCard(
                "Para Imprimir",
                analysis["FlexReadyToPrint"] ?: 0,
                Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(8.dp))
            AnalysisCard(
                "Para Retirar",
                analysis["FlexPendientes"] ?: 0,
                Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Envíos por Despacho",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            AnalysisCard(
                "Para Imprimir",
                analysis["DespReadyToPrint"] ?: 0,
                Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(8.dp))
            AnalysisCard(
                "Para Despachar",
                analysis["DespPendientes"] ?: 0,
                Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun AnalysisCard(title: String, count: Int, modifier: Modifier = Modifier) {
    ElevatedCard(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.headlineMedium,
                color = if (count > 0 && (title.contains("Listos") || title.contains("Total"))) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}