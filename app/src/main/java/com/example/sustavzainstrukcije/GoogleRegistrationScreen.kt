package com.example.sustavzainstrukcije

import android.content.ContentValues.TAG
import android.util.Log
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.sustavzainstrukcije.ui.theme.SustavZaInstrukcijeTheme
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Calendar
import java.util.Locale

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

        // If instructor, show subject and available hours inputs
        //if (role == "instructor") {
            // Subject input
            // Available hours input
        //}

        //Button(onClick = {
            // Save user data to Firestore
            //saveUserData(role, subjects, availableHours)
            //onRegistrationComplete()
        //}) {
            //Text("Complete Registration")
        //}
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RoleSelector(
    selectedRole: String,
    onRoleSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val roles = listOf("student", "instructor")

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        OutlinedTextField(
            value = selectedRole.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() },
            onValueChange = {},
            readOnly = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor()
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            roles.forEach { role ->
                DropdownMenuItem(
                    text = { Text(role.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }) },
                    onClick = {
                        onRoleSelected(role)
                        expanded = false
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun SubjectsInput(
    subjects: List<String>,
    availableSubjects: List<String>,
    onSubjectAdded: (String) -> Unit,
    onSubjectRemoved: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var selectedSubject by remember { mutableStateOf("") }

    val filteredAvailableSubjects = availableSubjects.filter { it !in subjects }.sorted()

    Column {
        Text("Subjects you teach:", style = MaterialTheme.typography.labelMedium)

        Row(
            modifier = Modifier
                .horizontalScroll(rememberScrollState())
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            subjects.forEach { subject ->
                InputChip(
                    selected = false,
                    onClick = { },
                    label = { Text(subject) },
                    trailingIcon = {
                        IconButton(onClick = { onSubjectRemoved(subject) }) {
                            Icon(Icons.Default.Close, contentDescription = "Remove")
                        }
                    }
                )
            }
        }


        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded }
        ) {
            OutlinedTextField(
                value = selectedSubject,
                onValueChange = {},
                readOnly = true,
                label = { Text("Select Subject") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier
                    .menuAnchor()
                    .fillMaxWidth()
            )

            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                filteredAvailableSubjects.forEach { subject ->
                    DropdownMenuItem(
                        text = { Text(subject) },
                        onClick = {
                            onSubjectAdded(subject)
                            selectedSubject = subject
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DaySelector(
    selectedDay: String,
    onDaySelected: (String) -> Unit
) {
    val days = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        OutlinedTextField(
            value = selectedDay,
            onValueChange = {},
            readOnly = true,
            label = { Text("Select Day") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth()
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            days.forEach { day ->
                DropdownMenuItem(
                    text = { Text(day) },
                    onClick = {
                        onDaySelected(day)
                        expanded = false
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AvailableHoursInput(
    availableHours: Map<String, List<String>>,
    onHoursUpdated: (Map<String, List<String>>) -> Unit,
) {
    var showDialog by remember { mutableStateOf(false) }
    var selectedDay by remember { mutableStateOf("") }
    var startTime by remember { mutableStateOf("") }
    var endTime by remember { mutableStateOf("") }
    val orderedDays = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")

    Column {
        Text("Available Hours:", style = MaterialTheme.typography.labelMedium)

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column {
                Button(onClick = { showDialog = true }) {
                    Text("Add Hours")
                }
            }

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .height(100.dp)
            ) {
                items(orderedDays) { day ->
                    Text("$day:", modifier = Modifier.padding(4.dp))

                    availableHours[day]?.forEach { timeSlot ->
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            InputChip(
                                selected = false,
                                onClick = {},
                                label = { Text(timeSlot) },
                                trailingIcon = {
                                    IconButton(onClick = {
                                        val updated = availableHours.toMutableMap()
                                        val currentSlots = updated[day]?.toMutableList() ?: mutableListOf()
                                        currentSlots.remove(timeSlot)
                                        updated[day] = currentSlots
                                        onHoursUpdated(updated)
                                    }) {
                                        Icon(Icons.Default.Close, contentDescription = "Remove")
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Add Available Hours") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    DaySelector(
                        selectedDay = selectedDay,
                        onDaySelected = { selectedDay = it }
                    )
                    TimePicker(
                        label = "Start Time",
                        selectedTime = startTime,
                        onTimeSelected = { startTime = it }
                    )
                    TimePicker(
                        label = "End Time",
                        selectedTime = endTime,
                        onTimeSelected = { endTime = it }
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (selectedDay.isNotBlank() && startTime.isNotBlank() && endTime.isNotBlank()) {
                        val timeSlot = "$startTime - $endTime"
                        val updated = availableHours.toMutableMap()
                        val currentSlots = updated[selectedDay] ?: emptyList()
                        val newSlots = (currentSlots + timeSlot).sortedBy { it.split(" - ")[0] }
                        updated[selectedDay] = newSlots
                        onHoursUpdated(updated)
                        showDialog = false
                    }
                }) {
                    Text("Add")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun TimePicker(
    label: String,
    selectedTime: String,
    onTimeSelected: (String) -> Unit
) {
    var showDialog by remember { mutableStateOf(false) }

    OutlinedTextField(
        value = selectedTime,
        onValueChange = {},
        label = { Text(label) },
        trailingIcon = {
            IconButton(onClick = { showDialog = true }) {
                Icon(Icons.Default.Schedule, contentDescription = "Select time")
            }
        },
        readOnly = true,
        modifier = Modifier.fillMaxWidth()
    )

    if (showDialog) {
        TimePickerDialog(
            onDismissRequest = { showDialog = false },
            onTimeSelected = { hour, minute ->
                val time = String.format("%02d:%02d", hour, minute)
                onTimeSelected(time)
                showDialog = false
            }
        )
    }
}

@Composable
private fun TimePickerDialog(
    onDismissRequest: () -> Unit,
    onTimeSelected: (Int, Int) -> Unit
) {
    val calendar = Calendar.getInstance()
    var selectedHour by remember { mutableIntStateOf(calendar.get(Calendar.HOUR_OF_DAY)) }
    var selectedMinute by remember { mutableIntStateOf(calendar.get(Calendar.MINUTE)) }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text("Select Time") },
        text = {
            Column {
                NumberPicker(
                    value = selectedHour,
                    onValueChange = { selectedHour = it },
                    range = 0..23,
                    label = "Hour"
                )
                NumberPicker(
                    value = selectedMinute,
                    onValueChange = { selectedMinute = it },
                    range = 0..59,
                    label = "Minute"
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onTimeSelected(selectedHour, selectedMinute) }) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun NumberPicker(
    value: Int,
    onValueChange: (Int) -> Unit,
    range: IntRange,
    label: String
) {
    var sliderPosition by remember { mutableFloatStateOf(value.toFloat()) }

    Column {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Slider(
                value = sliderPosition,
                onValueChange = {
                    sliderPosition = it
                    onValueChange(it.toInt())
                },
                valueRange = range.first.toFloat()..range.last.toFloat(),
                steps = range.last - range.first,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = sliderPosition.toInt().toString().padStart(2, '0'),
                modifier = Modifier.padding(start = 16.dp),
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}

fun saveUserData(role: String?, subjects: List<String>, availableHours: Map<String, List<String>>) {
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

@Preview(showBackground = true)
@Composable
fun GoogleRegistrationScreen() {
    SustavZaInstrukcijeTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            GoogleRegistrationScreen(onRegistrationComplete = {})
        }
    }
}
