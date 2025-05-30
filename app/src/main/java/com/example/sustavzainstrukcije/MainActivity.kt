package com.example.sustavzainstrukcije

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.content.ContextCompat
import androidx.credentials.exceptions.GetCredentialException
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.example.sustavzainstrukcije.ui.screens.MainScreen
import com.example.sustavzainstrukcije.ui.theme.SustavZaInstrukcijeTheme
import com.example.sustavzainstrukcije.ui.ui.navigation.NavGraph
import com.example.sustavzainstrukcije.ui.utils.generateChatId
import com.example.sustavzainstrukcije.ui.viewmodels.AuthViewModel
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val viewModel: AuthViewModel by viewModels()
    private lateinit var appNavController: NavHostController

    private var pendingChatNavigation: Pair<String, String>? = null

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                Log.d("NotificationPermission", "Notification permission granted.")
                viewModel.updateFcmToken()
            } else {
                Log.d("NotificationPermission", "Notification permission denied.")
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            SustavZaInstrukcijeTheme {
                appNavController = rememberNavController()

                LaunchedEffect(appNavController) {
                    viewModel.checkAuthState()
                }

                LaunchedEffect(viewModel.isAuthenticated.collectAsState().value, appNavController) {
                    val isAuth = viewModel.isAuthenticated.value
                    if (isAuth == true) {
                        handleIntentExtras(intent)
                    }
                }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    NavGraph(
                        navController = appNavController,
                        authViewModel = viewModel,
                        onGoogleSignIn = {
                            viewModel.initiateGoogleSignIn { request ->
                                lifecycleScope.launch {
                                    try {
                                        val credential = viewModel.credentialManager.getCredential(
                                            context = this@MainActivity,
                                            request = request
                                        )
                                        viewModel.handleSignInResult(credential)
                                    } catch (e: GetCredentialException) {
                                        viewModel.handleSignInError(e)
                                    }
                                }
                            }
                        },
                        onGoogleRegistrationComplete = {
                            viewModel.checkUserInFirestore(viewModel.currentUserId)
                        }
                    )
                }
            }
        }
        askNotificationPermission()
    }

    private fun askNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED -> {
                    Log.d("NotificationPermission", "Permission already granted.")
                    viewModel.updateFcmToken()
                }
                shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS) -> {
                    Log.d("NotificationPermission", "Showing rationale for notification permission.")
                    requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
                else -> {
                    Log.d("NotificationPermission", "Requesting notification permission.")
                    requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        } else {
            Log.d("NotificationPermission", "No runtime permission needed for notifications (API < 33).")
            viewModel.updateFcmToken()
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        Log.d("MainActivity", "onNewIntent called - app was already running")
        setIntent(intent)

        // Provjeri je li korisnik autentificiran prije navigacije
        if (viewModel.isAuthenticated.value == true) {
            handleIntentExtras(intent)
        } else {
            Log.d("MainActivity", "User not authenticated in onNewIntent, will handle after auth")
        }
    }

    private fun handleIntentExtras(intent: Intent) {
        Log.d("MainActivity", "=== HANDLING INTENT EXTRAS ===")
        Log.d("MainActivity", "Intent action: ${intent.action}")
        Log.d("MainActivity", "Intent flags: ${intent.flags}")

        intent.extras?.let { extras ->
            Log.d("MainActivity", "Found ${extras.size()} extras:")
            for (key in extras.keySet()) {
                Log.d("MainActivity", "  Extra: $key = ${extras.get(key)}")
            }
        } ?: Log.d("MainActivity", "No extras found in intent")

        // Provjeri i FCM podatke i MyFirebaseMessagingService podatke
        val navigateTo = intent.getStringExtra("navigateTo")
        val chatId = intent.getStringExtra("chatId")
        val otherUserId = intent.getStringExtra("otherUserId")

        Log.d("MainActivity", "NavigateTo value: '$navigateTo'")
        Log.d("MainActivity", "ChatId: '$chatId', OtherUserId: '$otherUserId'")

        // Provjeri ima li podataka za navigaciju (bilo iz MyFirebaseMessagingService ili direktno iz FCM)
        if ((navigateTo == "ChatScreen" || (chatId != null && otherUserId != null)) &&
            chatId != null && otherUserId != null) {

            Log.d("MainActivity", "Chat navigation requested: chatId=$chatId, otherUserId=$otherUserId")

            if (::appNavController.isInitialized) {
                val currentUserId = viewModel.currentUserId
                Log.d("MainActivity", "Current user ID: $currentUserId")

                if (currentUserId != null) {
                    Log.d("MainActivity", "All conditions met, attempting navigation...")

                    lifecycleScope.launch {
                        kotlinx.coroutines.delay(1000)

                        try {
                            appNavController.navigate("chat/$chatId/$otherUserId") {
                                launchSingleTop = true
                            }
                            Log.i("MainActivity", "✅ Successfully navigated to chat: $chatId with user: $otherUserId")

                            // Očisti intent
                            intent.removeExtra("navigateTo")
                            intent.removeExtra("chatId")
                            intent.removeExtra("otherUserId")
                            intent.action = null
                        } catch (e: Exception) {
                            Log.e("MainActivity", "❌ Navigation failed: ${e.message}", e)
                        }
                    }
                } else {
                    Log.w("MainActivity", "❌ Current user ID is null, cannot navigate to chat")
                }
            } else {
                Log.w("MainActivity", "❌ NavController not initialized")
            }
        } else {
            Log.d("MainActivity", "No ChatScreen navigation requested. NavigateTo: '$navigateTo', ChatId: '$chatId', OtherUserId: '$otherUserId'")
        }
        Log.d("MainActivity", "=== END HANDLING INTENT EXTRAS ===")
    }


}

@Preview(showBackground = true)
@Composable
fun LoginScreenPreview() {
    SustavZaInstrukcijeTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            MainScreen(
                onLoginClick = {},
                onRegisterClick = {},
                onGoogleSignInClick = {}
            )
        }
    }
}
