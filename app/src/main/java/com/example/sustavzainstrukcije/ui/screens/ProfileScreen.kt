package com.example.sustavzainstrukcije.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
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
import com.example.sustavzainstrukcije.ui.utils.AvailableHoursInput
import com.example.sustavzainstrukcije.ui.utils.SubjectSelector
import com.example.sustavzainstrukcije.ui.viewmodels.AuthViewModel
import com.example.sustavzainstrukcije.ui.viewmodels.SubjectsViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    navController: NavHostController,
    authViewModel: AuthViewModel = viewModel(),
    subjectsViewModel: SubjectsViewModel = viewModel()
) {
    val currentUser by authViewModel.userData.collectAsState()
    val globalSubjects by subjectsViewModel.subjects.collectAsState()

    var name by remember { mutableStateOf("") }
    var subjects by remember { mutableStateOf(listOf<String>()) }
    var availableHours by remember { mutableStateOf<Map<String, List<String>>>(emptyMap()) }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    fun notify(msg: String) = scope.launch { snackbarHostState.showSnackbar(msg) }

    LaunchedEffect(currentUser) {
        authViewModel.fetchCurrentUserData()
        currentUser?.let { user ->
            name = user.name
            subjects = user.subjects
            availableHours = user.availableHours
        }
    }

    if (currentUser == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Profile", style = MaterialTheme.typography.headlineSmall) },
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
                .padding(20.dp)
                .padding(padding),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Email: ${currentUser?.email ?: "N/A"}",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(vertical = 8.dp)
            )

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Name") },
                modifier = Modifier.fillMaxWidth()
            )

            if (currentUser?.role == "instructor") {
                Text(text = "Subjects", style = MaterialTheme.typography.bodyLarge)

                // 1) postojeća selekcija iz globalne liste
                SubjectSelector(
                    subjects = globalSubjects,                 // umjesto INSTRUCTOR_SUBJECTS [7]
                    selectedSubjects = subjects,
                    onSubjectSelected = { s ->
                        if (s !in subjects) subjects = subjects + s
                    },
                    onSubjectDeselected = { s ->
                        subjects = subjects - s
                    }
                )

                // 2) dodavanje novog subjecta koji ne postoji u globalnoj listi
                var newSubject by remember { mutableStateOf("") }
                OutlinedTextField(
                    value = newSubject,
                    onValueChange = { newSubject = it },
                    label = { Text("Add new subject") },
                    modifier = Modifier.fillMaxWidth()
                )
                Button(onClick = {
                    val candidate = newSubject.trim()
                    if (candidate.isEmpty()) {
                        notify("Subject cannot be empty")
                        return@Button
                    }
                    val exists = globalSubjects.any { it.equals(candidate, ignoreCase = true) }
                    if (exists) {
                        notify("Subject already exists")
                        return@Button
                    }
                    scope.launch {
                        subjectsViewModel.addSubjectIfMissing(candidate)  // upiši u "subjects" kolekciju [17]
                        subjects = subjects + candidate                   // lokalni prikaz
                        newSubject = ""
                        notify("Subject added")
                    }
                }) { Text("Add") }

                // 3) dostupni sati (tvoje postojeće rješenje)
                AvailableHoursInput(
                    availableHours = availableHours,
                    onHoursUpdated = { updated -> availableHours = updated },
                    onNotify = ::notify
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    scope.launch {
                        val original = currentUser!!.subjects
                        val toAdd = subjects.filter { it !in original }.toTypedArray()
                        val toRemove = original.filter { it !in subjects }.toTypedArray()

                        val userId = currentUser!!.id
                        if (toAdd.isNotEmpty()) {
                            subjectsViewModel.addSubjectsToUser(userId, *toAdd)  // arrayUnion [16]
                        }
                        if (toRemove.isNotEmpty()) {
                            subjectsViewModel.removeSubjectsFromUser(userId, *toRemove) // arrayRemove [16]
                        }

                        // ostale podatke (ime/sati) ažurira tvoj ViewModel
                        authViewModel.updateInstructorProfile(name, subjects, availableHours)
                        notify("Profile updated")
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Save Changes") }
        }
    }
}
