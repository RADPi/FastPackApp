package com.fastpack.ui.login

import android.util.Patterns // Para validación de email
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions // Importado
import androidx.compose.foundation.text.KeyboardOptions // Ya estaba
import androidx.compose.material.icons.Icons
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi // Importado
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection // Importado
import androidx.compose.ui.focus.FocusRequester // Importado
import androidx.compose.ui.focus.focusRequester // Importado
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager // Importado
import androidx.compose.ui.platform.LocalSoftwareKeyboardController // Importado
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction // Importado
import androidx.compose.ui.text.input.KeyboardType // Ya estaba
import androidx.compose.ui.text.input.PasswordVisualTransformation // Ya estaba
import androidx.compose.ui.text.input.VisualTransformation // Importado
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.fastpack.R
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalComposeUiApi::class) // Necesario para LocalSoftwareKeyboardController
@Composable
fun LoginScreen(
    viewModel: LoginViewModel = hiltViewModel(),
    onNavigateToHome: () -> Unit,
    onNavigateToRegister: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    // Controladores de foco y teclado
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    // Focus Requesters
    val emailFocusRequester = remember { FocusRequester() }
    val passwordFocusRequester = remember { FocusRequester() }
    val loginButtonFocusRequester = remember { FocusRequester() } // Para el botón

    // Estado para la validación del email
    var isEmailValid by remember { mutableStateOf(true) }
    // Estado para la visibilidad de la contraseña
    var passwordVisible by remember { mutableStateOf(false) }

    LaunchedEffect(key1 = true) {
        // 1. Poner el foco en el email cuando se abra la pantalla
        emailFocusRequester.requestFocus()

        viewModel.effect.collectLatest { effect ->
            when (effect) {
                is LoginEffect.ShowSnackbar -> {
                    snackbarHostState.showSnackbar(effect.message)
                }
                LoginEffect.NavigateToHome -> {
                    onNavigateToHome()
                }
                LoginEffect.NavigateToRegister -> {
                    onNavigateToRegister()
                }
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValuesOuter ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValuesOuter)
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(250.dp)
                        .background(Color.White)
                        .padding(start = 10.dp, end = 10.dp, top = 5.dp, bottom = 15.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Image(
                        painter = painterResource(R.drawable.logofp250),
                        contentDescription = "Logo",
                        modifier = Modifier.fillMaxSize()
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = uiState.emailInput,
                    onValueChange = {
                        viewModel.onEvent(LoginUiEvent.EmailChanged(it))
                        // 2. Validación de email en tiempo real (opcional, pero útil)
                        isEmailValid = Patterns.EMAIL_ADDRESS.matcher(it).matches() || it.isEmpty()
                    },
                    label = { Text("Email") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(emailFocusRequester), // Aplicar FocusRequester
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Next // Acción para ir al siguiente campo
                    ),
                    keyboardActions = KeyboardActions(
                        onNext = {
                            // 3. Al oprimir "Realizado/Next" en email, pasar a contraseña
                            isEmailValid = Patterns.EMAIL_ADDRESS.matcher(uiState.emailInput).matches()
                            if (isEmailValid) {
                                passwordFocusRequester.requestFocus()
                            }
                            // Si quieres forzar el foco incluso si no es válido, elimina el if
                            // passwordFocusRequester.requestFocus()
                        }
                    ),
                    isError = !isEmailValid && uiState.emailInput.isNotEmpty(), // Mostrar error si no es válido y no está vacío
                    supportingText = { // Mensaje de error
                        if (!isEmailValid && uiState.emailInput.isNotEmpty()) {
                            Text("Introduce un email válido")
                        }
                    }
                )
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = uiState.passwordInput,
                    onValueChange = { viewModel.onEvent(LoginUiEvent.PasswordChanged(it)) },
                    label = { Text("Contraseña") },
                    singleLine = true,
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(passwordFocusRequester), // Aplicar FocusRequester
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done // Acción para finalizar
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            // 5. Al oprimir "Realizado/Done" en contraseña
                            keyboardController?.hide() // Cerrar teclado
                            loginButtonFocusRequester.requestFocus() // Mover foco al botón de login
                        }
                    ),
                    // 4. Badge de visualización de contraseña
                    trailingIcon = {
                        val image = if (passwordVisible)
                            Icon(painter = (painterResource(id = R.drawable.baseline_visibility_24)), contentDescription = "Mostrar contraseña")
                        else Icon(painter = (painterResource(id = R.drawable.baseline_visibility_off_24)), contentDescription = "Ocultar contraseña")

                        val description = if (passwordVisible) "Ocultar contraseña" else "Mostrar contraseña"

                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            image
                        }
                    }
                )
                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = {
                        // Validar email antes de intentar login si no se hizo en onNext
                        isEmailValid = Patterns.EMAIL_ADDRESS.matcher(uiState.emailInput).matches()
                        if (isEmailValid && uiState.passwordInput.isNotEmpty()) { // Añadir otras validaciones si es necesario
                            viewModel.onEvent(LoginUiEvent.LoginClicked)
                        } else if (!isEmailValid) {
                            emailFocusRequester.requestFocus() // Si el email es inválido, devolver foco
                            // Opcionalmente mostrar un Snackbar si el login falla por validación
                        }
                        // Considerar también si la contraseña está vacía
                    },
                    enabled = !uiState.isLoading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(loginButtonFocusRequester) // Aplicar FocusRequester al botón
                ) {
                    if (uiState.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Text("Ingresar")
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                TextButton(onClick = { viewModel.onEvent(LoginUiEvent.NavigateToRegister) }) {
                    Text("¿No tienes cuenta? Regístrate")
                }
            }
        }
    }
}