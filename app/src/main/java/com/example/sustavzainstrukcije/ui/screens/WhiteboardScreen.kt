package com.example.sustavzainstrukcije.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
    val allPages by whiteboardViewModel.allPages.collectAsState()
    val currentPageIndex by whiteboardViewModel.currentPageIndex.collectAsState()

    var isLoading by remember { mutableStateOf(true) }
    var currentPath by remember { mutableStateOf(Path()) }
    var currentPoints by remember { mutableStateOf(listOf<Point>()) }
    var selectedColor by remember { mutableStateOf(Color.Black) }
    var strokeWidth by remember { mutableStateOf(5f) }
    var showColorPicker by remember { mutableStateOf(false) }

    LaunchedEffect(sessionId) {
        whiteboardViewModel.initializeWhiteboard(sessionId)
    }

    LaunchedEffect(allPages) {
        if (allPages.isNotEmpty()) {
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
        // Top Bar s page navigation
        TopAppBar(
            title = {
                Text("Whiteboard - Stranica ${currentPage?.pageNumber ?: 1} od ${allPages.size}")
            },
            navigationIcon = {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Natrag")
                }
            }
        )

        // Page Navigation Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Previous Page Button
            IconButton(
                onClick = { whiteboardViewModel.navigateToPreviousPage() },
                enabled = currentPageIndex > 0
            ) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Prethodna stranica")
            }

            // Page Indicators
            LazyRow(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(allPages.size) { index ->
                    PageIndicator(
                        pageNumber = allPages[index].pageNumber,
                        isSelected = index == currentPageIndex,
                        onClick = { whiteboardViewModel.setCurrentPage(index) }
                    )
                }

                // Add New Page Button (samo na zadnjoj stranici)
                if (allPages.isNotEmpty() && currentPageIndex == allPages.size - 1) {
                    item {
                        IconButton(
                            onClick = { whiteboardViewModel.createNewPage() },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = "Nova stranica",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            // Next Page Button
            IconButton(
                onClick = { whiteboardViewModel.navigateToNextPage() },
                enabled = currentPageIndex < allPages.size - 1
            ) {
                Icon(Icons.Default.ArrowForward, contentDescription = "Sljedeća stranica")
            }
        }

        // Drawing Tools (postojeći kod)
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

            // Stroke Width Slider
            Text("Debljina:")
            Slider(
                value = strokeWidth,
                onValueChange = { strokeWidth = it },
                valueRange = 1f..20f,
                modifier = Modifier.weight(1f)
            )
            Text("${strokeWidth.toInt()}px")
        }

        // Canvas (postojeći kod ostaje isti)
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
                        onDrag = { change, _ ->
                            val newPoint = Point(change.position.x, change.position.y)
                            currentPoints = currentPoints + newPoint
                            currentPath.lineTo(change.position.x, change.position.y)
                        },
                        onDragEnd = {
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

    // Color Picker Dialog (postojeći kod)
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
fun PageIndicator(
    pageNumber: Int,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .size(32.dp)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected)
                MaterialTheme.colorScheme.primary
            else
                MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isSelected) 4.dp else 2.dp
        )
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = pageNumber.toString(),
                style = MaterialTheme.typography.bodySmall,
                color = if (isSelected)
                    MaterialTheme.colorScheme.onPrimary
                else
                    MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun ColorPickerDialog(
    onColorSelected: (Color) -> Unit,
    onDismiss: () -> Unit
) {
    val colors = listOf(
        Color.Black, Color.Red, Color.Green, Color.Blue,
        Color.Yellow, Color.Magenta, Color.Cyan, Color.Gray,
        Color(0xFF8B4513),
        Color(0xFFFFA500),
        Color(0xFF800080),
        Color(0xFFFFB6C1)
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


