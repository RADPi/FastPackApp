package com.fastpack.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fastpack.data.model.AuthResponse
import com.fastpack.data.model.UserRequest
import com.fastpack.data.repository.AuthRepository
import com.fastpack.util.Resource // Tu clase Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

// Estado de la UI
data class LoginScreenUiState(
    val emailInput: String = "",
    val passwordInput: String = "",
    val isLoading: Boolean = false,
)

// Eventos de la UI (acciones del usuario)
sealed class LoginUiEvent {
    data class EmailChanged(val email: String) : LoginUiEvent()
    data class PasswordChanged(val password: String) : LoginUiEvent()
    object LoginClicked : LoginUiEvent()
    object NavigateToRegister : LoginUiEvent()
}

// Efectos secundarios (navegación, snackbars)
sealed class LoginEffect {
    data class ShowSnackbar(val message: String) : LoginEffect()
    object NavigateToHome : LoginEffect()
    object NavigateToRegister : LoginEffect()
}

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginScreenUiState())
    val uiState: StateFlow<LoginScreenUiState> = _uiState.asStateFlow()

    private val _effect = Channel<LoginEffect>()
    val effect: Flow<LoginEffect> = _effect.receiveAsFlow()

    fun onEvent(event: LoginUiEvent) {
        when (event) {
            is LoginUiEvent.EmailChanged -> {
                _uiState.update { it.copy(emailInput = event.email) }
            }

            is LoginUiEvent.PasswordChanged -> {
                _uiState.update { it.copy(passwordInput = event.password) }
            }

            LoginUiEvent.LoginClicked -> {
                loginUser()
            }

            LoginUiEvent.NavigateToRegister -> {
                viewModelScope.launch {
                    _effect.send(LoginEffect.NavigateToRegister)
                }
            }
        }
    }

    private fun loginUser() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val email = _uiState.value.emailInput
            val password = _uiState.value.passwordInput

            if (email.isBlank() || password.isBlank()) {
                _effect.send(LoginEffect.ShowSnackbar("Email y contraseña son requeridos"))
                _uiState.update { it.copy(isLoading = false) }
                return@launch
            }

            val result: Resource<AuthResponse> =
                authRepository.loginUser(UserRequest(email = email, password = password))

            when (result) {
                is Resource.Success -> {
                    // El token ya se guardó en el AuthRepository.
                    // result.data contiene AuthResponse, podrías usar result.data.user si es necesario.
                    _effect.send(LoginEffect.NavigateToHome)
                }

                is Resource.Error -> {
                    _effect.send(
                        LoginEffect.ShowSnackbar(
                            result.message ?: "Error de inicio de sesión desconocido"
                        )
                    )
                }

                is Resource.Loading -> {
                    // Si tienes un estado de Loading explícito emitido por el repositorio
                    // (actualmente no lo estamos haciendo para login/register), lo manejarías aquí.
                    // Por ahora, el isLoading del UiState se controla manualmente.
                }
            }
            _uiState.update { it.copy(isLoading = false) }
        }
    }
}