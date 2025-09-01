package com.example.sustavzainstrukcije.ui.utils

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Star
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import java.time.LocalTime

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
    onSubjectRemoved: (String) -> Unit,
    onAddNewSubject: (String) -> Unit,
    onNotify: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var selectedSubject by remember { mutableStateOf("") }
    var newSubject by remember { mutableStateOf("") }

    val filteredAvailableSubjects = availableSubjects
        .filter { it !in subjects }
        .sorted()

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

        ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
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
            ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
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

        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = newSubject,
            onValueChange = { newSubject = it },
            label = { Text("Add new subject") },
            modifier = Modifier.fillMaxWidth()
        )
        Button(onClick = {
            val candidate = newSubject.trim()
            if (candidate.isEmpty()) {
                onNotify("Subject cannot be empty")
                return@Button
            }
            val existsInGlobal = availableSubjects.any { it.equals(candidate, ignoreCase = true) }
            val existsInLocal = subjects.any { it.equals(candidate, ignoreCase = true) }
            if (existsInGlobal || existsInLocal) {
                onNotify("Subject already exists")
                return@Button
            }
            onAddNewSubject(candidate)
            onSubjectAdded(candidate)
            newSubject = ""
            onNotify("Subject added")
        }) {
            Text("Add")
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
    onNotify: (String) -> Unit
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

                        if (!isValidRange(startTime, endTime)) {
                            onNotify("Start hour starts after end hour")
                            return@Button
                        }

                        val currentSlots = availableHours[selectedDay] ?: emptyList()
                        if (overlapsAny(currentSlots, startTime, endTime)) {
                            onNotify("Time slot overlaps with existing slots")
                            return@Button
                        }

                        val timeSlot = "$startTime - $endTime"
                        val updated = availableHours.toMutableMap()
                        val newSlots = (currentSlots + timeSlot)
                            .sortedBy { parseTimeHHmm(it.substringBefore(" - ")) }
                        updated[selectedDay] = newSlots
                        onHoursUpdated(updated)

                        showAddHoursDialog = false
                        selectedDay = ""
                        startTime = ""
                        endTime = ""

                        onNotify("Time slot added")
                    }
                }) { Text("Add") }
            }

            ,
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
fun InstructorHorizontalCard(
    instructor: User,
    modifier: Modifier = Modifier,
    avgRating: Double = 0.0,
    ratingCount: Int = 0,
    onCheckProfile: () -> Unit
) {
    Card(
        modifier = modifier
            .width(200.dp)
            .clickable { onCheckProfile() },
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        shape = MaterialTheme.shapes.large
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                val initial = instructor.name.firstOrNull()?.uppercase() ?: ""
                if (initial.isNotBlank()) {
                    Text(
                        text = initial,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Avatar",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Text(
                text = instructor.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )

            if (!instructor.subjects.isNullOrEmpty()) {
                Text(
                    text = instructor.subjects.joinToString(", "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                RatingBar(rating = avgRating.toFloat())
                Text(
                    text = "(${ratingCount})",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
        }
    }
}

@Composable
fun InstructorsHorizontalRow(
    title: String,
    instructors: List<User>,
    modifier: Modifier = Modifier,
    onCheckProfile: (instructorId: String) -> Unit,
    cardContent: @Composable (User) -> Unit
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
            items(instructors) { instructor ->
                cardContent(instructor)
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

private fun parseTimeHHmm(t: String): LocalTime =
    LocalTime.parse(t) // očekuje "HH:mm" format, npr. "08:45"

private fun isValidRange(start: String, end: String): Boolean {
    val s = parseTimeHHmm(start)
    val e = parseTimeHHmm(end)
    return s < e
}

private fun overlapsAny(
    daySlots: List<String>,
    newStartStr: String,
    newEndStr: String
): Boolean {
    val newStart = parseTimeHHmm(newStartStr)
    val newEnd = parseTimeHHmm(newEndStr)

    return daySlots.any { slot ->
        val sStr = slot.substringBefore(" - ")
        val eStr = slot.substringAfter(" - ")
        val s = parseTimeHHmm(sStr)
        val e = parseTimeHHmm(eStr)
        (newStart < e) && (newEnd > s)
    }
}

@Composable
fun RatingBar(
    rating: Float,
    maxRating: Int = 5,
    modifier: Modifier = Modifier,
    starSize: Dp = 25.dp,
    onRatingSelected: ((Int) -> Unit)? = null
) {
    Row(modifier = modifier) {
        for (i in 1..maxRating) {
            val filled = i <= rating
            Icon(
                imageVector = Icons.Default.Star,
                contentDescription = "Star $i",
                tint = if (filled) Color(0xFFFFD700) else Color.Gray,
                modifier = Modifier
                    .size(starSize)
                    .then(
                        if (onRatingSelected != null) {
                            Modifier.clickable { onRatingSelected(i) }
                        } else {
                            Modifier
                        }
                    )
            )
        }
    }
}


