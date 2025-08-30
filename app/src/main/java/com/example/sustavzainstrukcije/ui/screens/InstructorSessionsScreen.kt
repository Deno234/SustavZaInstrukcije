package com.example.sustavzainstrukcije.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.example.sustavzainstrukcije.ui.data.InstructionSession
import com.example.sustavzainstrukcije.ui.viewmodels.SessionViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InstructorSessionsScreen(
    navController: NavHostController,
    sessionViewModel: SessionViewModel = viewModel()
) {
    val sessions by sessionViewModel.sessions.collectAsState()
    val userNames by sessionViewModel.userNames.collectAsState()
    val lastVisitedMap by sessionViewModel.lastVisitedMap.collectAsState()

    val scope = rememberCoroutineScope()
    var pendingRoute by remember { mutableStateOf<String?>(null) }
    var showConfirmOutsideHours by remember { mutableStateOf(false) }


    LaunchedEffect(Unit) {
        sessionViewModel.getInstructorSessions()
        sessionViewModel.loadUserNames()
    }

    LaunchedEffect(sessions) {
        sessionViewModel.listenToOnlineUsersForSessions(sessions.map { it.id })
        val sessionIds = sessions.map { it.id }
        sessionViewModel.fetchLastVisitedTimestamps(sessionIds)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("My Sessions") },
            navigationIcon = {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Go back")
                }
            },
            actions = {
                IconButton(onClick = { navController.navigate("create_session") }) {
                    Icon(Icons.Default.Add, contentDescription = "Create session")
                }
            }
        )

        if (sessions.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "No created sessions",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Button(
                        onClick = { navController.navigate("create_session") }
                    ) {
                        Text("Create first session")
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    Text(
                        text = "Total sessions: ${sessions.size}",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

                items(sessions) { session ->
                    SessionCard(
                        session = session,
                        onlineCount = sessionViewModel.onlineUsersMap.collectAsState().value[session.id]?.size ?: 0,
                        userNames = userNames,
                        lastVisited = lastVisitedMap[session.id],
                        onJoinClick = {
                            scope.launch {
                                val ok = sessionViewModel.checkInstructorIsWithinWorkingHours()
                                if (!ok) {
                                    pendingRoute = "whiteboard/${session.id}"
                                    showConfirmOutsideHours = true
                                } else {
                                    navController.navigate("whiteboard/${session.id}")
                                }
                            }
                        },
                        onContinueClick = {
                            scope.launch {
                                val ok = sessionViewModel.checkInstructorIsWithinWorkingHours()
                                if (!ok) {
                                    pendingRoute = "whiteboard/${session.id}"
                                    showConfirmOutsideHours = true
                                } else {
                                    navController.navigate("whiteboard/${session.id}")
                                }
                            }
                        }
                    )
                }
            }
        }
    }

    if (showConfirmOutsideHours) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showConfirmOutsideHours = false },
            title = { Text("Outside working hours") },
            text = { Text("You are outside your defined working hours. Enter session anyway?") },
            dismissButton = {
                TextButton(onClick = { showConfirmOutsideHours = false }) {
                    Text("Cancel")
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val route = pendingRoute
                    showConfirmOutsideHours = false
                    pendingRoute = null
                    if (route != null) navController.navigate(route)
                }) { Text("Enter") }
            }
        )
    }

}

@Composable
fun SessionCard(
    session: InstructionSession,
    onlineCount: Int,
    userNames: Map<String, String>,
    lastVisited: Long?,
    onJoinClick: () -> Unit,
    onContinueClick: () -> Unit,
    sessionViewModel: SessionViewModel = viewModel()
) {

    val invitedNames = listOfNotNull(
        userNames[session.instructorId]
    ) + session.studentIds.mapNotNull { userNames[it] }

    val invitedNamesText = invitedNames.joinToString(", ")

    var showDeleteDialog by remember { mutableStateOf(false) }


    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        val formatter = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())

        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Subject: ${session.subject}",
                    style = MaterialTheme.typography.titleMedium
                )

                OutlinedButton(
                    onClick = { showDeleteDialog = true },
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Delete")
                }
            }

            Text(
                text = "Users in session: $onlineCount",
                style = MaterialTheme.typography.bodyMedium,
            )

            Text(
                text = "Invited: $invitedNamesText",
                style = MaterialTheme.typography.bodySmall
            )

            Text(
                text = "Created: ${formatter.format(Date(session.createdAt))}",
                style = MaterialTheme.typography.bodySmall
            )

            if (lastVisited != null) {
                Text("Last visited: ${formatter.format(Date(lastVisited))}", style = MaterialTheme.typography.bodySmall)
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (session.startedAt != null) {
                    Button(
                        onClick = onContinueClick,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Enter Session")
                    }
                } else {
                    Button(
                        onClick = onJoinClick,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Start Session")
                    }
                }
            }

            if (showDeleteDialog) {
                androidx.compose.material3.AlertDialog(
                    onDismissRequest = { showDeleteDialog = false },
                    title = { Text("Delete Session") },
                    text = { Text("Are you sure you want to remove this session from your list? This action will not delete it for other users.") },
                    confirmButton = {
                        TextButton(onClick = {
                            sessionViewModel.hideSessionForCurrentUser(session.id)
                            showDeleteDialog = false
                        }) {
                            Text("Yes, Delete")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDeleteDialog = false }) {
                            Text("Cancel")
                        }
                    }
                )
            }


        }
    }
}
