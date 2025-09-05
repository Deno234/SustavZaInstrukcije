package com.example.sustavzainstrukcije.ui.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import com.example.sustavzainstrukcije.ui.screens.MessagesScreen
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.sustavzainstrukcije.ui.data.AuthState
import com.example.sustavzainstrukcije.ui.screens.ChatScreen
import com.example.sustavzainstrukcije.ui.screens.CheckProfileScreen
import com.example.sustavzainstrukcije.ui.screens.CreateSessionScreen
import com.example.sustavzainstrukcije.ui.screens.FindInstructorsScreen
import com.example.sustavzainstrukcije.ui.screens.GoogleRegistrationScreen
import com.example.sustavzainstrukcije.ui.screens.HomeScreen
import com.example.sustavzainstrukcije.ui.screens.InstructorSessionsScreen
import com.example.sustavzainstrukcije.ui.screens.LoginScreen
import com.example.sustavzainstrukcije.ui.screens.MainScreen
import com.example.sustavzainstrukcije.ui.screens.ProfileScreen
import com.example.sustavzainstrukcije.ui.screens.RatingsScreen
import com.example.sustavzainstrukcije.ui.screens.RegisterScreen
import com.example.sustavzainstrukcije.ui.screens.StudentSessionsScreen
import com.example.sustavzainstrukcije.ui.screens.WhiteboardScreen
import com.example.sustavzainstrukcije.ui.viewmodels.AuthViewModel

@Composable
fun NavGraph(
    navController: NavHostController = rememberNavController(),
    authViewModel: AuthViewModel,
    onGoogleSignIn: () -> Unit,
    onGoogleRegistrationComplete: () -> Unit
) {

    val authState by authViewModel.authState.collectAsState()

    LaunchedEffect(authState) {
        when (authState) {
            AuthState.Authenticated -> {
                navController.navigate("home") {
                    popUpTo(0) { inclusive = true }
                    launchSingleTop = true
                }
            }

            AuthState.NeedsRegistration -> {
                navController.navigate("googleRegistration") {
                    popUpTo(0) { inclusive = true }
                    launchSingleTop = true
                }
            }

            AuthState.Unauthenticated -> {
                navController.navigate("main") {
                    popUpTo(0) { inclusive = true }
                    launchSingleTop = true
                }
            }

            else -> {}
        }
    }


    val startDestination = when (authState) {
        AuthState.Authenticated -> "home"
        AuthState.NeedsRegistration -> "googleRegistration"
        AuthState.Unauthenticated -> "main"
        AuthState.Checking -> "loading"
    }

    NavHost(navController = navController, startDestination = startDestination) {

        composable("loading") {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }

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
                authViewModel = authViewModel,
                onLoginComplete = { navController.navigate("home") }
            )
        }

        composable("googleRegistration") {
            GoogleRegistrationScreen(
                onRegistrationComplete = onGoogleRegistrationComplete
            )
        }
        composable("home") {
            HomeScreen(navController, authViewModel)
        }

        composable("profile") {
            ProfileScreen(navController)
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
                Text("Error: Chat ID or ID of the other user is missing.")
            }
        }

        composable("messages/{userId}") { backStackEntry ->
            val userId = backStackEntry.arguments?.getString("userId") ?: ""
            MessagesScreen(userId = userId, navController = navController)
        }

        composable("findInstructorsScreen") {
            FindInstructorsScreen(navController)
        }

        composable("create_session") {
            CreateSessionScreen(navController)
        }

        composable("instructor_sessions") {
            InstructorSessionsScreen(navController)
        }

        composable("student_sessions") {
            StudentSessionsScreen(navController)
        }

        composable("whiteboard/{sessionId}") { backStackEntry ->
            val sessionId = backStackEntry.arguments?.getString("sessionId") ?: ""
            if (sessionId.isNotEmpty()) {
                WhiteboardScreen(
                    sessionId = sessionId,
                    navController = navController
                )
            } else {
                Text("Error: Missing session ID.")
            }
        }

        composable("ratings/{instructorId}") { backStackEntry ->
            val instructorId = backStackEntry.arguments?.getString("instructorId")!!
            RatingsScreen(navController, instructorId)
        }
    }
}
