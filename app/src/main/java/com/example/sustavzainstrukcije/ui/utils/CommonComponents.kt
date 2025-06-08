package com.example.sustavzainstrukcije.ui.utils

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.sustavzainstrukcije.ui.data.User
import java.util.Locale
import androidx.compose.material3.TimePicker

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoleSelector(
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
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, true)
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubjectsInput(
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
                    .fillMaxWidth()
                    .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, true)
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

@Composable
fun SubjectSelector(
    subjects: List<String>,
    selectedSubjects: List<String>,
    onSubjectSelected: (String) -> Unit,
    onSubjectDeselected: (String) -> Unit
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        items(subjects) { subject ->
            val isSelected = selectedSubjects.contains(subject)
            AssistChip(
                onClick = {
                    if (isSelected) {
                        onSubjectDeselected(subject)
                    } else {
                        onSubjectSelected(subject)
                    }
                },
                label = { Text(subject) },
                colors = AssistChipDefaults.assistChipColors(
                    containerColor =
                        if (isSelected) MaterialTheme.colorScheme.primary else
                            MaterialTheme.colorScheme.surface
                )
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DaySelector(
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
                .fillMaxWidth()
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, true)
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

@Composable
fun AvailableHoursInput(
    availableHours: Map<String, List<String>>,
    onHoursUpdated: (Map<String, List<String>>) -> Unit,
) {
    var showAddHoursDialog by remember { mutableStateOf(false) }
    var showViewHoursDialog by remember { mutableStateOf(false) }
    var selectedDay by remember { mutableStateOf("") }
    var startTime by remember { mutableStateOf("") }
    var endTime by remember { mutableStateOf("") }
    val orderedDays =
        listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")

    val sortedAvailableHours = orderedDays.associateWith { day ->
        availableHours[day] ?: emptyList()
    }

    Column {
        Text("Available Hours:", style = MaterialTheme.typography.labelMedium)

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(onClick = { showAddHoursDialog = true }) {
                Text("Add Hours")
            }

            Button(onClick = { showViewHoursDialog = true }) {
                Text("Show Selected Hours")
            }

            if (showViewHoursDialog) {
                AlertDialog(
                    onDismissRequest = { showViewHoursDialog = false },
                    title = { Text("Selected Hours") },
                    text = {
                        LazyColumn(modifier = Modifier.heightIn(max = 400.dp)) {
                            sortedAvailableHours.forEach { (day, slots) ->
                                if (slots.isNotEmpty()) {
                                    item {
                                        Text(
                                            text = day,
                                            style = MaterialTheme.typography.titleMedium,
                                            modifier = Modifier.padding(vertical = 8.dp)
                                        )
                                    }
                                    items(slots) { slot ->
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text(
                                                text = slot,
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .padding(vertical = 4.dp)
                                            )
                                            IconButton(
                                                onClick = {
                                                    val updated = availableHours.toMutableMap()
                                                    val currentSlots =
                                                        updated[day]?.toMutableList()
                                                            ?: mutableListOf()
                                                    currentSlots.remove(slot)
                                                    updated[day] = currentSlots
                                                    onHoursUpdated(updated)
                                                }
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Close,
                                                    contentDescription = "Remove"
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { showViewHoursDialog = false }) {
                            Text("Close")
                        }
                    }
                )
            }
        }
    }

    if (showAddHoursDialog) {
        AlertDialog(
            onDismissRequest = { showAddHoursDialog = false },
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
                        val updatedAvailableHours =
                            availableHours.toMutableMap()
                        val currentSlots =
                            updatedAvailableHours[selectedDay] ?: emptyList()
                        val newSlots =
                            (currentSlots + timeSlot).sortedBy { it.split(" - ")[0] }
                        updatedAvailableHours[selectedDay] =
                            newSlots
                        onHoursUpdated(updatedAvailableHours)
                        showAddHoursDialog = false
                    }
                }) {
                    Text("Add")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showAddHoursDialog = false
                    selectedDay = ""
                    startTime = ""
                    endTime = ""
                }) {
                    Text("Cancel")
                }
            }
        )
    }
}


@Composable
fun TimePicker(
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
                val time = String.format(Locale.getDefault(), "%02d:%02d", hour, minute)
                onTimeSelected(time)
                showDialog = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimePickerDialog(
    onDismissRequest: () -> Unit,
    onTimeSelected: (Int, Int) -> Unit
) {
    val state = rememberTimePickerState(
        initialHour = 0,
        initialMinute = 0,
        is24Hour = true
    )

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text("Select Time") },
        text = {
            TimePicker(state = state)
        },
        confirmButton = {
            TextButton(onClick = {
                onTimeSelected(state.hour, state.minute)
            }) {
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
fun InstructorsHorizontalRow(
    title: String,
    instructors: List<User>,
    modifier: Modifier = Modifier,
    onCheckProfile: (intructorId: String) -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge
        )
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            /**
             * Zašto je dobro koristiti items:
             * items - works within LazyRow/LazyColumn
             * More efficient for large lists
             * Lazy - only renders visible items
             * Efficient -> only updates changed items
             */
            items(instructors) { instructor ->
                InstructorHorizontalCard(
                    instructor = instructor,
                    onCheckProfile = { onCheckProfile(instructor.id) })
            }
        }
    }
}

@Composable
fun InstructorHorizontalCard(
    instructor: User,
    onCheckProfile: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(280.dp)
            .padding(top = 8.dp)
            .padding(end = 8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            Text(
                text = instructor.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Teaches: ${instructor.subjects.sorted().joinToString(", ")}",
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = onCheckProfile,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Text("Check Profile")
            }
        }
    }
}

@Composable
fun SubjectDropdown(
    selectedSubject: String?,
    onSubjectSelected: (String?) -> Unit,
    availableSubjects: List<String>
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        OutlinedButton(onClick = { expanded = true }) {
            Text(selectedSubject ?: "Select Subject")
            Icon(Icons.Default.ArrowDropDown, contentDescription = "Dropdown Arrow")
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            DropdownMenuItem(
                text = { Text("All Subjects") },
                onClick = {
                    onSubjectSelected(null)
                    expanded = false
                }
            )

            availableSubjects.forEach { subject ->
                DropdownMenuItem(
                    text = { Text(subject) },
                    onClick = {
                        onSubjectSelected(subject)
                        expanded = false
                    }
                )
            }
        }
    }
}
