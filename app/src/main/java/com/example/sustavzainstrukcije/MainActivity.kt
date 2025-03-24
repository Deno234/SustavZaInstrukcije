package com.example.sustavzainstrukcije

import android.os.Bundle
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
import androidx.navigation.compose.rememberNavController
import com.example.sustavzainstrukcije.ui.screens.MainScreen
import com.example.sustavzainstrukcije.ui.theme.SustavZaInstrukcijeTheme
import com.example.sustavzainstrukcije.ui.ui.navigation.NavGraph
import com.example.sustavzainstrukcije.ui.viewmodels.AuthViewModel

class MainActivity : ComponentActivity() {

    /**
     * This creates a single instance of AuthViewModel tied to activity’s lifecycle.
     * Survives screen rotations (keeps data safe)
     * Separates business logic (sign-in, navigation) from UI code
     */
    private val viewModel: AuthViewModel by viewModels()

    private val signInLauncher = registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) {
        /**
         * ViewModels manage complex logic (e.g., validating sign-in tokens, navigating post-login).
         * Keeps activity code clean and focused on UI.
         * Follows MVVM pattern: UI displays data, ViewModel processes it
         */
        viewModel.handleSignInResult(it)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        setContent {
            SustavZaInstrukcijeTheme {
                /**
                 * Jetpack Compose requires the NavController to be created inside a composable function (like setContent).
                 * rememberNavController() ensures it survives recompositions (UI updates)
                 * By passing the navController from MainActivity, you ensure that all parts of your app share the same navigation controller.
                 */
                val navController = rememberNavController()


                /**
                 * Ensures the ViewModel can trigger navigation (e.g., after sign-in).
                 * LaunchedEffect runs this code once when the composable starts (safe for setup tasks).
                 * Avoids passing navController directly to the ViewModel (which could cause lifecycle issues)
                 */
                LaunchedEffect(navController) {
                    viewModel.setNavController(navController)
                }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    NavGraph(
                        navController = navController,
                        onGoogleSignIn = {
                            viewModel.initiateGoogleSignIn { intentRequest ->
                                signInLauncher.launch(intentRequest)
                            }
                        },
                        onGoogleRegistrationComplete = {
                            viewModel.checkUserInFirestore(viewModel.currentUserId)
                        }
                    )
                }
            }
        }
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