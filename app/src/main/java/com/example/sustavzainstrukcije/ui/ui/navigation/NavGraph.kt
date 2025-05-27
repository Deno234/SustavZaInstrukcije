package com.example.sustavzainstrukcije.ui.ui.navigation

import androidx.compose.material3.Text
import com.example.sustavzainstrukcije.ui.screens.MessagesScreen
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.sustavzainstrukcije.ui.screens.ChatScreen
import com.example.sustavzainstrukcije.ui.screens.CheckProfileScreen
import com.example.sustavzainstrukcije.ui.screens.GoogleRegistrationScreen
import com.example.sustavzainstrukcije.ui.screens.HomeScreen
import com.example.sustavzainstrukcije.ui.screens.LoginScreen
import com.example.sustavzainstrukcije.ui.screens.MainScreen
import com.example.sustavzainstrukcije.ui.screens.ProfileScreen
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
            HomeScreen(navController)
        }

        composable("profile") {
            ProfileScreen()
        }

        composable("checkProfile/{instructorId}") { backStackEntry ->
            val instructorId = backStackEntry.arguments?.getString("instructorId") ?: ""
            CheckProfileScreen(instructorId = instructorId, navController = navController)
        }

        composable("chat/{chatId}/{otherUserId}") { backStackEntry ->
            val chatId = backStackEntry.arguments?.getString("chatId") ?: ""
            val otherUserId = backStackEntry.arguments?.getString("otherUserId") ?: ""

            if (chatId.isNotEmpty() && otherUserId.isNotEmpty()) {
                ChatScreen(
                    chatId = chatId,
                    otherUserId = otherUserId,
                    navController = navController
                )
            } else {
                Text("Greška: ID chata ili ID drugog korisnika nedostaje.")
            }
        }

        composable("messages/{userId}") { backStackEntry ->
            val userId = backStackEntry.arguments?.getString("userId") ?: ""
            MessagesScreen(userId = userId, navController = navController)
        }

        /*composable("appointments") {
            AppointmentsScreen()
        }

        composable("otherInstructors") {
            OtherInstructorsScreen()
        }*/
    }
}
