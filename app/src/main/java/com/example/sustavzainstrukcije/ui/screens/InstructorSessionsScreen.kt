package com.example.sustavzainstrukcije.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.example.sustavzainstrukcije.ui.data.InstructionSession
import com.example.sustavzainstrukcije.ui.viewmodels.SessionViewModel
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InstructorSessionsScreen(
    navController: NavHostController,
    sessionViewModel: SessionViewModel = viewModel()
) {
    val sessions by sessionViewModel.sessions.collectAsState()

    LaunchedEffect(Unit) {
        sessionViewModel.getInstructorSessions()
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Top Bar
        TopAppBar(
            title = { Text("Moji Sessioni") },
            navigationIcon = {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Natrag")
                }
            },
            actions = {
                IconButton(onClick = { navController.navigate("create_session") }) {
                    Icon(Icons.Default.Add, contentDescription = "Kreiraj session")
                }
            }
        )

        // Content
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
                        text = "Nema kreiranih sessiona",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Button(
                        onClick = { navController.navigate("create_session") }
                    ) {
                        Text("Kreiraj prvi session")
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
                        text = "Ukupno sessiona: ${sessions.size}",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

                items(sessions) { session ->
                    SessionCard(
                        session = session,
                        onJoinClick = {
                            navController.navigate("whiteboard/${session.id}")
                        },
                        onContinueClick = {
                            navController.navigate("whiteboard/${session.id}")
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun SessionCard(
    session: InstructionSession,
    onJoinClick: () -> Unit,
    onContinueClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Predmet: ${session.subject}",
                style = MaterialTheme.typography.titleMedium
            )

            Text(
                text = "Status: ${session.status}",
                style = MaterialTheme.typography.bodyMedium,
                color = when (session.status) {
                    "active" -> MaterialTheme.colorScheme.primary
                    "pending" -> MaterialTheme.colorScheme.secondary
                    "completed" -> MaterialTheme.colorScheme.tertiary
                    else -> MaterialTheme.colorScheme.onSurface
                }
            )

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

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                when (session.status) {
                    "active" -> {
                        Button(
                            onClick = onContinueClick,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Nastavi session")
                        }
                    }
                    "pending" -> {
                        Button(
                            onClick = onJoinClick,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Pokreni session")
                        }
                    }
                    "completed" -> {
                        OutlinedButton(
                            onClick = onContinueClick,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Pregledaj session")
                        }
                    }
                }
            }
        }
    }
}
