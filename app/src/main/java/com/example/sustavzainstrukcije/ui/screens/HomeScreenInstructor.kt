package com.example.sustavzainstrukcije.ui.screens

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.example.sustavzainstrukcije.ui.viewmodels.AuthViewModel
import com.example.sustavzainstrukcije.ui.viewmodels.SessionViewModel

@Composable
fun HomeScreenInstructor(navController: NavHostController, instructorId: String?, authViewModel: AuthViewModel = viewModel(), sessionViewModel: SessionViewModel = viewModel()) {
    Log.d("HomeScreenInstructor", "Instructor id: $instructorId")

    LaunchedEffect(Unit) {
        sessionViewModel.getInstructorSessions()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically)
    ) {

        Button(
            onClick = { navController.navigate("profile") },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("My Profile")
        }

        Button(
            onClick = { navController.navigate("instructor_sessions") },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("My Sessions")
        }

        Button(
            onClick = { navController.navigate("messages/$instructorId") },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("My Messages")
        }

        Button(
            onClick = { authViewModel.signOut() },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.error
            )
        ) {
            Text("Sign Out", color = MaterialTheme.colorScheme.onError)
        }
    }
}