package com.adhithimaran.cultivate.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.launch
import com.adhithimaran.cultivate.BuildConfig

val WEB_CLIENT_ID = BuildConfig.WEB_CLIENT_ID

@Composable
fun LoginScreen (
    onLoginSuccess: () -> Unit,
    authViewModel: AuthViewModel = viewModel()
) {
    val context = LocalContext.current
    val authState by authViewModel.authState.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isSignUp by remember { mutableStateOf(false) }

    val credentialManager = remember { CredentialManager.create(context) }

    LaunchedEffect(authState) {
        if (authState is AuthState.Success) {
            onLoginSuccess()
            authViewModel.resetState()
        }
    }

    Column (
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Cultivate", style = MaterialTheme.typography.headlineLarge)
        Spacer(modifier = Modifier.height(32.dp))

        OutlinedTextField (
            value = email,
            onValueChange = { email = it },
            label = { Text("Email")},
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                if (isSignUp) authViewModel.signUpWithEmail(email, password)
                else authViewModel.signInWithEmail(email, password)
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = authState !is AuthState.Loading
        ) {
            Text(if (isSignUp) "Create Account" else "Sign In")
        }

        Spacer(modifier = Modifier.height(8.dp))

        TextButton(onClick = { isSignUp = !isSignUp }) {
            Text(if (isSignUp) "Already have an account? Sign in" else "No account? Sign up")
        }

        Spacer(modifier = Modifier.height(16.dp))

        HorizontalDivider()

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedButton(
            onClick = {
                scope.launch {
                    try {
                        val googleIdOption = GetGoogleIdOption.Builder()
                            .setServerClientId(WEB_CLIENT_ID)
                            .setFilterByAuthorizedAccounts(false)
                            .build()

                        val request = GetCredentialRequest.Builder()
                            .addCredentialOption(googleIdOption)
                            .build()

                        val result = credentialManager.getCredential(
                            request = request,
                            context = context
                        )

                        val googleIdToken = GoogleIdTokenCredential
                            .createFrom(result.credential.data)
                            .idToken

                        authViewModel.signInWithGoogleToken(googleIdToken)

                    } catch (e: GetCredentialException) {
                        // user cancelled or error — do nothing
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = authState !is AuthState.Loading
        ) {
            Text("Sign in with Google")
        }

        if (authState is AuthState.Loading) {
            Spacer(modifier = Modifier.height(16.dp))
            CircularProgressIndicator()
        }

        if (authState is AuthState.Error) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = (authState as AuthState.Error).message,
                color = MaterialTheme.colorScheme.error
            )
        }

    }

}
