package com.johnfatoma.sana.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.johnfatoma.sana.auth.GoogleSignInManager
import kotlinx.coroutines.launch

/**
 * Sana AI — Sign-In Screen
 *
 * Deliberately just one button. No email/password fields, no
 * "create account" flow — per the explicit product decision to
 * keep this as simple as "continue with Google."
 */
@Composable
fun SignInScreen(
    webClientId: String,
    onSignedIn: (userId: String) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val signInManager = remember {
        GoogleSignInManager(context = context, webClientId = webClientId)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(PaddingValues(24.dp)),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "Sana",
            style = MaterialTheme.typography.headlineLarge,
        )

        Text(
            text = "Your personal AI assistant",
            style = MaterialTheme.typography.bodyMedium,
        )

        androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = {
                if (isLoading) return@Button
                isLoading = true
                errorMessage = null

                scope.launch {
                    when (val result = signInManager.signIn()) {
                        is GoogleSignInManager.SignInResult.Success -> {
                            isLoading = false
                            onSignedIn(result.userId)
                        }
                        is GoogleSignInManager.SignInResult.Failure -> {
                            isLoading = false
                            errorMessage = result.message
                        }
                        GoogleSignInManager.SignInResult.Cancelled -> {
                            isLoading = false
                            // User backed out of the picker — not an error worth showing
                        }
                    }
                }
            },
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.height(20.dp))
            } else {
                Text("Continue with Google")
            }
        }

        errorMessage?.let { message ->
            androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = message,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}
