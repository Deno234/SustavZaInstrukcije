package com.example.sustavzainstrukcije.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.sustavzainstrukcije.ui.data.User
import com.example.sustavzainstrukcije.ui.utils.InstructorHorizontalCard
import com.example.sustavzainstrukcije.ui.utils.InstructorsHorizontalRow
import com.example.sustavzainstrukcije.ui.utils.SubjectDropdown
import com.example.sustavzainstrukcije.ui.viewmodels.InstructorsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FindInstructorsScreen(
    navController: NavController,
    instructorsViewModel: InstructorsViewModel = viewModel()
) {
    val isInstructorsLoading by instructorsViewModel.loadingState.collectAsState()
    val subjects by instructorsViewModel.subjects.collectAsState()
    val filteredGroupedInstructors by instructorsViewModel.filteredInstructors.collectAsState()
    val ratingsMap by instructorsViewModel.ratingsByInstructorAndSubject.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedSubject by remember { mutableStateOf<String?>(null) }
    var sortByRating by remember { mutableStateOf(false) }

    LaunchedEffect(searchQuery, selectedSubject) {
        instructorsViewModel.filterInstructors(searchQuery, selectedSubject)
    }

    LaunchedEffect(selectedSubject) {
        selectedSubject?.let { subject ->
            instructorsViewModel.listenToAllInstructorRatingsForSubject(subject)
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Find Instructors", style = MaterialTheme.typography.headlineSmall) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Filter & Sort",
                style = MaterialTheme.typography.titleMedium
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                shape = MaterialTheme.shapes.large
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        label = { Text("Search by name") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    SubjectDropdown(
                        selectedSubject = selectedSubject,
                        onSubjectSelected = { selectedSubject = it },
                        availableSubjects = subjects
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        FilledTonalButton(onClick = { sortByRating = !sortByRating }) {
                            Text(if (sortByRating) "Sorted by Rating" else "Sorted by Name")
                        }
                    }
                }
            }

            if (isInstructorsLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (filteredGroupedInstructors.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No instructors found", style = MaterialTheme.typography.bodyLarge)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(32.dp)
                ) {
                    filteredGroupedInstructors.forEach { (subject, instructorList) ->
                        val enriched = instructorList.map { instructor ->
                            val key = instructor.id to subject
                            val ratingData = ratingsMap[key] ?: (0.0 to 0)
                            Triple(instructor, ratingData.first, ratingData.second)
                        }.let { list ->
                            if (sortByRating) list.sortedByDescending { it.second }
                            else list.sortedBy { it.first.name }
                        }

                        item {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                HorizontalDivider()
                                Text(
                                    text = subject,
                                    style = MaterialTheme.typography.titleLarge
                                )
                                InstructorsHorizontalRow(
                                    title = "",
                                    instructors = enriched.map { it.first },
                                    onCheckProfile = { instructorId ->
                                        navController.navigate("checkProfile/$instructorId")
                                    },
                                    cardContent = { instructor: User ->
                                        val key = instructor.id to subject
                                        val (avg, count) = ratingsMap[key] ?: (0.0 to 0)
                                        InstructorHorizontalCard(
                                            instructor = instructor,
                                            avgRating = avg,
                                            ratingCount = count,
                                            onCheckProfile = {
                                                navController.navigate("checkProfile/${instructor.id}")
                                            }
                                        )
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

