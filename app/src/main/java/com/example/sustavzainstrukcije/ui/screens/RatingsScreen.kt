package com.example.sustavzainstrukcije.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.sustavzainstrukcije.ui.utils.RatingBar
import com.example.sustavzainstrukcije.ui.utils.SubjectDropdown
import com.example.sustavzainstrukcije.ui.viewmodels.AuthViewModel
import com.example.sustavzainstrukcije.ui.viewmodels.InstructorsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RatingsScreen(
    navController: NavController,
    instructorId: String,
    instructorsViewModel: InstructorsViewModel = viewModel(),
    authViewModel: AuthViewModel = viewModel()
) {
    val instructor by instructorsViewModel.checkedInstructor.collectAsState()
    val ratingsMap by instructorsViewModel.ratingsByInstructorAndSubject.collectAsState()
    val commentsMap by instructorsViewModel.commentsByInstructorAndSubject.collectAsState()

    var selectedSubject by remember { mutableStateOf<String?>(null) }
    var rating by remember { mutableStateOf(0) }
    var comment by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(instructorId) {
        instructorsViewModel.fetchCheckedInstructor(instructorId)
        instructorsViewModel.listenToAllInstructorRatings()
        instructorsViewModel.listenToAllInstructorComments(instructorId)
    }


    LaunchedEffect(Unit) {
        if (authViewModel.userData.value == null) {
            authViewModel.fetchCurrentUserData()
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Ratings & Comments") },
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
            if (!instructor?.subjects.isNullOrEmpty()) {
                SubjectDropdown(
                    selectedSubject = selectedSubject,
                    onSubjectSelected = { subjOrNull ->
                        selectedSubject = subjOrNull
                        subjOrNull?.let {
                            instructorsViewModel.listenToInstructorRatingsForSubject(instructorId, it)
                        }
                    },
                    availableSubjects = instructor?.subjects ?: emptyList()
                )
            }

            if (selectedSubject != null) {
                val (avg, count) = ratingsMap[instructorId to selectedSubject!!] ?: (0.0 to 0)
                val comments = commentsMap[instructorId to selectedSubject!!] ?: emptyList()

                Text("Average Rating: %.1f".format(avg))
                RatingBar(rating = avg.toFloat())
                Text("($count ratings)")

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f, fill = true)
                ) {
                    items(comments) { c ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Text(c.studentName ?: "Anon", style = MaterialTheme.typography.bodySmall)
                                Text("Subject: ${c.subject}", style = MaterialTheme.typography.labelSmall)
                                Text(c.text, style = MaterialTheme.typography.bodyMedium)
                                Spacer(Modifier.height(4.dp))
                                RatingBar(rating = c.rating.toFloat())
                            }
                        }
                    }

                }

                RatingBar(
                    rating = rating.toFloat(),
                    onRatingSelected = {
                        newRating -> rating = newRating
                        errorMessage = null },
                    starSize = 35.dp
                )
                OutlinedTextField(
                    value = comment,
                    onValueChange = {
                        comment = it
                        errorMessage = null},
                    label = { Text("Your Comment") },
                    modifier = Modifier.fillMaxWidth(),
                    isError = errorMessage != null && comment.isBlank()
                )
                if (errorMessage != null) {
                    Text(
                        text = errorMessage ?: "",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Button(
                    onClick = {
                        when {
                            rating == 0 -> {
                                errorMessage = "Please select a rating."
                            }
                            comment.isBlank() -> {
                                errorMessage = "Please enter a comment."
                            }
                            else -> {
                                val studentId = authViewModel.currentUserId ?: return@Button
                                val studentName = authViewModel.userData.value?.name ?: "Unknown"

                                instructorsViewModel.addOrUpdateRating(
                                    instructorId = instructorId,
                                    studentId = studentId,
                                    subject = selectedSubject!!,
                                    rating = rating,
                                    comment = comment,
                                    studentName = studentName
                                )
                                comment = ""
                                rating = 0
                                errorMessage = null
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Submit")
                }

            } else {
                val avgAll = instructorsViewModel.ratingsByInstructor.value[instructorId]?.first ?: 0.0
                val countAll = instructorsViewModel.ratingsByInstructor.value[instructorId]?.second ?: 0
                val commentsMapSecond by instructorsViewModel.commentsByInstructorAndSubject.collectAsState()

                val allComments = commentsMapSecond
                    .filterKeys { it.first == instructorId }
                    .flatMap { it.value }


                Text("Average Rating: %.1f".format(avgAll))
                RatingBar(rating = avgAll.toFloat())
                Text("($countAll ratings)")

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f, fill = true)
                ) {
                    items(allComments) { c ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Text(c.studentName ?: "Anon", style = MaterialTheme.typography.bodySmall)
                                Text("Subject: ${c.subject}", style = MaterialTheme.typography.labelSmall)
                                Text(c.text, style = MaterialTheme.typography.bodyMedium)
                                Spacer(Modifier.height(4.dp))
                                RatingBar(rating = c.rating.toFloat())
                            }
                        }
                    }
                }
            }
        }
    }
}
