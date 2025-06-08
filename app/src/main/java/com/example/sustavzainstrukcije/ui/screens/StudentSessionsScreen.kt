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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.example.sustavzainstrukcije.ui.data.InstructionSession
import com.example.sustavzainstrukcije.ui.data.SessionInvitation
import com.example.sustavzainstrukcije.ui.viewmodels.SessionViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentSessionsScreen(
    navController: NavHostController,
    sessionViewModel: SessionViewModel = viewModel()
) {
    val sessions by sessionViewModel.sessions.collectAsState()
    val invitations by sessionViewModel.invitations.collectAsState()

    val lastVisited by sessionViewModel.lastVisitedMap.collectAsState()
    val onlineUsers by sessionViewModel.onlineUsersMap.collectAsState()

    LaunchedEffect(Unit) {
        sessionViewModel.getAllStudentSessions()
        sessionViewModel.getStudentInvitations()
        sessionViewModel.loadLastVisitedSessions()
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("My Sessions") },
            navigationIcon = {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            }
        )

        if (sessions.isEmpty() && invitations.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "No sessions",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Wait for invitations from instructors",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (invitations.isNotEmpty()) {
                    item {
                        Text(
                            text = "New invitations (${invitations.size})",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }

                    items(invitations) { invitation ->
                        InvitationCard(
                            invitation = invitation,
                            onAccept = {
                                sessionViewModel.acceptInvitation(invitation.id) {
                                    sessionViewModel.getAllStudentSessions()
                                    sessionViewModel.getStudentInvitations()
                                }
                            },
                            onDecline = {
                                sessionViewModel.declineInvitation(invitation.id) {
                                    sessionViewModel.getStudentInvitations()
                                }
                            }
                        )
                    }

                    item {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
                    }
                }

                item {
                    Text(
                        text = "All sessions (${sessions.size})",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

                items(sessions) { session ->
                    StudentSessionCard(
                        session = session,
                        lastVisited = lastVisited[session.id],
                        isInstructorOnline = onlineUsers[session.id]?.contains(session.instructorId) == true,
                        onJoinClick = {
                            navController.navigate("whiteboard/${session.id}")
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun InvitationCard(
    invitation: SessionInvitation,
    onAccept: () -> Unit,
    onDecline: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "New invitation",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary
                )

                Badge(
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Text("NOVO", color = MaterialTheme.colorScheme.onPrimary)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Subject: ${invitation.subject}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )

            Text(
                text = "Created at: ${Date(invitation.createdAt)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onAccept,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Check, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Accept")
                }

                OutlinedButton(
                    onClick = onDecline,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Close, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Decline")
                }
            }
        }
    }
}

@Composable
fun StudentSessionCard(
    session: InstructionSession,
    lastVisited: Long?,
    isInstructorOnline: Boolean,
    onJoinClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
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

                Badge(
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    val badgeText = when {
                        lastVisited == null -> "New"
                        isInstructorOnline -> "Active"
                        else -> "Idle"
                    }

                    Text(
                        text = badgeText,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Created: ${formatter.format(Date(session.createdAt))}",
                style = MaterialTheme.typography.bodySmall
            )

            lastVisited?.let {
                Text(
                    text = "Last visited: ${formatter.format(Date(it))}",
                    style = MaterialTheme.typography.bodySmall
                )
            }


            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = onJoinClick,
                modifier = Modifier.fillMaxWidth(),
                enabled = true
            ) {
                Text(
                    "Enter Session"
                )
            }
        }
    }
}

