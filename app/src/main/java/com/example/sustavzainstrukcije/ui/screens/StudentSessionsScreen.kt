package com.example.sustavzainstrukcije.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.example.sustavzainstrukcije.ui.data.InstructionSession
import com.example.sustavzainstrukcije.ui.data.SessionInvitation
import com.example.sustavzainstrukcije.ui.viewmodels.SessionViewModel
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentSessionsScreen(
    navController: NavHostController,
    sessionViewModel: SessionViewModel = viewModel()
) {
    val sessions by sessionViewModel.sessions.collectAsState()
    val invitations by sessionViewModel.invitations.collectAsState()

    LaunchedEffect(Unit) {
        sessionViewModel.getAllStudentSessions() // Koristi novu funkciju
        sessionViewModel.getStudentInvitations()
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Top Bar
        TopAppBar(
            title = { Text("Moji Sessioni") },
            navigationIcon = {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Natrag")
                }
            }
        )

        // Content
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
                        text = "Nema sessiona",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Čekaj pozivnice od instruktora",
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
                // POZIVNICE NA VRHU
                if (invitations.isNotEmpty()) {
                    item {
                        Text(
                            text = "Nove pozivnice (${invitations.size})",
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
                                    // Samo refresh podatke, ne navigiraj
                                    sessionViewModel.getAllStudentSessions()
                                    sessionViewModel.getStudentInvitations()
                                }
                            },
                            onDecline = {
                                // TODO: Implement decline functionality
                            }
                        )
                    }

                    item {
                        Divider(modifier = Modifier.padding(vertical = 16.dp))
                    }
                }

                // SVI SESSIONI (prihvaćeni i aktivni)
                item {
                    Text(
                        text = "Svi sessioni (${sessions.size})",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

                items(sessions) { session ->
                    StudentSessionCard(
                        session = session,
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
                    text = "Nova pozivnica",
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
                text = "Predmet: ${invitation.subject}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )

            Text(
                text = "Kreiran: ${Date(invitation.createdAt)}",
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
                    Text("Prihvati")
                }

                OutlinedButton(
                    onClick = onDecline,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Close, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Odbij")
                }
            }
        }
    }
}

@Composable
fun StudentSessionCard(
    session: InstructionSession,
    onJoinClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = when (session.status) {
                "active" -> MaterialTheme.colorScheme.surfaceVariant
                "pending" -> MaterialTheme.colorScheme.surface
                "completed" -> MaterialTheme.colorScheme.surfaceVariant
                else -> MaterialTheme.colorScheme.surface
            }
        )
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
                    text = "Predmet: ${session.subject}",
                    style = MaterialTheme.typography.titleMedium
                )

                Badge(
                    containerColor = when (session.status) {
                        "active" -> MaterialTheme.colorScheme.primary
                        "pending" -> MaterialTheme.colorScheme.secondary
                        "completed" -> MaterialTheme.colorScheme.tertiary
                        else -> MaterialTheme.colorScheme.outline
                    }
                ) {
                    Text(
                        text = session.status.uppercase(),
                        color = when (session.status) {
                            "active" -> MaterialTheme.colorScheme.onPrimary
                            "pending" -> MaterialTheme.colorScheme.onSecondary
                            "completed" -> MaterialTheme.colorScheme.onTertiary
                            else -> MaterialTheme.colorScheme.onSurface
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Kreiran: ${Date(session.createdAt)}",
                style = MaterialTheme.typography.bodySmall
            )

            if (session.startedAt != null) {
                Text(
                    text = "Počeo: ${Date(session.startedAt)}",
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = onJoinClick,
                modifier = Modifier.fillMaxWidth(),
                enabled = session.status == "active" || session.status == "completed"
            ) {
                Text(
                    when (session.status) {
                        "active" -> "Pridruži se sessionu"
                        "pending" -> "Čeka prihvaćanje"
                        "completed" -> "Pregledaj session"
                        else -> "Otvori session"
                    }
                )
            }
        }
    }
}

