package com.example.sustavzainstrukcije.ui.screens

import android.util.Log
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.example.sustavzainstrukcije.ui.viewmodels.AuthViewModel

@Composable
fun HomeScreen(
    navController: NavHostController,
    authViewModel: AuthViewModel = viewModel(),
) {
    val userData by authViewModel.userData.collectAsState()

    LaunchedEffect(Unit) {
        authViewModel.fetchCurrentUserData()
    }

    when (userData?.role) {
        "student" -> HomeScreenStudent(navController)
        "instructor" -> HomeScreenInstructor(navController, userData!!.id)
        else -> CircularProgressIndicator()
    }
}
