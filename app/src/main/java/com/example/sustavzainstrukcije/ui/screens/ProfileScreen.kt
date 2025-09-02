package com.example.sustavzainstrukcije.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import com.example.sustavzainstrukcije.ui.utils.AvailableHoursInput
import com.example.sustavzainstrukcije.ui.utils.RatingBar
import com.example.sustavzainstrukcije.ui.utils.SubjectSelector
import com.example.sustavzainstrukcije.ui.viewmodels.AuthViewModel
import com.example.sustavzainstrukcije.ui.viewmodels.InstructorsViewModel
import com.example.sustavzainstrukcije.ui.viewmodels.SubjectsViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    navController: NavHostController,
    authViewModel: AuthViewModel = viewModel(),
    subjectsViewModel: SubjectsViewModel = viewModel(),
    instructorsViewModel: InstructorsViewModel = viewModel()
) {
    val currentUser by authViewModel.userData.collectAsState()
    val globalSubjects by subjectsViewModel.subjects.collectAsState()
    val commentsMap by instructorsViewModel.commentsByInstructorAndSubject.collectAsState()

    var name by remember { mutableStateOf("") }
    var subjects by remember { mutableStateOf(listOf<String>()) }
    var availableHours by remember { mutableStateOf<Map<String, List<String>>>(emptyMap()) }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    fun notify(msg: String) = scope.launch { snackbarHostState.showSnackbar(msg) }

    // image picker
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { authViewModel.updateProfilePicture(it) }
    }

    // dialog states
    var showAddSubjectDialog by remember { mutableStateOf(false) }
    var showCommentsDialog by remember { mutableStateOf(false) }
    var selectedSubject by remember { mutableStateOf<String?>(null) }
    var showImageDialog by remember { mutableStateOf(false) }

    LaunchedEffect(currentUser) {
        authViewModel.fetchCurrentUserData()
        currentUser?.let { user ->
            name = user.name
            subjects = user.subjects
            availableHours = user.availableHours
        }
    }

    LaunchedEffect(currentUser?.id) {
        if (currentUser?.role == "instructor") {
            instructorsViewModel.listenToAllInstructorRatings()
            instructorsViewModel.listenToAllInstructorComments(currentUser!!.id)
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp)
                .padding(padding),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // profile card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(110.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (!currentUser?.profilePictureUrl.isNullOrEmpty()) {
                                AsyncImage(
                                    model = currentUser?.profilePictureUrl,
                                    contentDescription = "Profile Picture",
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(CircleShape)
                                        .clickable { showImageDialog = true },
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                val initial = currentUser?.name?.firstOrNull()?.uppercase() ?: "?"
                                Text(
                                    text = initial,
                                    style = MaterialTheme.typography.headlineLarge.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                )
                            }
                        }

                        OutlinedButton(onClick = { launcher.launch("image/*") }) {
                            Text("Change Profile Picture")
                        }

                        Text(
                            text = "Email: ${currentUser?.email ?: "N/A"}",
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium)
                        )
                    }
                }
            }

            // name
            item {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp)),
                    singleLine = true
                )
            }

            // subjects + available hours
            if (currentUser?.role == "instructor") {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text("Subjects", style = MaterialTheme.typography.titleMedium)

                            SubjectSelector(
                                subjects = globalSubjects,
                                selectedSubjects = subjects,
                                onSubjectSelected = { s -> if (s !in subjects) subjects = subjects + s },
                                onSubjectDeselected = { s -> subjects = subjects - s }
                            )

                            Button(
                                onClick = { showAddSubjectDialog = true },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Add New Subject")
                            }
                        }
                    }
                }

                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Available Hours", style = MaterialTheme.typography.titleMedium)
                            Spacer(Modifier.height(8.dp))
                            AvailableHoursInput(
                                availableHours = availableHours,
                                onHoursUpdated = { updated -> availableHours = updated },
                                onNotify = ::notify
                            )
                        }
                    }
                }
            }

            // save changes button
            item {
                Button(
                    onClick = {
                        scope.launch {
                            val original = currentUser?.subjects ?: emptyList()
                            val toAdd = subjects.filter { it !in original }.toTypedArray()
                            val toRemove = original.filter { it !in subjects }.toTypedArray()

                            val userId = currentUser?.id ?: return@launch
                            if (toAdd.isNotEmpty()) {
                                subjectsViewModel.addSubjectsToUser(userId, *toAdd)
                            }
                            if (toRemove.isNotEmpty()) {
                                subjectsViewModel.removeSubjectsFromUser(userId, *toRemove)
                            }

                            authViewModel.updateInstructorProfile(name, subjects, availableHours)
                            notify("Profile updated")
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("Save Changes", style = MaterialTheme.typography.bodyLarge)
                }
            }

            // comments & ratings
            currentUser?.let { user ->
                if (user.role == "instructor") {
                    item {
                        OutlinedButton(
                            onClick = { showCommentsDialog = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("View Comments & Ratings")
                        }
                    }
                }
            }
        }
    }

    // dialogs
    if (showAddSubjectDialog) {
        var newSubject by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddSubjectDialog = false },
            title = { Text("Add New Subject") },
            text = {
                OutlinedTextField(
                    value = newSubject,
                    onValueChange = { newSubject = it },
                    label = { Text("Subject name") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val candidate = newSubject.trim()
                    if (candidate.isEmpty()) {
                        notify("Subject cannot be empty")
                    } else if (globalSubjects.any { it.equals(candidate, ignoreCase = true) }) {
                        notify("Subject already exists")
                    } else {
                        scope.launch {
                            subjectsViewModel.addSubjectIfMissing(candidate)
                            subjects = subjects + candidate
                            notify("Subject added")
                        }
                    }
                    showAddSubjectDialog = false
                }) { Text("Add") }
            },
            dismissButton = {
                TextButton(onClick = { showAddSubjectDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showCommentsDialog) {
        val allComments = commentsMap.filterKeys { it.first == currentUser!!.id }.flatMap { it.value }
        AlertDialog(
            onDismissRequest = { showCommentsDialog = false },
            title = { Text("Comments & Ratings") },
            text = {
                Column {
                    // filter
                    var expanded by remember { mutableStateOf(false) }
                    Box {
                        OutlinedButton(onClick = { expanded = true }) {
                            Text(selectedSubject ?: "All Subjects")
                        }
                        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                            DropdownMenuItem(
                                text = { Text("All Subjects") },
                                onClick = {
                                    selectedSubject = null
                                    expanded = false
                                }
                            )
                            subjects.forEach { s ->
                                DropdownMenuItem(
                                    text = { Text(s) },
                                    onClick = {
                                        selectedSubject = s
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(12.dp))

                    val filtered = if (selectedSubject == null) {
                        allComments
                    } else {
                        allComments.filter { it.subject == selectedSubject }
                    }

                    if (filtered.isNotEmpty()) {
                        val avg = filtered.map { it.rating }.average()
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(bottom = 12.dp)
                        ) {
                            Text(
                                text = "Average rating: %.1f".format(avg),
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium)
                            )
                            RatingBar(rating = avg.toFloat())
                        }
                    } else {
                        Text(
                            "No ratings available",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                    }

                    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(filtered) { c ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(c.studentName ?: "Anon", style = MaterialTheme.typography.labelLarge)
                                    Text("Subject: ${c.subject}", style = MaterialTheme.typography.labelSmall)
                                    Spacer(Modifier.height(4.dp))
                                    Text(c.text, style = MaterialTheme.typography.bodyMedium)
                                    Spacer(Modifier.height(6.dp))
                                    RatingBar(rating = c.rating.toFloat())
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showCommentsDialog = false }) { Text("Close") }
            }
        )
    }


    if (showImageDialog) {
        Dialog(onDismissRequest = { showImageDialog = false }) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.onBackground.copy(alpha = 0.95f)),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = currentUser?.profilePictureUrl,
                    contentDescription = "Full Image",
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.Fit
                )
            }
        }
    }
}
