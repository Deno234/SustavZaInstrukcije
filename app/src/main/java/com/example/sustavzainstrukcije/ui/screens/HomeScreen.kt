package com.example.sustavzainstrukcije.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.sustavzainstrukcije.ui.viewmodels.AuthViewModel

@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    viewModel: AuthViewModel = viewModel()
) {

    LaunchedEffect(Unit) {
        viewModel.fetchCurrentUserData()
    }
    val userData by viewModel.userData.collectAsState()

    Column(modifier.fillMaxSize().padding(16.dp)) {
        if (userData == null) {
            CircularProgressIndicator()
        } else {
            Text("Welcome ${userData?.name}", style = MaterialTheme.typography.headlineMedium)
            Text("Role: ${userData?.role}")

            if (userData?.role == "instructor") {
                Text("Subjects:", style = MaterialTheme.typography.titleMedium)
                userData?.subjects?.forEach { subject ->
                    Text("- $subject")
                }

                Text("Availability:", style = MaterialTheme.typography.titleMedium)
                userData?.availableHours?.forEach { (day, hours) ->
                    Text("$day: ${hours.joinToString()}")
                }
            }
        }
    }
}