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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.sustavzainstrukcije.ui.utils.AvailableHoursInput
import com.example.sustavzainstrukcije.ui.utils.RoleSelector
import com.example.sustavzainstrukcije.ui.utils.SubjectsInput
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

@Composable
fun GoogleRegistrationScreen(
    onRegistrationComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    var role by rememberSaveable { mutableStateOf("instructor") }
    var subjects by rememberSaveable { mutableStateOf(emptyList<String>()) }
    var availableTime by rememberSaveable { mutableStateOf(emptyMap<String, List<String>>())}
    val allSubjects = listOf("Mathematics", "Physics", "Chemistry", "Biology", "Computer Science", "Literature", "History", "Geography")
    var errorMessage by remember { mutableStateOf<String?>(null) }

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
                availableSubjects = allSubjects,
                onSubjectAdded = { newSubject ->
                    subjects = subjects + newSubject
                },
                onSubjectRemoved = { subjectToRemove ->
                    subjects = subjects.filter { it != subjectToRemove }
                }
            )

            AvailableHoursInput(
                availableHours = availableTime,
                onHoursUpdated = { availableTime = it }
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
                if (validateForm(role, subjects, availableTime)) {
                    saveUserData(
                        role = role,
                        subjects = subjects,
                        availableHours = availableTime,
                    )
                }
                onRegistrationComplete()
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Register")
        }
    }
}

private fun saveUserData(role: String?, subjects: List<String>, availableHours: Map<String, List<String>>) {
    val user = FirebaseAuth.getInstance().currentUser
    if (user != null) {

        val userData = hashMapOf<String, Any>().apply {
            put("name", user.displayName ?: "")
            put("email", user.email ?: "")
            put("role", role ?: "student")
        }


        if (role == "instructor") {
            // Convert List to Firestore-safe format
            userData["subjects"] = ArrayList(subjects)

            // Convert Map<String, List> to Firestore-safe format
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
            }
            .addOnFailureListener { e ->
                Log.w(TAG, "Error saving user data", e)
            }
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
