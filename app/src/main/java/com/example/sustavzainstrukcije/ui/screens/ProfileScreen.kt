package com.example.sustavzainstrukcije.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.navigation.NavHostController
import com.example.sustavzainstrukcije.ui.utils.AvailableHoursInput
import com.example.sustavzainstrukcije.ui.utils.Constants.INSTRUCTOR_SUBJECTS
import com.example.sustavzainstrukcije.ui.utils.SubjectSelector
import com.example.sustavzainstrukcije.ui.viewmodels.AuthViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    navController: NavHostController, authViewModel: AuthViewModel = viewModel()
) {
    val currentUser by authViewModel.userData.collectAsState()
    var name by remember { mutableStateOf("") }
    var subjects by remember { mutableStateOf(listOf<String>()) }
    var availableHours by remember { mutableStateOf(emptyMap<String, List<String>>()) }

    LaunchedEffect(currentUser) {
        authViewModel.fetchCurrentUserData()
        currentUser?.let { user ->
            name = user.name
            subjects = user.subjects
            availableHours = user.availableHours
        }
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
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        CenterAlignedTopAppBar(
            title = {
                Text(
                    text = "Profile",
                    style = MaterialTheme.typography.headlineSmall
                )
            },
            navigationIcon = {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            }
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
                },
                onNotify = TODO()
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
    }
}