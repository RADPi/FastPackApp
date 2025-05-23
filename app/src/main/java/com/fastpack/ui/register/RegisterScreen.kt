package com.fastpack.ui.register

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.fastpack.R
import kotlinx.coroutines.flow.collectLatest

@Composable
fun RegisterScreen( // <--- No se pasa NavController aquí directamente
    viewModel: RegisterViewModel = hiltViewModel(),
    onNavigateToHome: () -> Unit,
    onNavigateToLogin: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState() // Consistente con LoginScreen
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(key1 = true) {
        viewModel.effect.collectLatest { effect ->
            when (effect) {
                is RegisterEffect.ShowSnackbar -> {
                    snackbarHostState.showSnackbar(effect.message)
                }
                RegisterEffect.NavigateToHome -> {
                    onNavigateToHome()
                }
                RegisterEffect.NavigateToLogin -> {
                    onNavigateToLogin()
                }
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Image(painterResource(R.drawable.logofp250), contentDescription = "Logo", modifier = Modifier.size(250.dp))

                OutlinedTextField(
                    value = uiState.emailInput,
                    onValueChange = { viewModel.onEvent(RegisterUiEvent.EmailChanged(it)) },
                    label = { Text("Email") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = uiState.passwordInput,
                    onValueChange = { viewModel.onEvent(RegisterUiEvent.PasswordChanged(it)) },
                    label = { Text("Contraseña") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = { viewModel.onEvent(RegisterUiEvent.RegisterClicked) },
                    enabled = !uiState.isLoading,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (uiState.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Text("Registrar")
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                TextButton(onClick = { viewModel.onEvent(RegisterUiEvent.NavigateToLogin)
                }) {
                    Text("¿Ya tienes cuenta? Ingresa")
                }
            }
        }
    }
}

// ... (Preview)