package com.example.sustavzainstrukcije.ui.screens

import android.content.ContentValues.TAG
import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.sustavzainstrukcije.ui.utils.AvailableHoursInput
import com.example.sustavzainstrukcije.ui.utils.RoleSelector
import com.example.sustavzainstrukcije.ui.utils.SubjectsInput
import com.example.sustavzainstrukcije.ui.viewmodels.SubjectsViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch

@Composable
fun GoogleRegistrationScreen(
    onRegistrationComplete: () -> Unit,
    modifier: Modifier = Modifier,
    subjectsViewModel: SubjectsViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    var role by rememberSaveable { mutableStateOf("instructor") }
    var subjects by rememberSaveable { mutableStateOf(emptyList<String>()) }
    var availableTime by rememberSaveable { mutableStateOf(emptyMap<String, List<String>>())}
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val subjectsFromDb by subjectsViewModel.subjects.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    fun notify(msg: String) { scope.launch { snackbarHostState.showSnackbar(msg) } }


    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
    ) { padding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Create Account",
                style = MaterialTheme.typography.headlineMedium
            )

            RoleSelector(
                selectedRole = role,
                onRoleSelected = { role = it }
            )

            if (role == "instructor") {
                SubjectsInput(
                    subjects = subjects,
                    availableSubjects = subjectsFromDb,
                    onSubjectAdded = { subjects = subjects + it},
                    onSubjectRemoved = { subjects = subjects - it},
                    onAddNewSubject = { candidate ->
                        scope.launch {
                            subjectsViewModel.addSubjectIfMissing(candidate)
                        }},
                    onNotify = ::notify
                )

                AvailableHoursInput(
                    availableHours = availableTime,
                    onHoursUpdated = { availableTime = it },
                    onNotify = { msg -> scope.launch { snackbarHostState.showSnackbar(msg) } }
                )
            }

            errorMessage?.let {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            Button(
                onClick = {
                    val valid = validateForm(role, subjects, availableTime)
                    if (!valid) {
                        when {
                            role == "instructor" && subjects.isEmpty() -> notify("Choose at least one subject")
                            role == "instructor" && availableTime.isEmpty() -> notify("Add at least one available time")
                            else -> notify("Please complete all required fields.")
                        }
                        return@Button
                    }


                    saveUserData(
                        role = role,
                        subjects = subjects,
                        availableHours = availableTime,
                        onSuccess = {
                            notify("Registration successful")
                            onRegistrationComplete()
                        },
                        onFailure = { raw ->
                            val msg = raw.ifBlank { "Registration failed" }
                            notify(msg)
                        }
                    )

                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Register")
            }

        }
    }

}

private fun saveUserData(
    role: String?,
    subjects: List<String>,
    availableHours: Map<String, List<String>>,
    onSuccess: () -> Unit,
    onFailure: (String) -> Unit
) {
    val user = FirebaseAuth.getInstance().currentUser
    if (user != null) {

        val userData = hashMapOf<String, Any>().apply {
            put("name", user.displayName ?: "")
            put("email", user.email ?: "")
            put("role", role ?: "student")
        }

        if (role == "instructor") {
            userData["subjects"] = ArrayList(subjects)

            val hoursMap = HashMap<String, ArrayList<String>>()
            availableHours.forEach { (key, value) ->
                hoursMap[key] = ArrayList(value)
            }
            userData["availableHours"] = hoursMap
        }

        FirebaseFirestore.getInstance().collection("users")
            .document(user.uid)
            .set(userData)
            .addOnSuccessListener {
                Log.d(TAG, "User data saved successfully")
                onSuccess()
            }
            .addOnFailureListener { e ->
                Log.w(TAG, "Error saving user data", e)
                onFailure("Error saving user data: ${e.localizedMessage}")
            }
    } else {
        onFailure("No signed-in user.")
    }
}


private fun validateForm(
    role: String,
    subjects: List<String>,
    availableHours: Map<String, List<String>>
): Boolean {
    return when {
        role == "instructor" && subjects.isEmpty() -> false
        role == "instructor" && availableHours.isEmpty() -> false
        else -> true
    }
}
