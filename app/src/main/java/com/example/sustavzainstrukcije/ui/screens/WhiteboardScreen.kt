package com.example.sustavzainstrukcije.ui.screens

import android.util.Log
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.LineWeight
import androidx.compose.material.icons.filled.People
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.core.graphics.toColorInt
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.example.sustavzainstrukcije.R
import com.example.sustavzainstrukcije.ui.data.DrawingStroke
import com.example.sustavzainstrukcije.ui.data.EraseMode
import com.example.sustavzainstrukcije.ui.data.Point
import com.example.sustavzainstrukcije.ui.data.ToolMode
import com.example.sustavzainstrukcije.ui.viewmodels.SessionViewModel
import com.example.sustavzainstrukcije.ui.viewmodels.WhiteboardViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sqrt

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun WhiteboardScreen(
    sessionId: String,
    navController: NavHostController,
    whiteboardViewModel: WhiteboardViewModel = viewModel(),
    sessionViewModel: SessionViewModel = viewModel()
) {
    val strokes by whiteboardViewModel.strokes.collectAsState()
    val currentPage by whiteboardViewModel.currentPage.collectAsState()
    val allPages by whiteboardViewModel.allPages.collectAsState()
    val currentPageIndex by whiteboardViewModel.currentPageIndex.collectAsState()

    var isLoading by remember { mutableStateOf(true) }
    var currentPath by remember { mutableStateOf(Path()) }
    var currentPoints by remember { mutableStateOf(listOf<Point>()) }
    var selectedColor by remember { mutableStateOf(Color.Black) }
    var strokeWidth by remember { mutableFloatStateOf(5f) }
    var showColorPicker by remember { mutableStateOf(false) }

    val isEraser by whiteboardViewModel.isEraserActive.collectAsState()
    val eraserMode by whiteboardViewModel.eraseMode.collectAsState()
    val eraserWidth by whiteboardViewModel.eraserSize.collectAsState()

    val colorToUse = if (isEraser) Color.White else selectedColor

    var showEraserSizePopup by remember { mutableStateOf(false) }
    var localEraserWidth by remember { mutableFloatStateOf(eraserWidth) }
    var showEraseModePopup by remember { mutableStateOf(false) }

    var showStrokeWidthPopup by remember { mutableStateOf(false) }

    val toolMode by whiteboardViewModel.toolMode.collectAsState()

    var showToolSelector by remember { mutableStateOf(false) }

    var showTextInputDialog by remember { mutableStateOf(false) }
    var textInput by remember { mutableStateOf("") }
    var textInputOffset by remember { mutableStateOf(Offset.Zero) }

    var textFontSize by remember { mutableFloatStateOf(16f) }
    var isTextBold by remember { mutableStateOf(false) }

    var showRenameDialog by remember { mutableStateOf(false) }

    var showDeleteDialog by remember { mutableStateOf(false) }

    var showClearDialog by remember { mutableStateOf(false) }

    val pointers by whiteboardViewModel.pointers.collectAsState()
    val userNames by whiteboardViewModel.userNames.collectAsState()

    var showUserPopup by remember { mutableStateOf(false) }

    val currentSession by sessionViewModel.currentSession.collectAsState()
    val onlineUserIds by sessionViewModel.onlineUsers.collectAsState()
    val currentUserId = Firebase.auth.currentUser?.uid

    val currentUserIsInstructor = currentSession?.instructorId == currentUserId
    val instructorOnline = currentSession?.instructorId?.let { onlineUserIds.contains(it) } == true
    val isEditable = currentUserIsInstructor || instructorOnline

    val canUndo by whiteboardViewModel.canUndo.collectAsState()
    val canRedo by whiteboardViewModel.canRedo.collectAsState()



    LaunchedEffect(sessionId) {
        sessionViewModel.loadSession(sessionId)
        whiteboardViewModel.initializeWhiteboard(sessionId)
        sessionViewModel.setUserOnline(sessionId)
        sessionViewModel.listenToOnlineUsers(sessionId)
    }

    LaunchedEffect(allPages) {
        if (allPages.isNotEmpty()) {
            isLoading = false
        }
    }

    LaunchedEffect(instructorOnline) {
        Log.d("WB", "Instructor online = $instructorOnline")
    }

    LaunchedEffect(isEditable) {
        Log.d("WB", "isEditable = $isEditable")
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
                Text(
                    text = currentPage?.title ?: "Page ${currentPage?.pageNumber ?: 1}",
                    modifier = Modifier.combinedClickable(
                        onClick = {},
                        onLongClick = { showRenameDialog = true }
                    )
                )
            },
            navigationIcon = {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            },
            actions = {
                IconButton(onClick = { showUserPopup = true }) {
                    Icon(Icons.Default.People, contentDescription = "Online users")
                }
                IconButton(onClick = { showDeleteDialog = true }) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete page")
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
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Previous page")
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
                                contentDescription = "New page",
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
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Next page")
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
                    Icon(Icons.Default.LineWeight, contentDescription = "Pen width")
                }

                IconButton(
                    onClick = { whiteboardViewModel.toggleEraser() },
                    modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_eraser),
                        contentDescription = "Eraser",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
                IconButton(onClick = { showToolSelector = true }) {
                    Icon(Icons.Default.Build, contentDescription = "Choose a tool")
                }
            } else {
                IconButton(onClick = { showEraseModePopup = true }) {
                    Icon(
                        Icons.Default.Build,
                        contentDescription = "Choose eraser type"
                    )
                }

                IconButton(onClick = { showEraserSizePopup = true }) {
                    Icon(
                        Icons.Default.LineWeight,
                        contentDescription = "Pick eraser width"
                    )
                }

                IconButton(
                    onClick = { whiteboardViewModel.toggleEraser() },
                    modifier = Modifier.background(MaterialTheme.colorScheme.primaryContainer, CircleShape)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_eraser),
                        contentDescription = "Switch to drawing",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            IconButton(onClick = { whiteboardViewModel.undo() }, enabled = isEditable && canUndo) {
                Icon(
                    Icons.AutoMirrored.Filled.Undo,
                    contentDescription = "Undo"
                )
            }

            IconButton(onClick = { whiteboardViewModel.redo() }, enabled = isEditable && canRedo) {
                Icon(
                    Icons.AutoMirrored.Filled.Redo,
                    contentDescription = "Redo"
                )
            }

            IconButton(onClick = {
                showClearDialog = true
            }) {
                Icon(
                    Icons.Default.DeleteOutline,
                    contentDescription = "Clear the page"
                )
            }

        }

        // 4. Canvas
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
                    .pointerInput(isEditable, isEraser, eraserMode, strokes) {
                        detectTapGestures { offset ->
                            if (offset.x < 0 || offset.y < 0 || offset.x > size.width || offset.y > size.height || !isEditable) {
                                return@detectTapGestures
                            }
                            if (isEraser && eraserMode == EraseMode.STROKE) {
                                // Pronađi i obriši stroke na toj poziciji
                                val strokeToRemove = strokes.lastOrNull { isTouchingStroke(it, offset.x, offset.y) }
                                strokeToRemove?.let { whiteboardViewModel.removeStroke(it.id) }
                            } else if (!isEraser) {
                                if (toolMode == ToolMode.TEXT) {
                                    showTextInputDialog = true
                                    textInputOffset = offset
                                } else if (toolMode == ToolMode.POINTER) {
                                    whiteboardViewModel.sendPointer(sessionId, Point(offset.x, offset.y))
                                    return@detectTapGestures
                                }
                                    else {
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
                    }
                    .pointerInput(isEditable, isEraser, eraserMode) {
                        detectDragGestures(
                            onDragStart = { offset ->
                                if (offset.x < 0 || offset.y < 0 || offset.x > size.width || offset.y > size.height || !isEditable) return@detectDragGestures

                                currentPoints = listOf(Point(offset.x, offset.y))

                                if (isEraser && eraserMode == EraseMode.STROKE) {
                                    val strokeToRemove = strokes.lastOrNull { stroke ->
                                        if (stroke.color == "#FFFFFF") return@lastOrNull false
                                        stroke.points.any { point ->
                                            val dx = point.x - offset.x
                                            val dy = point.y - offset.y
                                            sqrt(dx * dx + dy * dy) < stroke.strokeWidth * 2
                                        }
                                    }
                                    strokeToRemove?.let {
                                        whiteboardViewModel.removeStroke(it.id)
                                    }

                                    return@detectDragGestures
                                }


                                if (toolMode == ToolMode.DRAW || (isEraser && eraserMode == EraseMode.COLOR)) {
                                    currentPath = Path().apply { moveTo(offset.x, offset.y) }
                                }
                            },
                            onDrag = { change, _ ->
                                if (!isEditable) return@detectDragGestures
                                val newPoint = Point(change.position.x, change.position.y)

                                // Za oblike: uvijek se čuva početna i krajnja točka
                                if (toolMode == ToolMode.SHAPE_RECT || toolMode == ToolMode.SHAPE_CIRCLE || toolMode == ToolMode.SHAPE_LINE) {
                                    currentPoints = listOf(currentPoints.firstOrNull() ?: newPoint, newPoint)
                                    return@detectDragGestures
                                }

                                when {
                                    isEraser && eraserMode == EraseMode.COLOR -> {
                                        currentPoints = currentPoints + newPoint
                                        currentPath.lineTo(newPoint.x, newPoint.y)
                                    }

                                    toolMode == ToolMode.DRAW -> {
                                        currentPoints = currentPoints + newPoint
                                        currentPath.lineTo(newPoint.x, newPoint.y)
                                    }
                                }
                            },

                            onDragEnd = {
                                if (!isEditable) return@detectDragGestures
                                val shouldAdd = when {
                                    toolMode == ToolMode.DRAW && currentPoints.size > 1 -> true
                                    toolMode in listOf(ToolMode.SHAPE_LINE, ToolMode.SHAPE_RECT, ToolMode.SHAPE_CIRCLE) && currentPoints.size == 2 -> true
                                    isEraser && eraserMode == EraseMode.COLOR && currentPoints.size > 1 -> true
                                    else -> false
                                }

                                if (shouldAdd) {
                                    val strokeColor = if (isEraser) {
                                        String.format("#%06X", Color.White.toArgb() and 0xFFFFFF)
                                    } else {
                                        String.format("#%06X", selectedColor.toArgb() and 0xFFFFFF)
                                    }

                                    val strokeWidthToUse =
                                        if (isEraser) eraserWidth else strokeWidth

                                    val shapeType = when (toolMode) {
                                        ToolMode.SHAPE_RECT -> "rect"
                                        ToolMode.SHAPE_CIRCLE -> "circle"
                                        ToolMode.SHAPE_LINE -> "line"
                                        else -> null
                                    }

                                    whiteboardViewModel.addStroke(
                                        points = currentPoints,
                                        color = strokeColor,
                                        strokeWidth = strokeWidthToUse,
                                        shapeType = shapeType
                                    )
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
                        when {
                            stroke.shapeType?.startsWith("text:") == true && stroke.points.size == 1 -> {

                                val full = stroke.shapeType
                                val text = full.substringAfter("text:").substringBefore(";")
                                val fontSize = full.substringAfter("font=").substringBefore(";")
                                    .toFloatOrNull() ?: (stroke.strokeWidth * 6)
                                val isBold = full.substringAfter("bold=").toBooleanStrictOrNull() ?: false

                                drawContext.canvas.nativeCanvas.drawText(
                                    text,
                                    stroke.points[0].x,
                                    stroke.points[0].y,
                                    android.graphics.Paint().apply {
                                        this.color = stroke.color.toColorInt()
                                        textSize = fontSize
                                        isFakeBoldText = isBold
                                        isAntiAlias = true
                                    }
                                )
                            }

                            stroke.points.size == 1 -> {
                                val point = stroke.points.first()
                                drawCircle(
                                    color = color,
                                    radius = stroke.strokeWidth / 2f,
                                    center = Offset(point.x, point.y)
                                )
                            }

                            stroke.points.size == 2 && stroke.shapeType != null -> {
                                val p1 = Offset(stroke.points[0].x, stroke.points[0].y)
                                val p2 = Offset(stroke.points[1].x, stroke.points[1].y)

                                when (stroke.shapeType) {
                                    "rect" -> drawRect(
                                        color = color,
                                        topLeft = Offset(minOf(p1.x, p2.x), minOf(p1.y, p2.y)),
                                        size = androidx.compose.ui.geometry.Size(
                                            width = abs(p2.x - p1.x),
                                            height = abs(p2.y - p1.y)
                                        ),
                                        style = Stroke(width = stroke.strokeWidth)
                                    )

                                    "circle" -> {
                                        val radius =
                                            sqrt((p2.x - p1.x).pow(2) + (p2.y - p1.y).pow(2)) / 2f
                                        val center = Offset((p1.x + p2.x) / 2, (p1.y + p2.y) / 2)
                                        drawCircle(
                                            color = color,
                                            radius = radius,
                                            center = center,
                                            style = Stroke(width = stroke.strokeWidth)
                                        )
                                    }

                                    "line" -> drawLine(
                                        color = color,
                                        start = p1,
                                        end = p2,
                                        strokeWidth = stroke.strokeWidth,
                                        cap = StrokeCap.Round
                                    )
                                }
                            }

                            stroke.shapeType == null && stroke.points.size > 1 -> {
                                val path = Path().apply {
                                    moveTo(stroke.points[0].x, stroke.points[0].y)
                                    for (point in stroke.points.drop(1)) {
                                        lineTo(point.x, point.y)
                                    }
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
                }

                pointers.forEach { (userId, point) ->
                    drawCircle(
                        color = Color.Red,
                        center = Offset(point.x, point.y),
                        radius = 10f
                    )

                    val label = userNames[userId] ?: userId.take(6)

                    drawContext.canvas.nativeCanvas.drawText(
                        label,
                        point.x + 12,
                        point.y - 12,
                        android.graphics.Paint().apply {
                            color = android.graphics.Color.BLACK
                            textSize = 28f
                            isFakeBoldText = true
                        }
                    )
                }


                if (isEraser && currentPoints.isNotEmpty()) {
                    val lastPoint = currentPoints.last()

                    drawCircle(
                        color = Color.White.copy(alpha = 0.6f),
                        radius = eraserWidth / 2f,
                        center = Offset(lastPoint.x, lastPoint.y)
                    )

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

                if (currentPoints.size == 2 && !isEraser) {
                    val p1 = Offset(currentPoints[0].x, currentPoints[0].y)
                    val p2 = Offset(currentPoints[1].x, currentPoints[1].y)

                    when (toolMode) {
                        ToolMode.SHAPE_RECT -> drawRect(
                            color = colorToUse,
                            topLeft = Offset(minOf(p1.x, p2.x), minOf(p1.y, p2.y)),
                            size = androidx.compose.ui.geometry.Size(
                                width = abs(p2.x - p1.x),
                                height = abs(p2.y - p1.y)
                            ),
                            style = Stroke(width = strokeWidth)
                        )

                        ToolMode.SHAPE_LINE -> drawLine(
                            color = colorToUse,
                            start = p1,
                            end = p2,
                            strokeWidth = strokeWidth,
                            cap = StrokeCap.Round
                        )

                        ToolMode.SHAPE_CIRCLE -> {
                            val radius = sqrt((p2.x - p1.x).pow(2) + (p2.y - p1.y).pow(2)) / 2f
                            val center = Offset((p1.x + p2.x) / 2, (p1.y + p2.y) / 2)
                            drawCircle(
                                color = colorToUse,
                                radius = radius,
                                center = center,
                                style = Stroke(width = strokeWidth)
                            )
                        }

                        else -> {}
                    }
                }

            }
        }

        if (showColorPicker) {
            ColorPickerDialog(
                onColorSelected = { color ->
                    selectedColor = color
                    showColorPicker = false
                },
                onDismiss = { showColorPicker = false }
            )
        }

        DisposableEffect(sessionId) {
            onDispose {
                sessionViewModel.setUserOffline(sessionId)
            }
        }

        DisposableEffect(Unit) {
            sessionViewModel.setUserOnline(sessionId)
            onDispose {
                sessionViewModel.setUserOffline(sessionId)
            }
        }


        if (showEraserSizePopup) {
            AlertDialog(
                onDismissRequest = { showEraserSizePopup = false },
                title = { Text("Eraser Width") },
                text = {
                    Column {
                        Slider(
                            value = localEraserWidth,
                            onValueChange = { localEraserWidth = it },
                            valueRange = 8f..100f
                        )
                        Text("Current width: ${localEraserWidth.toInt()}px")
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        whiteboardViewModel.setEraserSize(localEraserWidth)
                        showEraserSizePopup = false
                    }) {
                        Text("Done")
                    }
                }
            )
        }

        if (showEraseModePopup) {
            AlertDialog(
                onDismissRequest = { showEraseModePopup = false },
                title = { Text("Eraser Type") },
                text = {
                    Column {
                        TextButton(onClick = {
                            whiteboardViewModel.setEraseMode(EraseMode.COLOR)
                            showEraseModePopup = false
                        }, colors = ButtonDefaults.textButtonColors(
                            containerColor = if (eraserMode == EraseMode.COLOR) Color.LightGray else Color.Transparent
                        )) { Text("White Color") }
                        TextButton(onClick = {
                            whiteboardViewModel.setEraseMode(EraseMode.STROKE)
                            showEraseModePopup = false
                        }, colors = ButtonDefaults.textButtonColors(
                                containerColor = if (eraserMode == EraseMode.STROKE) Color.LightGray else Color.Transparent
                            )) { Text("Remove Stroke") }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showEraseModePopup = false }) {
                        Text("Close")
                    }
                }
            )
        }

        if (showStrokeWidthPopup) {
            AlertDialog(
                onDismissRequest = { showStrokeWidthPopup = false },
                title = { Text("Pen Width") },
                text = {
                    Column {
                        Slider(
                            value = strokeWidth,
                            onValueChange = { strokeWidth = it },
                            valueRange = 1f..20f
                        )
                        Text("Current width: ${strokeWidth.toInt()}px")
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showStrokeWidthPopup = false }) {
                        Text("Done")
                    }
                }
            )
        }

        if (showToolSelector) {
            AlertDialog(
                onDismissRequest = { showToolSelector = false },
                title = { Text("Choose Tool") },
                text = {
                    Column {
                        TextButton(onClick = {
                            whiteboardViewModel.setToolMode(ToolMode.DRAW, sessionId)
                            showToolSelector = false
                        }, colors = ButtonDefaults.textButtonColors(
                            containerColor = if (toolMode == ToolMode.DRAW) Color.LightGray else Color.Transparent
                        )
                        ) { Text("Pen") }

                        TextButton(onClick = {
                            whiteboardViewModel.setToolMode(ToolMode.SHAPE_RECT, sessionId)
                            showToolSelector = false
                        }, colors = ButtonDefaults.textButtonColors(
                            containerColor = if (toolMode == ToolMode.SHAPE_RECT) Color.LightGray else Color.Transparent
                        )) { Text("Rectangle") }

                        TextButton(onClick = {
                            whiteboardViewModel.setToolMode(ToolMode.SHAPE_CIRCLE, sessionId)
                            showToolSelector = false
                        }, colors = ButtonDefaults.textButtonColors(
                            containerColor = if (toolMode == ToolMode.SHAPE_CIRCLE) Color.LightGray else Color.Transparent
                        )) { Text("Circle") }

                        TextButton(onClick = {
                            whiteboardViewModel.setToolMode(ToolMode.SHAPE_LINE, sessionId)
                            showToolSelector = false
                        }, colors = ButtonDefaults.textButtonColors(
                            containerColor = if (toolMode == ToolMode.SHAPE_LINE) Color.LightGray else Color.Transparent
                        )) { Text("Line") }

                        TextButton(onClick = {
                            whiteboardViewModel.setToolMode(ToolMode.TEXT, sessionId)
                            showToolSelector = false
                        }, colors = ButtonDefaults.textButtonColors(
                            containerColor = if (toolMode == ToolMode.TEXT) Color.LightGray else Color.Transparent
                        )) { Text("Text") }

                        TextButton(onClick = {
                            whiteboardViewModel.setToolMode(ToolMode.POINTER, sessionId)
                            showToolSelector = false
                        }, colors = ButtonDefaults.textButtonColors(
                            containerColor = if (toolMode == ToolMode.POINTER) Color.LightGray else Color.Transparent
                        )) { Text("Pointer") }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showToolSelector = false }) {
                        Text("Close")
                    }
                }
            )
        }

        if (showTextInputDialog) {
            AlertDialog(
                onDismissRequest = { showTextInputDialog = false },
                title = { Text("Enter Text") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = textInput,
                            onValueChange = { textInput = it },
                            label = { Text("Text") }
                        )

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "Size: ${textFontSize.toInt()}sp",
                                modifier = Modifier.width(100.dp)
                            )
                            Slider(
                                value = textFontSize,
                                onValueChange = { textFontSize = it },
                                valueRange = 12f..64f,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            androidx.compose.material3.Checkbox(
                                checked = isTextBold,
                                onCheckedChange = { isTextBold = it }
                            )
                            Text("Bolded")
                        }
                    }
                },

                confirmButton = {
                    TextButton(onClick = {
                        val textPoint = listOf(Point(textInputOffset.x, textInputOffset.y))
                        whiteboardViewModel.addStroke(
                            points = textPoint,
                            color = String.format("#%06X", selectedColor.toArgb() and 0xFFFFFF),
                            strokeWidth = strokeWidth,
                            shapeType = "text:$textInput;font=${textFontSize.toInt()};bold=${isTextBold}"
                        )
                        showTextInputDialog = false
                        textInput = ""
                    }) {
                        Text("Enter")
                    }
                },
                dismissButton = {
                    TextButton(onClick = {
                        showTextInputDialog = false
                        textInput = ""
                    }) {
                        Text("Cancel")
                    }
                }
            )
        }

        if (showRenameDialog && isEditable) {
            var newTitle by remember { mutableStateOf(currentPage?.title ?: "") }

            AlertDialog(
                onDismissRequest = { showRenameDialog = false },
                title = { Text("Edit Page Name") },
                text = {
                    OutlinedTextField(
                        value = newTitle,
                        onValueChange = { newTitle = it },
                        label = { Text("Name") }
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        whiteboardViewModel.renamePage(currentPage?.id.orEmpty(), newTitle)
                        showRenameDialog = false
                    }) { Text("Save") }
                },
                dismissButton = {
                    TextButton(onClick = { showRenameDialog = false }) { Text("Cancel") }
                }
            )
        }

        if (showDeleteDialog && isEditable) {
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                title = { Text("Delete Page") },
                text = { Text("Are you sure you want to delete this page?") },
                confirmButton = {
                    TextButton(onClick = {
                        whiteboardViewModel.deleteCurrentPage()
                        showDeleteDialog = false
                    }) {
                        Text("Delete")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }

        if (showClearDialog && isEditable) {
            AlertDialog(
                onDismissRequest = { showClearDialog = false },
                title = { Text("Clear Page") },
                text = { Text("Are you sure you want to clear this page?") },
                confirmButton = {
                    TextButton(onClick = {
                        whiteboardViewModel.clearCurrentPage()
                        showClearDialog = false
                    }) { Text("Yes") }
                },
                dismissButton = {
                    TextButton(onClick = { showClearDialog = false }) { Text("Cancel") }
                }
            )
        }

        if (showUserPopup) {
            AlertDialog(
                onDismissRequest = { showUserPopup = false },
                title = { Text("Users") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        val allUserIds = listOfNotNull(currentSession?.instructorId, currentSession?.studentId)
                        allUserIds.forEach { userId ->
                            val name = userNames[userId] ?: userId.take(6)
                            val isOnline = onlineUserIds.contains(userId)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(if (isOnline) Color(0xFF4CAF50) else Color.Gray)
                                )
                                Text(text = "  $name", modifier = Modifier.padding(start = 4.dp))
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showUserPopup = false }) {
                        Text("Close")
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
        title = { Text("Pick Color") },
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
                Text("Cancel")
            }
        }
    )
}

fun isTouchingStroke(stroke: DrawingStroke, x: Float, y: Float): Boolean {
    if (stroke.color == "#FFFFFF") return false

    return when {
        stroke.shapeType?.startsWith("text:") == true && stroke.points.size == 1 -> {
            val textX = stroke.points[0].x
            val textY = stroke.points[0].y
            val fontSize = stroke.shapeType.substringAfter("font=")
                .substringBefore(";")
                .toFloatOrNull() ?: (stroke.strokeWidth * 6)
            val tolerance = fontSize
            abs(x - textX) < tolerance * 4 && abs(y - textY) < tolerance
        }

        stroke.shapeType == "circle" && stroke.points.size == 2 -> {
            val p1 = stroke.points[0]
            val p2 = stroke.points[1]
            val centerX = (p1.x + p2.x) / 2
            val centerY = (p1.y + p2.y) / 2
            val radius = sqrt((p2.x - p1.x).pow(2) + (p2.y - p1.y).pow(2)) / 2
            val distance = sqrt((x - centerX).pow(2) + (y - centerY).pow(2))
            abs(distance - radius) < stroke.strokeWidth * 1.5f
        }

        stroke.shapeType == "rect" && stroke.points.size == 2 -> {
            val left = minOf(stroke.points[0].x, stroke.points[1].x)
            val right = maxOf(stroke.points[0].x, stroke.points[1].x)
            val top = minOf(stroke.points[0].y, stroke.points[1].y)
            val bottom = maxOf(stroke.points[0].y, stroke.points[1].y)
            val border = stroke.strokeWidth * 1.5f

            val isNearLeft = abs(x - left) < border && y in top..bottom
            val isNearRight = abs(x - right) < border && y in top..bottom
            val isNearTop = abs(y - top) < border && x in left..right
            val isNearBottom = abs(y - bottom) < border && x in left..right

            isNearLeft || isNearRight || isNearTop || isNearBottom
        }

        stroke.shapeType == "line" && stroke.points.size == 2 -> {
            val (x1, y1) = stroke.points[0]
            val (x2, y2) = stroke.points[1]
            val lineLength = sqrt((x2 - x1).pow(2) + (y2 - y1).pow(2))
            if (lineLength == 0f) return false

            val distance = abs((y2 - y1) * x - (x2 - x1) * y + x2 * y1 - y2 * x1) / lineLength
            val minX = minOf(x1, x2) - stroke.strokeWidth
            val maxX = maxOf(x1, x2) + stroke.strokeWidth
            val minY = minOf(y1, y2) - stroke.strokeWidth
            val maxY = maxOf(y1, y2) + stroke.strokeWidth

            distance < stroke.strokeWidth * 1.5f && x in minX..maxX && y in minY..maxY
        }

        else -> {
            stroke.points.any {
                val dx = it.x - x
                val dy = it.y - y
                sqrt(dx * dx + dy * dy) < stroke.strokeWidth * 2
            }
        }
    }
}




