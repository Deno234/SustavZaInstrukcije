package com.example.sustavzainstrukcije.ui.ui.navigation

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.sustavzainstrukcije.ui.screens.GoogleRegistrationScreen
import com.example.sustavzainstrukcije.ui.screens.HomeScreen
import com.example.sustavzainstrukcije.ui.screens.LoginScreen
import com.example.sustavzainstrukcije.ui.screens.PrijavaScreen
import com.example.sustavzainstrukcije.ui.screens.RegisterScreen
import com.example.sustavzainstrukcije.ui.viewmodels.AuthViewModel

@Composable
fun NavGraph(
    navController: NavHostController = rememberNavController(),
    onGoogleSignIn: () -> Unit,
    viewModel: AuthViewModel = viewModel()
) {
    NavHost(navController = navController, startDestination = "login") {
        composable("login") {
            LoginScreen(
                onLoginClick = { navController.navigate("prijava") },
                onRegisterClick = { navController.navigate("register") },
                onGoogleSignInClick = onGoogleSignIn
            )
        }
        composable("register") {
            RegisterScreen(
                onRegistrationComplete = { navController.popBackStack() }
            )
        }
        composable("prijava") {
            PrijavaScreen(
                onPrijavaComplete = { navController.navigate("home") }
            )
        }
        composable("googleRegistration") {
            GoogleRegistrationScreen(
                onRegistrationComplete = {
                    viewModel.checkUserInFirestore(viewModel.currentUserId)
                }
            )
        }
        composable("home") {
            HomeScreen()
        }
    }
}
