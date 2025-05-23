package com.fastpack.ui.home

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fastpack.data.repository.ShipmentRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

// Estado para la UI de HomeScreen
data class HomeScreenUiState(
    val analysisResult: Map<String, Int> = emptyMap(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val shipmentRepository: ShipmentRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeScreenUiState())
    val uiState: StateFlow<HomeScreenUiState> = _uiState.asStateFlow()

    init {
        loadShipmentAnalysis()
    }

    fun loadShipmentAnalysis() {
        viewModelScope.launch {
            _uiState.value = HomeScreenUiState(isLoading = true) // Inicia carga
            val result = shipmentRepository.fetchAndAnalyzeShipmentsForPacking()
            result.fold(
                onSuccess = { analysisMap ->
                    Log.d("HomeViewModel", "Analysis Result: $analysisMap")
                    _uiState.value = HomeScreenUiState(
                        analysisResult = analysisMap,
                        isLoading = false
                    )
                },
                onFailure = { exception ->
                    Log.e("HomeViewModel", "Excepción al analizar envíos:", exception) // Loguea el mensaje de la excepción y su stack trace
                    _uiState.value = HomeScreenUiState(
                        isLoading = false,
                        errorMessage = exception.message ?: "Error desconocido al analizar envíos"
                    )
                }
            )
        }
    }

    // Opcional: Función para limpiar el mensaje de error una vez mostrado
    fun errorMessageShown() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
}