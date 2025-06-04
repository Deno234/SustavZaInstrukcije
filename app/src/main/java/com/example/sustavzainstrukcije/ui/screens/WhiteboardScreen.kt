package com.example.sustavzainstrukcije.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.example.sustavzainstrukcije.ui.data.Point
import com.example.sustavzainstrukcije.ui.viewmodels.WhiteboardViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WhiteboardScreen(
    sessionId: String,
    navController: NavHostController,
    whiteboardViewModel: WhiteboardViewModel = viewModel()
) {
    val strokes by whiteboardViewModel.strokes.collectAsState()
    val currentPage by whiteboardViewModel.currentPage.collectAsState()
    var isLoading by remember { mutableStateOf(true) }

    var currentPath by remember { mutableStateOf(Path()) }
    var currentPoints by remember { mutableStateOf(listOf<Point>()) }
    var selectedColor by remember { mutableStateOf(Color.Black) }
    var strokeWidth by remember { mutableStateOf(5f) }
    var showColorPicker by remember { mutableStateOf(false) }

    LaunchedEffect(sessionId) {
        whiteboardViewModel.initializeWhiteboard(sessionId)
    }

    LaunchedEffect(currentPage) {
        if (currentPage != null) {
            isLoading = false
        }
    }

    if (isLoading) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
        return
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Top Bar
        TopAppBar(
            title = {
                Text("Whiteboard - Stranica ${currentPage?.pageNumber ?: 1}")
            },
            navigationIcon = {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Natrag")
                }
            },
            actions = {
                IconButton(onClick = { whiteboardViewModel.createNewPage() }) {
                    Icon(Icons.Default.Add, contentDescription = "Nova stranica")
                }
            }
        )

        // Drawing Tools
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Color Picker Button
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(selectedColor)
                    .clickable { showColorPicker = true }
            )

            Text("Debljina:")
            Slider(
                value = strokeWidth,
                onValueChange = { strokeWidth = it },
                valueRange = 1f..20f,
                modifier = Modifier.weight(1f)
            )
            Text("${strokeWidth.toInt()}px")

            IconButton(onClick = { /* TODO: Implement clear */ }) {
                Icon(Icons.Default.Clear, contentDescription = "Očisti")
            }
        }

        // Canvas
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            currentPath = Path().apply { moveTo(offset.x, offset.y) }
                            currentPoints = listOf(Point(offset.x, offset.y))
                        },
                        onDrag = { change, dragAmount ->
                            val newPoint = Point(change.position.x, change.position.y)
                            currentPoints = currentPoints + newPoint
                            currentPath.lineTo(change.position.x, change.position.y)
                        },
                        onDragEnd = {
                            // Pošalji stroke na Firebase
                            if (currentPoints.isNotEmpty()) {
                                whiteboardViewModel.addStroke(
                                    points = currentPoints,
                                    color = String.format("#%06X", selectedColor.toArgb() and 0xFFFFFF),
                                    strokeWidth = strokeWidth
                                )
                            }
                            currentPoints = emptyList()
                            currentPath = Path()
                        }
                    )

                }
        ) {
            // Crtaj postojeće stroke-ove
            strokes.forEach { stroke ->
                if (stroke.points.isNotEmpty()) {
                    val path = Path()
                    path.moveTo(stroke.points.first().x, stroke.points.first().y)
                    stroke.points.drop(1).forEach { point ->
                        path.lineTo(point.x, point.y)
                    }

                    drawPath(
                        path = path,
                        color = Color(android.graphics.Color.parseColor(stroke.color)),
                        style = Stroke(
                            width = stroke.strokeWidth,
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round
                        )
                    )
                }
            }

            // Crtaj trenutni stroke
            if (currentPoints.isNotEmpty()) {
                drawPath(
                    path = currentPath,
                    color = selectedColor,
                    style = Stroke(
                        width = strokeWidth,
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )
            }
        }
    }

    // Color Picker Dialog
    if (showColorPicker) {
        ColorPickerDialog(
            onColorSelected = { color ->
                selectedColor = color
                showColorPicker = false
            },
            onDismiss = { showColorPicker = false }
        )
    }
}

@Composable
fun ColorPickerDialog(
    onColorSelected: (Color) -> Unit,
    onDismiss: () -> Unit
) {
    val colors = listOf(
        Color.Black, Color.Red, Color.Green, Color.Blue,
        Color.Yellow, Color.Magenta, Color.Cyan, Color.Gray
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Izaberi boju") },
        text = {
            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                modifier = Modifier.padding(16.dp)
            ) {
                items(colors.size) { index ->
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .padding(4.dp)
                            .clip(CircleShape)
                            .background(colors[index])
                            .clickable { onColorSelected(colors[index]) }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Odustani")
            }
        }
    )
}
