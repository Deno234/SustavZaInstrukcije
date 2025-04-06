package com.example.sustavzainstrukcije.ui.screens

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
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
import com.example.sustavzainstrukcije.ui.utils.Constants.INSTRUCTOR_SUBJECTS
import com.example.sustavzainstrukcije.ui.viewmodels.AuthViewModel

@Composable
fun ProfileScreenInstructor(
    authViewModel: AuthViewModel = viewModel(),
) {
    val currentUser by authViewModel.userData.collectAsState()
    var name by remember { mutableStateOf(currentUser?.name ?: "") }
    var subjects by remember { mutableStateOf(currentUser?.subjects ?: listOf()) }
    Log.d("ProfileScreenInstructor", "Subjects: $subjects")

    LaunchedEffect(Unit) {
        authViewModel.fetchCurrentUserData()
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

        Text(text = "Subjects", style = MaterialTheme.typography.bodyLarge)

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(INSTRUCTOR_SUBJECTS) { subject ->
                val isSelected = subjects.contains(subject)
                AssistChip(
                    onClick = {
                        subjects = if (isSelected) {
                            subjects - subject
                        } else {
                            subjects + subject
                        }
                    },
                    label = { Text(subject) },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor =
                            if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                authViewModel.updateInstructorProfile(name, subjects)
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Save Changes")
        }
    }
}
