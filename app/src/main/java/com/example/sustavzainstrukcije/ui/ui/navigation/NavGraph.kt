package com.example.sustavzainstrukcije.ui.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.sustavzainstrukcije.ui.screens.GoogleRegistrationScreen
import com.example.sustavzainstrukcije.ui.screens.HomeScreen
import com.example.sustavzainstrukcije.ui.screens.LoginScreen
import com.example.sustavzainstrukcije.ui.screens.MainScreen
import com.example.sustavzainstrukcije.ui.screens.RegisterScreen

@Composable
fun NavGraph(
    navController: NavHostController = rememberNavController(),
    onGoogleSignIn: () -> Unit,
    onGoogleRegistrationComplete: () -> Unit
) {
    NavHost(navController = navController, startDestination = "main") {
        composable("main") {
            MainScreen(
                onLoginClick = { navController.navigate("login") },
                onRegisterClick = { navController.navigate("register") },
                onGoogleSignInClick = onGoogleSignIn
            )
        }
        composable("register") {
            RegisterScreen(
                onRegistrationComplete = { navController.popBackStack() }
            )
        }
        composable("login") {
            LoginScreen(
                onLoginComplete = { navController.navigate("home") }
            )
        }
        composable("googleRegistration") {
            GoogleRegistrationScreen(
                onRegistrationComplete = onGoogleRegistrationComplete
            )
        }
        composable("home") {
            HomeScreen()
        }
    }
}
