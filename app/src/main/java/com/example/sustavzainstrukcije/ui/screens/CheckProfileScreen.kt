package com.example.sustavzainstrukcije.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.navigation.NavController
import com.example.sustavzainstrukcije.ui.utils.Constants.ORDERED_DAYS
import com.example.sustavzainstrukcije.ui.utils.generateChatId
import com.example.sustavzainstrukcije.ui.viewmodels.AuthViewModel
import com.example.sustavzainstrukcije.ui.viewmodels.InstructorsViewModel

@Composable
fun CheckProfileScreen(
    instructorId: String,
    navController: NavController,
    instructorsViewModel: InstructorsViewModel = viewModel(),
    authViewModel: AuthViewModel = viewModel()
) {
    val instructor by instructorsViewModel.checkedInstructor.collectAsState()
    var showTimeSlots by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        instructorsViewModel.fetchCheckedInstructor(instructorId)
        authViewModel.fetchCurrentUserData()
    }

    Column(modifier = Modifier
        .fillMaxSize()
        .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally)
    {
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = instructor?.name ?: "Loading...",
                    style = MaterialTheme.typography.headlineSmall
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = instructor?.email ?: "",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Subjects: ${instructor?.subjects?.joinToString(", ") ?: ""}",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = { navController.navigate("chat/${authViewModel.currentUserId?.let {
                generateChatId(
                    it, instructorId)
            }}/$instructorId")},
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Chat with Instructor")
        }
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = { showTimeSlots = !showTimeSlots },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (showTimeSlots) "Hide Time Slots" else "Show Time Slots")
        }

        if (showTimeSlots) {
            instructor?.let { TimeSlotsSection(onDismissRequest = { showTimeSlots = false }, it.availableHours) }
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