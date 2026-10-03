package com.app.kallior.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.Google
import io.github.jan.supabase.auth.providers.builtin.Email
import kallos.data.AuthenticationState
import kallos.data.SupabaseManager
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope

internal object AuthFormValidator {
    private val emailPattern = Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")

    fun loginError(email: String, password: String): String? = when {
        email.isBlank() || password.isBlank() -> "Email and password are required."
        !emailPattern.matches(email) -> "Enter a valid email address."
        else -> null
    }

    fun registrationError(email: String, password: String, confirmation: String): String? = when {
        loginError(email, password) != null -> loginError(email, password)
        password.length < 8 -> "Use a password with at least 8 characters."
        password != confirmation -> "Passwords do not match."
        else -> null
    }
}

@Composable
fun AuthenticationGate(content: @Composable () -> Unit) {
    val authState by SupabaseManager.authenticationStates().collectAsState(initial = AuthenticationState.Loading)
    when (authState) {
        AuthenticationState.Loading -> LoadingScreen()
        is AuthenticationState.Authenticated -> content()
        is AuthenticationState.ConfigurationError -> ConfigurationErrorScreen((authState as AuthenticationState.ConfigurationError).message)
        AuthenticationState.Unauthenticated -> AuthFlow()
    }
}


@Composable
private fun AuthFlow() {
    var registering by remember { mutableStateOf(false) }
    if (registering) RegistrationScreen(onBack = { registering = false }) else LoginScreen(onRegister = {
        registering = true
    })
}

@Composable
private fun LoginScreen(onRegister: () -> Unit) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    AuthLayout(title = "Welcome back", error = error, loading = loading) {
        OutlinedTextField(
            email,
            { email = it },
            label = { Text("Email") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        OutlinedTextField(
            password,
            { password = it },
            label = { Text("Password") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            visualTransformation = PasswordVisualTransformation()
        )
        Button(onClick = {
            error = AuthFormValidator.loginError(email, password) ?: run {
                loading = true
                scope.launch {
                    runCatching {
                        SupabaseManager.client.auth.signInWith(Email) {
                            this.email = email; this.password = password
                        }
                    }
                        .onFailure { error = it.message ?: "Unable to sign in." }
                    loading = false
                }
                null
            }
        }, enabled = !loading, modifier = Modifier.fillMaxWidth()) { Text("Log in") }
        Button(onClick = {
            loading = true
            scope.launch {
                runCatching {
                    SupabaseManager.client.auth.signInWith(Google)
                }
                    .onFailure { error = it.message ?: "Unable to start Google sign-in." }
                loading = false
            }
        }, enabled = !loading, modifier = Modifier.fillMaxWidth()) { Text("Continue with Google") }
        TextButton(
            onClick = onRegister,
            enabled = !loading,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        ) { Text("Create an account") }
    }
}

@Composable
private fun RegistrationScreen(onBack: () -> Unit) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmation by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var message by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    AuthLayout(title = "Create account", error = error, loading = loading) {
        OutlinedTextField(
            email,
            { email = it },
            label = { Text("Email") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        OutlinedTextField(
            password,
            { password = it },
            label = { Text("Password") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            visualTransformation = PasswordVisualTransformation()
        )
        OutlinedTextField(
            confirmation,
            { confirmation = it },
            label = { Text("Confirm password") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            visualTransformation = PasswordVisualTransformation()
        )
        Button(onClick = {
            error = AuthFormValidator.registrationError(email, password, confirmation) ?: run {
                loading = true
                scope.launch {
                    runCatching {
                        SupabaseManager.client.auth.signUpWith(Email) {
                            this.email = email; this.password = password
                        }
                    }
                        .onSuccess { message = "Check your email to confirm your account if confirmation is enabled." }
                        .onFailure { error = it.message ?: "Unable to create account." }
                    loading = false
                }
                null
            }
        }, enabled = !loading, modifier = Modifier.fillMaxWidth()) { Text("Create account") }
        Button(onClick = {
            loading = true
            scope.launch {
                runCatching {
                    SupabaseManager.client.auth.signInWith(Google)
                }
                    .onFailure { error = it.message ?: "Unable to start Google sign-in." }
                loading = false
            }
        }, enabled = !loading, modifier = Modifier.fillMaxWidth()) { Text("Continue with Google") }
        message?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
        TextButton(
            onClick = onBack,
            enabled = !loading,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        ) { Text("Back to login") }
    }
}

@Composable
private fun AuthLayout(title: String, error: String?, loading: Boolean, content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier.fillMaxSize().background(KalliorColors.PrimaryLayer).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(title, style = MaterialTheme.typography.headlineLarge)
        Spacer(Modifier.height(24.dp))
        content()
        error?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 12.dp)) }
        if (loading) CircularProgressIndicator(modifier = Modifier.padding(top = 16.dp))
    }
}

@Composable
private fun LoadingScreen() = AuthLayout("Restoring session", null, true) {}

@Composable
private fun ConfigurationErrorScreen(message: String) = AuthLayout("Setup required", message, false) {}
