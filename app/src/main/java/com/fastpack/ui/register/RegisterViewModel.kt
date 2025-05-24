package com.fastpack.ui.register

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fastpack.data.model.UserRequest // O tu RegisterRequest si es diferente y lo usas
import com.fastpack.data.model.AuthResponse // Necesario para el tipo de Resource
import com.fastpack.data.repository.AuthRepository
import com.fastpack.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

// Estado de la UI
data class RegisterScreenUiState(
    val usernameInput: String = "",
    val emailInput: String = "",
    val passwordInput: String = "",
    // Añade otros campos si son necesarios para el registro (ej. nombre, confirmar contraseña)
    val isLoading: Boolean = false,
)

// Eventos de la UI
sealed class RegisterUiEvent {
    data class UsernameChanged(val username: String) : RegisterUiEvent()
    data class EmailChanged(val email: String) : RegisterUiEvent()
    data class PasswordChanged(val password: String) : RegisterUiEvent()
    // Añade otros eventos según los campos del registro
    object RegisterClicked : RegisterUiEvent()
    object NavigateToLogin : RegisterUiEvent()
}

// Efectos secundarios
sealed class RegisterEffect {
    data class ShowSnackbar(val message: String) : RegisterEffect()
    object NavigateToLogin : RegisterEffect()
    object NavigateToSettings : RegisterEffect()
}

@HiltViewModel
class RegisterViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(RegisterScreenUiState())
    val uiState: StateFlow<RegisterScreenUiState> = _uiState.asStateFlow()

    private val _effect = Channel<RegisterEffect>()
    val effect: Flow<RegisterEffect> = _effect.receiveAsFlow()

    fun onEvent(event: RegisterUiEvent) {
        when (event) {
            is RegisterUiEvent.UsernameChanged -> {
                _uiState.update { it.copy(usernameInput = event.username) }
            }
            is RegisterUiEvent.EmailChanged -> {
                _uiState.update { it.copy(emailInput = event.email) }
            }
            is RegisterUiEvent.PasswordChanged -> {
                _uiState.update { it.copy(passwordInput = event.password) }
            }
            RegisterUiEvent.RegisterClicked -> {
                registerUser()
            }
            RegisterUiEvent.NavigateToLogin -> {
                viewModelScope.launch {
                    _effect.send(RegisterEffect.NavigateToLogin)
                }
            }
        }
    }

    private fun registerUser() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val username = _uiState.value.usernameInput
            val email = _uiState.value.emailInput
            val password = _uiState.value.passwordInput

            // Añade validaciones para otros campos si los tienes (nombre, confirmar contraseña, etc.)
            if (username.isBlank() || email.isBlank() || password.isBlank()) {
                _effect.send(RegisterEffect.ShowSnackbar("Email y contraseña son requeridos"))
                _uiState.update { it.copy(isLoading = false) }
                return@launch
            }

            val result: Resource<AuthResponse> = authRepository.registerUser(
                UserRequest(
                    name = username,
                    email = email,
                    password = password
                )
            )

            when (result) {
                is Resource.Success -> {
                     _effect.send(RegisterEffect.ShowSnackbar("Registro exitoso."))
                     _effect.send(RegisterEffect.NavigateToSettings)
                }
                is Resource.Error -> {
                    _effect.send(RegisterEffect.ShowSnackbar(result.message ?: "Error en el registro desconocido"))
                }
                is Resource.Loading -> {
                    // Manejar estado de carga si es aplicable
                }
            }
            _uiState.update { it.copy(isLoading = false) }
        }
    }
}