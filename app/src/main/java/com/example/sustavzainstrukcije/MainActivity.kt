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
import com.example.sustavzainstrukcije.ui.viewmodels.AuthViewModel
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val viewModel: AuthViewModel by viewModels()
    private lateinit var appNavController: NavHostController

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
        intent?.let { handleIntentExtras(it) }
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
        Log.d("MainActivity", "onNewIntent called with intent: $intent")
        setIntent(intent)
        handleIntentExtras(intent)
    }

    private fun handleIntentExtras(intent: Intent) {
        Log.d("MainActivity", "Handling intent extras. Action: ${intent.action}, Data: ${intent.dataString}, Extras: ${intent.extras}")
        if (intent.getStringExtra("navigateTo") == "ChatScreen") {
            val chatId = intent.getStringExtra("chatId")
            val otherUserId = intent.getStringExtra("otherUserId")
            Log.d("MainActivity", "Intent extras for ChatScreen: chatId=$chatId, otherUserId=$otherUserId")

            if (chatId != null && otherUserId != null) {
                if (::appNavController.isInitialized) {
                    appNavController.navigate("chat/$chatId/$otherUserId") {
                        launchSingleTop = true
                    }
                    Log.i("MainActivity", "Navigated to chat: $chatId with user: $otherUserId")
                } else {
                    Log.e("MainActivity", "NavController not initialized when trying to navigate from notification.")
                }
            } else {
                Log.w("MainActivity", "chatId or otherUserId is null in intent extras for ChatScreen.")
            }
        } else {
            Log.d("MainActivity", "No specific navigation action ('ChatScreen') in intent or navigateTo extra is missing/different.")
        }
    }
}

// Preview ostaje isti
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
