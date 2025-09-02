package com.example.sustavzainstrukcije.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.sustavzainstrukcije.ui.utils.*
import com.example.sustavzainstrukcije.ui.viewmodels.AuthViewModel
import com.example.sustavzainstrukcije.ui.viewmodels.InstructorsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CheckProfileScreen(
    navController: NavController,
    instructorId: String,
    instructorsViewModel: InstructorsViewModel = viewModel(),
    authViewModel: AuthViewModel = viewModel()
) {
    val instructor by instructorsViewModel.checkedInstructor.collectAsState()

    var showHoursDialog by remember { mutableStateOf(false) }
    var showImageDialog by remember { mutableStateOf(false) }

    LaunchedEffect(instructorId) {
        instructorsViewModel.fetchCheckedInstructor(instructorId)
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Instructor Profile") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                    shape = MaterialTheme.shapes.large
                ) {
                    Column(
                        modifier = Modifier
                            .padding(20.dp)
                            .fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (!instructor?.profilePictureUrl.isNullOrEmpty()) {
                                AsyncImage(
                                    model = instructor?.profilePictureUrl,
                                    contentDescription = "Profile picture",
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(CircleShape)
                                        .clickable { showImageDialog = true },
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                val initial = instructor?.name?.firstOrNull()?.uppercase() ?: "?"
                                if (initial.isNotBlank()) {
                                    Text(
                                        text = initial,
                                        style = MaterialTheme.typography.headlineMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = "Avatar",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }


                        Text(
                            instructor?.name ?: "Loading...",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            instructor?.email ?: "",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (!instructor?.subjects.isNullOrEmpty()) {
                            Text(
                                "Subjects: ${instructor?.subjects?.joinToString(", ")}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }

            if (!instructor?.subjects.isNullOrEmpty()) {
                item {
                    FilledTonalButton(
                        onClick = {
                            navController.navigate("ratings/$instructorId")
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("View Ratings & Comments")
                    }
                }
            }

            item {
                FilledTonalButton(
                    onClick = {
                        navController.navigate(
                            "chat/${authViewModel.currentUserId?.let { generateChatId(it, instructorId) }}/$instructorId"
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Chat with Instructor")
                }
            }

            if (instructor != null) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text("Available Hours", style = MaterialTheme.typography.titleMedium)

                            Button(onClick = { showHoursDialog = true }, modifier = Modifier.fillMaxWidth()) {
                                Text("Show Available Hours")
                            }
                        }
                    }
                }
            }
        }

        if (showHoursDialog && instructor != null) {
            AlertDialog(
                onDismissRequest = { showHoursDialog = false },
                title = { Text("Available Hours") },
                text = {
                    val orderedDays = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")
                    Column {
                        orderedDays.forEach { day ->
                            val slots = instructor!!.availableHours[day] ?: emptyList()
                            if (slots.isNotEmpty()) {
                                Text(day, style = MaterialTheme.typography.titleMedium)
                                slots.forEach { slot ->
                                    Text("- $slot", style = MaterialTheme.typography.bodyMedium)
                                }
                                Spacer(Modifier.height(8.dp))
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showHoursDialog = false }) {
                        Text("Close")
                    }
                }
            )
        }

        if (showImageDialog && !instructor?.profilePictureUrl.isNullOrEmpty()) {
            Dialog(onDismissRequest = { showImageDialog = false }) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.onBackground.copy(alpha = 0.95f)),
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = instructor?.profilePictureUrl,
                        contentDescription = "Profile picture fullscreen",
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { showImageDialog = false },
                        contentScale = ContentScale.Fit
                    )
                }
            }
        }

    }
}
