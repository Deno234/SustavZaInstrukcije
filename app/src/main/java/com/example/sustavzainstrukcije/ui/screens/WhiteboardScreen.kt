package com.example.sustavzainstrukcije.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.material.icons.filled.Create
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.example.sustavzainstrukcije.R
import com.example.sustavzainstrukcije.ui.data.EraseMode
import com.example.sustavzainstrukcije.ui.data.Point
import com.example.sustavzainstrukcije.ui.viewmodels.WhiteboardViewModel
import androidx.core.graphics.toColorInt
import kotlin.math.sqrt

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

    val isEraser by whiteboardViewModel.isEraserActive.collectAsState()
    val eraserMode by whiteboardViewModel.eraseMode.collectAsState()
    val eraserWidth by whiteboardViewModel.eraserSize.collectAsState()

    val colorToUse = if (isEraser) Color.White else selectedColor

    var showEraserSizePopup by remember { mutableStateOf(false) }
    var localEraserWidth by remember { mutableStateOf(eraserWidth) }
    var showEraseModePopup by remember { mutableStateOf(false) }

    var showStrokeWidthPopup by remember { mutableStateOf(false) }

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

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
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

        // 2. Navigacija kroz stranice
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { whiteboardViewModel.navigateToPreviousPage() },
                enabled = currentPageIndex > 0
            ) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Prethodna stranica")
            }

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

            IconButton(
                onClick = { whiteboardViewModel.navigateToNextPage() },
                enabled = currentPageIndex < allPages.size - 1
            ) {
                Icon(Icons.Default.ArrowForward, contentDescription = "Sljedeća stranica")
            }
        }

        // 3. Alati za crtanje
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Pisanje
            if (!isEraser) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(selectedColor)
                        .clickable { showColorPicker = true }
                )
                IconButton(onClick = { showStrokeWidthPopup = true }) {
                    Icon(Icons.Default.Create, contentDescription = "Debljina olovke")
                }

                IconButton(
                    onClick = { whiteboardViewModel.toggleEraser() },
                    modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_eraser),
                        contentDescription = "Gumica",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            } else {
                // Gumica
                IconButton(onClick = { showEraseModePopup = true }) {
                    Icon(
                        painter = painterResource(R.drawable.ic_edit),
                        contentDescription = "Odaberi način gumice"
                    )
                }

                IconButton(onClick = { showEraserSizePopup = true }) {
                    Icon(
                        painter = painterResource(R.drawable.ic_tune),
                        contentDescription = "Postavi veličinu gumice"
                    )
                }

                IconButton(
                    onClick = { whiteboardViewModel.toggleEraser() },
                    modifier = Modifier.background(MaterialTheme.colorScheme.primaryContainer, CircleShape)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_eraser),
                        contentDescription = "Natrag na pisanje",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            IconButton(onClick = { whiteboardViewModel.undo() }) {
                Icon(
                    painter = painterResource(R.drawable.ic_notification),
                    contentDescription = "Undo"
                )
            }

            IconButton(onClick = { whiteboardViewModel.redo() }) {
                Icon(
                    painter = painterResource(R.drawable.nikola_tesla),
                    contentDescription = "Redo"
                )
            }

        }


        // 4. Canvas za crtanje
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(Color.White)
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .clipToBounds()
                    .background(Color.White)
                    .pointerInput(isEraser, eraserMode, strokes) {
                        detectTapGestures { offset ->
                            if (offset.x < 0 || offset.y < 0 || offset.x > size.width || offset.y > size.height) {
                                return@detectTapGestures
                            }
                            if (isEraser && eraserMode == EraseMode.STROKE) {
                                // Pronađi i obriši stroke na toj poziciji
                                val strokeToRemove = strokes.lastOrNull { stroke ->
                                    // Preskoči stroke-ove koji su bijeli (gumica bojom)
                                    if (stroke.color == "#FFFFFF") return@lastOrNull false
                                    stroke.points.any { point ->
                                        val dx = point.x - offset.x
                                        val dy = point.y - offset.y
                                        sqrt(dx * dx + dy * dy) < eraserWidth / 2
                                    }
                                }
                                strokeToRemove?.let { whiteboardViewModel.removeStroke(it.id) }
                            } else if (!isEraser) {
                                // Normalni tap za crtanje točke
                                val singlePoint = listOf(Point(offset.x, offset.y))
                                whiteboardViewModel.addStroke(
                                    points = singlePoint,
                                    color = String.format("#%06X", selectedColor.toArgb() and 0xFFFFFF),
                                    strokeWidth = strokeWidth
                                )
                            }
                        }
                    }
                    .pointerInput(isEraser, eraserMode) {
                        detectDragGestures(
                            onDragStart = { offset ->
                                if (offset.x < 0 || offset.y < 0 || offset.x > size.width || offset.y > size.height) {
                                    return@detectDragGestures
                                }
                                if (isEraser && eraserMode == EraseMode.STROKE) return@detectDragGestures // Preskoči za stroke gumicu
                                currentPath = Path().apply { moveTo(offset.x, offset.y) }
                                currentPoints = listOf(Point(offset.x, offset.y))
                            },
                            onDrag = { change, _ ->
                                val newPoint = Point(change.position.x, change.position.y)

                                when {
                                    isEraser && eraserMode == EraseMode.STROKE -> {
                                        val strokeToRemove = strokes.lastOrNull { stroke ->
                                            if (stroke.color == "#FFFFFF") return@lastOrNull false
                                            stroke.points.any { point ->
                                                val dx = point.x - newPoint.x
                                                val dy = point.y - newPoint.y
                                                sqrt(dx * dx + dy * dy) < stroke.strokeWidth * 2
                                            }
                                        }
                                        strokeToRemove?.let { whiteboardViewModel.removeStroke(it.id) }
                                    }

                                    isEraser && eraserMode == EraseMode.COLOR -> {
                                        currentPoints = currentPoints + newPoint
                                        currentPath.lineTo(newPoint.x, newPoint.y)
                                    }

                                    else -> {
                                        currentPoints = currentPoints + newPoint
                                        currentPath.lineTo(newPoint.x, newPoint.y)
                                    }
                                }
                            },

                            onDragEnd = {
                                if (currentPoints.isNotEmpty()) {
                                    if (!isEraser || eraserMode == EraseMode.COLOR) {
                                        val strokeColor = if (isEraser) {
                                            String.format("#%06X", Color.White.toArgb() and 0xFFFFFF)
                                        } else {
                                            String.format("#%06X", selectedColor.toArgb() and 0xFFFFFF)
                                        }

                                        val strokeWidthToUse = if (isEraser) {
                                            eraserWidth
                                        } else {
                                            strokeWidth
                                        }

                                        whiteboardViewModel.addStroke(
                                            points = currentPoints,
                                            color = strokeColor,
                                            strokeWidth = strokeWidthToUse
                                        )
                                    }
                                }

                                currentPoints = emptyList()
                                currentPath = Path()
                            }


                        )
                    }

            ) {
                strokes.forEach { stroke ->
                    if (stroke.points.isNotEmpty()) {
                        val color = Color(stroke.color.toColorInt())
                        if (stroke.points.size == 1) {
                            val point = stroke.points.first()
                            drawCircle(
                                color = color,
                                radius = stroke.strokeWidth / 2f,
                                center = Offset(point.x, point.y)
                            )
                        } else {
                            val path = Path()
                            path.moveTo(stroke.points.first().x, stroke.points.first().y)
                            stroke.points.drop(1).forEach { point ->
                                path.lineTo(point.x, point.y)
                            }
                            drawPath(
                                path = path,
                                color = color,
                                style = Stroke(
                                    width = stroke.strokeWidth,
                                    cap = StrokeCap.Round,
                                    join = StrokeJoin.Round
                                )
                            )
                        }
                    }
                }

                if (isEraser && currentPoints.isNotEmpty()) {
                    val lastPoint = currentPoints.last()

                    // Ispuna: blago bijela (transparentna)
                    drawCircle(
                        color = Color.White.copy(alpha = 0.6f),
                        radius = eraserWidth / 2f,
                        center = Offset(lastPoint.x, lastPoint.y)
                    )

                    // Obrub: svijetlo siva za kontrast
                    drawCircle(
                        color = Color.Gray,
                        radius = eraserWidth / 2f,
                        center = Offset(lastPoint.x, lastPoint.y),
                        style = Stroke(width = 2f)
                    )
                }

                if (currentPoints.isNotEmpty() && (!isEraser || eraserMode != EraseMode.STROKE)) {
                    drawPath(
                        path = currentPath,
                        color = colorToUse,
                        style = Stroke(
                            width = if (isEraser) eraserWidth else strokeWidth,
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round
                        )
                    )
                }

            }
        }

        // 5. Color picker dijalog
        if (showColorPicker) {
            ColorPickerDialog(
                onColorSelected = { color ->
                    selectedColor = color
                    showColorPicker = false
                },
                onDismiss = { showColorPicker = false }
            )
        }

        if (showEraserSizePopup) {
            AlertDialog(
                onDismissRequest = { showEraserSizePopup = false },
                title = { Text("Veličina gumice") },
                text = {
                    Column {
                        Slider(
                            value = localEraserWidth,
                            onValueChange = { localEraserWidth = it },
                            valueRange = 8f..100f
                        )
                        Text("Trenutna veličina: ${localEraserWidth.toInt()}px")
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        whiteboardViewModel.setEraserSize(localEraserWidth)
                        showEraserSizePopup = false
                    }) {
                        Text("Gotovo")
                    }
                }
            )
        }

        if (showEraseModePopup) {
            AlertDialog(
                onDismissRequest = { showEraseModePopup = false },
                title = { Text("Način gumice") },
                text = {
                    Column {
                        TextButton(onClick = {
                            whiteboardViewModel.setEraseMode(EraseMode.COLOR)
                            showEraseModePopup = false
                        }) { Text("Boja") }
                        TextButton(onClick = {
                            whiteboardViewModel.setEraseMode(EraseMode.STROKE)
                            showEraseModePopup = false
                        }) { Text("Stroke") }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showEraseModePopup = false }) {
                        Text("Zatvori")
                    }
                }
            )
        }

        if (showStrokeWidthPopup) {
            AlertDialog(
                onDismissRequest = { showStrokeWidthPopup = false },
                title = { Text("Debljina olovke") },
                text = {
                    Column {
                        Slider(
                            value = strokeWidth,
                            onValueChange = { strokeWidth = it },
                            valueRange = 1f..20f
                        )
                        Text("Trenutna debljina: ${strokeWidth.toInt()}px")
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showStrokeWidthPopup = false }) {
                        Text("Gotovo")
                    }
                }
            )
        }

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


