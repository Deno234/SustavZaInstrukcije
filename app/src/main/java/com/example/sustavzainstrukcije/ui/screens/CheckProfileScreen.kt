package com.example.sustavzainstrukcije.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.sustavzainstrukcije.ui.utils.Constants.ORDERED_DAYS
import com.example.sustavzainstrukcije.ui.utils.RatingBar
import com.example.sustavzainstrukcije.ui.utils.generateChatId
import com.example.sustavzainstrukcije.ui.viewmodels.AuthViewModel
import com.example.sustavzainstrukcije.ui.viewmodels.InstructorsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CheckProfileScreen(
    instructorId: String,
    navController: NavController,
    instructorsViewModel: InstructorsViewModel = viewModel(),
    authViewModel: AuthViewModel = viewModel()
) {
    val instructor by instructorsViewModel.checkedInstructor.collectAsState()
    var showTimeSlots by remember { mutableStateOf(false) }

    var selectedSubject by remember { mutableStateOf<String?>(null) }
    var rating by remember { mutableStateOf(0) }
    var comment by remember { mutableStateOf("") }
    val ratingsMap by instructorsViewModel.ratingsByInstructorAndSubject.collectAsState()

    LaunchedEffect(selectedSubject) {
        if (selectedSubject != null) {
            instructorsViewModel.listenToInstructorRatingsForSubject(instructorId, selectedSubject!!)
        }
    }

    LaunchedEffect(Unit) {
        instructorsViewModel.fetchCheckedInstructor(instructorId)
        authViewModel.fetchCurrentUserData()
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Instructor Details", style = MaterialTheme.typography.headlineSmall) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(instructor?.name ?: "Loading...", style = MaterialTheme.typography.headlineSmall)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(instructor?.email ?: "", style = MaterialTheme.typography.bodyMedium)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Subjects: ${instructor?.subjects?.joinToString(", ") ?: ""}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                if (!instructor?.subjects.isNullOrEmpty()) {
                    var expanded by remember { mutableStateOf(false) }
                    Box {
                        TextButton(onClick = { expanded = true }) {
                            Text(selectedSubject ?: "Choose subject")
                        }
                        DropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            instructor?.subjects?.forEach { subj ->
                                DropdownMenuItem(
                                    text = { Text(subj) },
                                    onClick = {
                                        selectedSubject = subj
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                if (selectedSubject != null) {
                    val (avg, count) = ratingsMap[instructorId to selectedSubject!!] ?: (0.0 to 0)

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        RatingBar(rating = avg.toFloat())
                        Text("($count ocjena)", style = MaterialTheme.typography.bodyMedium)

                        Text("Your rating:", style = MaterialTheme.typography.bodyMedium)
                        RatingBar(
                            rating = rating.toFloat(),
                            onRatingSelected = { newRating -> rating = newRating }
                        )

                        OutlinedTextField(
                            value = comment,
                            onValueChange = { comment = it },
                            label = { Text("Comment") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Button(
                            onClick = {
                                val studentId = authViewModel.currentUserId ?: return@Button
                                instructorsViewModel.addOrUpdateRating(
                                    instructorId,
                                    studentId,
                                    selectedSubject!!,
                                    rating,
                                    comment
                                )
                                comment = ""
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Submit Rating")
                        }
                    }
                }

                Button(
                    onClick = {
                        navController.navigate("chat/${authViewModel.currentUserId?.let {
                            generateChatId(it, instructorId)
                        }}/$instructorId")
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Chat with Instructor")
                }

                Button(
                    onClick = { showTimeSlots = !showTimeSlots },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (showTimeSlots) "Hide Time Slots" else "Show Time Slots")
                }

                if (showTimeSlots) {
                    instructor?.let {
                        TimeSlotsSection(
                            onDismissRequest = { showTimeSlots = false },
                            availableHours = it.availableHours
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TimeSlotsSection(onDismissRequest: () -> Unit, availableHours: Map<String, List<String>>) {
    val sortedAvailableHours = ORDERED_DAYS.associateWith { day ->
        availableHours[day] ?: emptyList()
    }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text("Selected Hours") },
        text = {
            LazyColumn(modifier = Modifier.heightIn(max = 400.dp)) {
                sortedAvailableHours.forEach { (day, slots) ->
                    if (slots.isNotEmpty()) {
                        item {
                            Text(
                                text = day,
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                        items(slots) { slot ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = slot,
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismissRequest) {
                Text("Close")
            }
        }
    )
}
