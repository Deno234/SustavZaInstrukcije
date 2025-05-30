package com.example.sustavzainstrukcije.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.sustavzainstrukcije.ui.utils.AvailableHoursInput
import com.example.sustavzainstrukcije.ui.utils.Constants.INSTRUCTOR_SUBJECTS
import com.example.sustavzainstrukcije.ui.utils.SubjectSelector
import com.example.sustavzainstrukcije.ui.viewmodels.AuthViewModel

@Composable
fun ProfileScreen(
    authViewModel: AuthViewModel = viewModel(),
) {
    val currentUser by authViewModel.userData.collectAsState()
    var name by remember { mutableStateOf(currentUser?.name ?: "") }
    var subjects by remember { mutableStateOf(currentUser?.subjects ?: listOf()) }
    var availableHours by remember { mutableStateOf(currentUser?.availableHours ?: emptyMap()) }

    LaunchedEffect(Unit) {
        authViewModel.fetchCurrentUserData()
    }

    if (currentUser == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Profile",
            style = MaterialTheme.typography.headlineLarge,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )

        Text(
            text = "Email: ${currentUser?.email ?: "N/A"}",
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(vertical = 8.dp)
        )

        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Name") },
            modifier = Modifier.fillMaxWidth()
        )

        if (currentUser?.role == "instructor") {
            Text(text = "Subjects", style = MaterialTheme.typography.bodyLarge)

            SubjectSelector(
                subjects = INSTRUCTOR_SUBJECTS,
                selectedSubjects = subjects,
                onSubjectSelected = { subject ->
                    subjects = subjects + subject
                },
                onSubjectDeselected = { subject ->
                    subjects = subjects - subject
                }
            )

            AvailableHoursInput(
                availableHours = availableHours,
                onHoursUpdated = { updatedHours ->
                    availableHours = updatedHours
                }
            )

            Spacer(modifier = Modifier.height(16.dp))
        }

        Button(
            onClick = {
                authViewModel.updateInstructorProfile(name, subjects, availableHours)
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Save Changes")
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