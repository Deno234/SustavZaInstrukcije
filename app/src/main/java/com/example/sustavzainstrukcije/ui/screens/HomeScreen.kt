package com.example.sustavzainstrukcije.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
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
import com.example.sustavzainstrukcije.ui.utils.InstructorsHorizontalRow
import com.example.sustavzainstrukcije.ui.viewmodels.AuthViewModel
import com.example.sustavzainstrukcije.ui.viewmodels.InstructorsViewModel

@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    authViewModel: AuthViewModel = viewModel(),
    instructorsViewModel: InstructorsViewModel = viewModel()
) {

    val userData by authViewModel.userData.collectAsState()
    val instructors by instructorsViewModel.instructors.collectAsState()
    val isInstructorsLoading by instructorsViewModel.loadingState.collectAsState()

    LaunchedEffect(Unit) {
        authViewModel.fetchCurrentUserData()
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        item {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "Welcome ${userData?.name}!",
                    style = MaterialTheme.typography.headlineMedium
                )
                Text(
                    "Role: ${userData?.role}",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (userData?.role == "student") {
            item {
                when {
                    isInstructorsLoading -> CircularProgressIndicator(modifier = Modifier.fillMaxWidth())
                    instructors.isEmpty() -> Text(
                        "No instructors available",
                        modifier = Modifier.padding(16.dp)
                    )
                    else -> InstructorsHorizontalRow(
                        title = "All instructors",
                        instructors = instructors
                    )
                }
            }
        }
    }
}