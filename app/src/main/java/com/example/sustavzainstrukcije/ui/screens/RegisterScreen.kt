package com.example.sustavzainstrukcije.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.sustavzainstrukcije.ui.theme.SustavZaInstrukcijeTheme
import com.example.sustavzainstrukcije.ui.utils.AvailableHoursInput
import com.example.sustavzainstrukcije.ui.utils.RoleSelector
import com.example.sustavzainstrukcije.ui.utils.SubjectsInput
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

@Composable
fun RegisterScreen(
    onRegistrationComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    var name by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var role by rememberSaveable { mutableStateOf("instructor") }
    var subjects by rememberSaveable { mutableStateOf(emptyList<String>()) }
    var availableTime by rememberSaveable { mutableStateOf(emptyMap<String, List<String>>())}
    var passwordVisible by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val allSubjects = listOf("Mathematics", "Physics", "Chemistry", "Biology", "Computer Science", "Literature", "History", "Geography")

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

        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Full Name") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            visualTransformation = if (passwordVisible)
            VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(
                        imageVector = if (passwordVisible)
                        Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        contentDescription = "Toggle password visibility"
                    )
                }
            },
            modifier = Modifier.fillMaxWidth()
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
                    registerUser(
                        name = name,
                        email = email,
                        password = password,
                        role = role,
                        subjects = subjects,
                        availableHours = availableTime,
                        onSuccess = onRegistrationComplete,
                        onError = { errorMessage = it }
                    )
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Register")
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

private fun registerUser(
    name: String,
    email: String,
    password: String,
    role: String,
    subjects: List<String>,
    availableHours: Map<String, List<String>>,
    onSuccess: () -> Unit,
    onError: (String) -> Unit
) {
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()

    auth.createUserWithEmailAndPassword(email, password)
        .addOnCompleteListener { authTask ->
            if (authTask.isSuccessful) {
                val userId = authTask.result?.user?.uid ?: return@addOnCompleteListener

                val userData = mutableMapOf<String, Any>(
                    "name" to name,
                    "email" to email,
                    "role" to role
                )

                if (role == "instructor") {
                    userData["subjects"] = subjects
                    userData["availableHours"] = availableHours
                }

                db.collection("users").document(userId)
                    .set(userData)
                    .addOnSuccessListener { onSuccess() }
                    .addOnFailureListener { e ->
                        onError("Failed to save user data: ${e.message}")
                    }
            } else {
                onError(authTask.exception?.message ?: "Registration failed")
            }
        }
}


@Preview(showBackground = true)
@Composable
fun RegisterScreenPreview() {
    SustavZaInstrukcijeTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            RegisterScreen(onRegistrationComplete = {})
        }
    }
}