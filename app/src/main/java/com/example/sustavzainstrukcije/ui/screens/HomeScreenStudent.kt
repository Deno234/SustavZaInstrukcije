package com.example.sustavzainstrukcije.ui.screens

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.example.sustavzainstrukcije.ui.viewmodels.AuthViewModel

@Composable
fun HomeScreenStudent(navController: NavHostController, userId: String?, authViewModel: AuthViewModel = viewModel()) {
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
            onClick = { navController.navigate("appointments") },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("My Appointments")
        }

        Button(
            onClick = { navController.navigate("messages/$userId") },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("My Messages")
        }

        Button(
            onClick = { navController.navigate("findInstructorsScreen") },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("View Instructors")
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