package com.example.sustavzainstrukcije.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.sustavzainstrukcije.ui.utils.InstructorsHorizontalRow
import com.example.sustavzainstrukcije.ui.utils.SubjectDropdown
import com.example.sustavzainstrukcije.ui.viewmodels.InstructorsViewModel

@Composable
fun FindInstructorsScreen(navController: NavController, instructorsViewModel: InstructorsViewModel = viewModel()) {
    val isInstructorsLoading by instructorsViewModel.loadingState.collectAsState()
    val subjects by instructorsViewModel.subjects.collectAsState()
    val filteredGroupedInstructors by instructorsViewModel.filteredInstructors.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedSubject by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(searchQuery, selectedSubject) {
        instructorsViewModel.filterInstructors(searchQuery, selectedSubject)
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {

        Text(
            text = "Search Instructors",
            modifier = Modifier.padding(16.dp).align(Alignment.CenterHorizontally),
            style = MaterialTheme.typography.headlineLarge,
            textAlign = TextAlign.Center
        )

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
            availableSubjects = subjects
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (isInstructorsLoading) {
            CircularProgressIndicator(modifier = Modifier.fillMaxWidth())
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                filteredGroupedInstructors.forEach { (subject, instructorList) ->
                    item {
                        InstructorsHorizontalRow(title = subject, instructors = instructorList,
                            onCheckProfile = { instructorId ->
                                navController.navigate("checkProfile/$instructorId")
                            }
                        )
                    }
                }
            }
        }
    }
}