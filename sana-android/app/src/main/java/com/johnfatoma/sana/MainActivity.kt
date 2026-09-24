package com.johnfatoma.sana

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.johnfatoma.sana.auth.SupabaseClientProvider
import com.johnfatoma.sana.ui.ChatViewModel
import com.johnfatoma.sana.ui.screens.ChatScreen
import com.johnfatoma.sana.ui.screens.SignInScreen

/**
 * Sana AI — Main Activity
 *
 * TODO before running: fill in these two placeholders. They're
 * intentionally centralized here rather than scattered.
 */
private const val BACKEND_BASE_URL = "https://your-app.onrender.com"
private const val GOOGLE_WEB_CLIENT_ID = "YOUR_WEB_CLIENT_ID.apps.googleusercontent.com"

class MainActivity : ComponentActivity() {

    private val requestPermissions = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { /* results not currently branched on — features degrade gracefully if denied */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestRuntimePermissions()

        setContent {
            MaterialTheme {
                Surface(modifier = Modifier) {
                    SanaNavHost()
                }
            }
        }
    }

    private fun requestRuntimePermissions() {
        val permissions = mutableListOf(Manifest.permission.RECORD_AUDIO)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        requestPermissions.launch(permissions.toTypedArray())
    }
}

@androidx.compose.runtime.Composable
private fun SanaNavHost() {
    val navController = rememberNavController()

    // If Supabase already has a session (e.g. app was reopened),
    // skip straight to chat instead of showing sign-in again.
    val startDestination = if (SupabaseClientProvider.currentUserId() != null) {
        "chat"
    } else {
        "sign_in"
    }

    NavHost(navController = navController, startDestination = startDestination) {
        composable("sign_in") {
            SignInScreen(
                webClientId = GOOGLE_WEB_CLIENT_ID,
                onSignedIn = {
                    navController.navigate("chat") {
                        popUpTo("sign_in") { inclusive = true }
                    }
                },
            )
        }

        composable("chat") {
            val application = androidx.compose.ui.platform.LocalContext.current
                .applicationContext as SanaApplication

            val viewModel: ChatViewModel = viewModel(
                factory = ChatViewModel.factory(application, BACKEND_BASE_URL),
            )

            ChatScreen(viewModel = viewModel)
        }
    }
}
