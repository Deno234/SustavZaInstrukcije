package com.example.sustavzainstrukcije.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.sustavzainstrukcije.ui.utils.InstructorsHorizontalRow
import com.example.sustavzainstrukcije.ui.utils.SubjectDropdown
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

    var searchQuery by remember { mutableStateOf("") }
    var selectedSubject by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        authViewModel.fetchCurrentUserData()
    }

    val filteredInstructors = instructors.filter { instructor ->
        (searchQuery.isEmpty() || instructor.name.contains(searchQuery, ignoreCase = true)) &&
                (selectedSubject == null || instructor.subjects.contains(selectedSubject))
    }

    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
        Text(
            "Welcome ${userData?.name}!",
            style = MaterialTheme.typography.headlineMedium
        )
        Text(
            "Role: ${userData?.role}",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            label = { Text("Search by name") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        SubjectDropdown(
            selectedSubject = selectedSubject,
            onSubjectSelected = { selectedSubject = it },
            availableSubjects = instructors.flatMap { it.subjects }.distinct()
        )

        Spacer(modifier = Modifier.height(16.dp))

        when {
            isInstructorsLoading -> CircularProgressIndicator(modifier = Modifier.fillMaxWidth())
            filteredInstructors.isEmpty() -> Text("No instructors found", modifier = Modifier.padding(16.dp))
            else -> InstructorsHorizontalRow(title = "Filtered Instructors", instructors = filteredInstructors)
        }
    }
}
