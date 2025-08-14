package com.example.sustavzainstrukcije.ui.screens

import android.util.Log
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
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
import com.example.sustavzainstrukcije.ui.data.Aabb
import com.example.sustavzainstrukcije.ui.data.DrawingStroke
import com.example.sustavzainstrukcije.ui.data.EraseMode
import com.example.sustavzainstrukcije.ui.data.Hit
import com.example.sustavzainstrukcije.ui.data.Point
import com.example.sustavzainstrukcije.ui.data.ToolMode
import com.example.sustavzainstrukcije.ui.viewmodels.SessionViewModel
import com.example.sustavzainstrukcije.ui.viewmodels.WhiteboardViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.firestore
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sqrt

private const val HIT_TOLERANCE_SCREEN_PX = 10f

private fun screenTolToWorld(scale: Float): Float = HIT_TOLERANCE_SCREEN_PX / scale

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


    var pan by remember { mutableStateOf(Offset.Zero) }      // world offset
    var scale by remember { mutableFloatStateOf(1f) }        // 1f = 100%
    val minScale = 0.25f
    val maxScale = 5f

    fun worldToScreen(p: Point, pan: Offset, scale: Float) =
        Offset((p.x - pan.x) * scale, (p.y - pan.y) * scale)

    fun screenToWorld(o: Offset, pan: Offset, scale: Float) =
        Point(o.x / scale + pan.x, o.y / scale + pan.y)

    var lastCentroid by remember { mutableStateOf(Offset.Zero) }

    val transformState = rememberTransformableState { zoomChange, offsetChange, _ ->
        val newScale = (scale * zoomChange).coerceIn(minScale, maxScale)
        if (newScale != scale) {
            val worldBefore = screenToWorld(lastCentroid, pan, scale)
            scale = newScale
            val worldAfter = screenToWorld(lastCentroid, pan, scale)
            pan -= Offset(worldAfter.x - worldBefore.x, worldAfter.y - worldBefore.y)
        }
        pan -= offsetChange / scale
    }




    DisposableEffect(sessionId, currentUserId) {
        if (currentUserIsInstructor) {
            val presenceRef = Firebase.firestore
                .collection("onlineUsers")
                .document(sessionId)

            presenceRef.set(
                mapOf("userIds" to FieldValue.arrayUnion(currentUserId)),
                SetOptions.merge()
            )

            onDispose {
                presenceRef.update("userIds", FieldValue.arrayRemove(currentUserId))
            }
        } else {
            onDispose { }
        }
    }




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

    LaunchedEffect(Unit) {
        if (currentUserId != null) {
            Firebase.firestore.collection("users")
                .document(currentUserId)
                .collection("lastVisited")
                .document(sessionId)
                .set(mapOf("timestamp" to System.currentTimeMillis()))
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

        // Navigacija kroz stranice
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
                if (isEditable && allPages.isNotEmpty() && currentPageIndex == allPages.size - 1) {
                    item {
                        IconButton(
                            onClick = { whiteboardViewModel.createNewPage(sessionId) },
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

        // Alati za crtanje
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
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

        // Canvas
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
                    // 1) Transform + double tap: SAMO u FREE_ROAM
                    // transformable + hvatanje centroida: SAMO u FREE_ROAM
                    .then(
                        if (toolMode == ToolMode.FREE_ROAM)
                            Modifier.pointerInput(toolMode) {
                                detectTransformGestures(panZoomLock = true) { centroid, _, _, _ ->
                                    lastCentroid = centroid
                                }
                            }
                        else Modifier
                    )
                    // 2) Stvarni pan/zoom preko transformable + rememberTransformableState
                    .then(
                        if (toolMode == ToolMode.FREE_ROAM)
                            Modifier.transformable(state = transformState)
                        else Modifier
                    )
                    // 3) Double-tap reset
                    .then(
                        if (toolMode == ToolMode.FREE_ROAM)
                            Modifier.pointerInput("free_roam_double_tap", scale, pan) {
                                detectTapGestures(onDoubleTap = {
                                    pan = Offset.Zero
                                    scale = 1f
                                })
                            }
                        else Modifier
                    )
                    .then(
                        if (toolMode != ToolMode.FREE_ROAM)
                            Modifier.pointerInput(isEditable, isEraser, eraserMode, strokes, toolMode) {
                                detectTapGestures { offset ->
                                    if (!isEditable) return@detectTapGestures
                                    val world = screenToWorld(offset, pan, scale)

                                    if (isEraser && eraserMode == EraseMode.STROKE) {
                                        val world = screenToWorld(offset, pan, scale)
                                        val candidates = strokes.asReversed().mapIndexedNotNull { idx, s ->
                                            if (s.color == "#FFFFFF") return@mapIndexedNotNull null // ako želiš preskakati “paint erase” poteze
                                            val d = distanceToStrokeWorld(world.x, world.y, s, scale)
                                            val tol = maxOf(screenTolToWorld(scale), s.strokeWidth * 0.75f)
                                            if (d <= tol) Hit(s, d, idx) else null
                                        }
                                        val toRemove = candidates.minWithOrNull(compareBy<Hit> { it.dist }.thenBy { it.z })?.stroke
                                        toRemove?.let { whiteboardViewModel.removeStroke(it.id) }
                                    }
                                    else if (!isEraser) {
                                        if (toolMode == ToolMode.TEXT) {
                                            showTextInputDialog = true
                                            textInputOffset = Offset(world.x, world.y) // spremi world lokaciju
                                        } else if (toolMode == ToolMode.POINTER) {
                                            whiteboardViewModel.sendPointer(sessionId, Point(world.x, world.y))
                                        } else {
                                            whiteboardViewModel.addStroke(
                                                points = listOf(Point(world.x, world.y)),
                                                color = String.format("#%06X", selectedColor.toArgb() and 0xFFFFFF),
                                                strokeWidth = strokeWidth
                                            )
                                        }
                                    }
                                }
                            }
                        else Modifier
                    )

                    .then(
                        if (toolMode != ToolMode.FREE_ROAM)
                            Modifier.pointerInput(isEditable, isEraser, eraserMode, toolMode) {
                                detectDragGestures(
                                    onDragStart = { startOffset ->
                                        if (!isEditable) return@detectDragGestures
                                        val w = screenToWorld(startOffset, pan, scale)
                                        currentPoints = listOf(Point(w.x, w.y))
                                        if (toolMode == ToolMode.DRAW || (isEraser && eraserMode == EraseMode.COLOR)) {
                                            currentPath = Path().apply { moveTo((w.x - pan.x) * scale, (w.y - pan.y) * scale) } // vidi napomenu dolje
                                        }
                                    },
                                    onDrag = { change, _ ->
                                        if (!isEditable) return@detectDragGestures
                                        val w = screenToWorld(change.position, pan, scale)
                                        val newPoint = Point(w.x, w.y)

                                        if (toolMode in listOf(ToolMode.SHAPE_RECT, ToolMode.SHAPE_CIRCLE, ToolMode.SHAPE_LINE)) {
                                            currentPoints = listOf(currentPoints.firstOrNull() ?: newPoint, newPoint)
                                            return@detectDragGestures
                                        }

                                        when {
                                            isEraser && eraserMode == EraseMode.COLOR -> {
                                                currentPoints = currentPoints + newPoint
                                                // ako koristiš Path za preview, Path radi u screen space-u:
                                                currentPath.lineTo((w.x - pan.x) * scale, (w.y - pan.y) * scale)
                                            }
                                            toolMode == ToolMode.DRAW -> {
                                                currentPoints = currentPoints + newPoint
                                                currentPath.lineTo((w.x - pan.x) * scale, (w.y - pan.y) * scale)
                                            }
                                        }
                                    },
                                    onDragEnd = {
                                        if (!isEditable) return@detectDragGestures
                                        val shouldAdd =
                                            (toolMode == ToolMode.DRAW && currentPoints.size > 1) ||
                                                    (toolMode in listOf(ToolMode.SHAPE_LINE, ToolMode.SHAPE_RECT, ToolMode.SHAPE_CIRCLE) && currentPoints.size == 2) ||
                                                    (isEraser && eraserMode == EraseMode.COLOR && currentPoints.size > 1)

                                        if (shouldAdd) {
                                            val strokeColor = if (isEraser) {
                                                String.format("#%06X", Color.White.toArgb() and 0xFFFFFF)
                                            } else {
                                                String.format("#%06X", selectedColor.toArgb() and 0xFFFFFF)
                                            }
                                            val strokeWidthToUse = if (isEraser) eraserWidth else strokeWidth
                                            val shapeType = when (toolMode) {
                                                ToolMode.SHAPE_RECT -> "rect"
                                                ToolMode.SHAPE_CIRCLE -> "circle"
                                                ToolMode.SHAPE_LINE -> "line"
                                                else -> null
                                            }
                                            whiteboardViewModel.addStroke(
                                                points = currentPoints,          // world točke!
                                                color = strokeColor,
                                                strokeWidth = strokeWidthToUse,  // world debljina!
                                                shapeType = shapeType
                                            )
                                        }
                                        currentPoints = emptyList()
                                        currentPath = Path()
                                    }
                                )
                            }
                        else Modifier
                    )

            ) {
                strokes.forEach { stroke ->
                    if (stroke.points.isNotEmpty()) {
                        when {
                            stroke.shapeType?.startsWith("text:") == true && stroke.points.size == 1 -> {

                                val sp = worldToScreen(stroke.points[0], pan, scale)
                                val full = stroke.shapeType ?: ""
                                val text = full.substringAfter("text:").substringBefore(";")
                                val fontSize = full.substringAfter("font=").substringBefore(";").toFloatOrNull()
                                    ?: (stroke.strokeWidth * 6)
                                val isBold = full.substringAfter("bold=").toBooleanStrictOrNull() ?: false

                                drawContext.canvas.nativeCanvas.drawText(
                                    text,
                                    sp.x,
                                    sp.y,
                                    android.graphics.Paint().apply {
                                        color = stroke.color.toColorInt()
                                        textSize = fontSize * scale
                                        isFakeBoldText = isBold
                                        isAntiAlias = true
                                    }
                                )

                            }

                            stroke.points.size == 1 -> {
                                val sp = worldToScreen(stroke.points.first(), pan, scale)
                                drawCircle(
                                    color = Color(stroke.color.toColorInt()),
                                    radius = (stroke.strokeWidth / 2f) * scale,
                                    center = sp
                                )
                            }

                            stroke.points.size == 2 && stroke.shapeType != null -> {
                                val p1 = worldToScreen(stroke.points[0], pan, scale)
                                val p2 = worldToScreen(stroke.points[1], pan, scale)
                                when (stroke.shapeType) {
                                    "line" -> drawLine(
                                        color = Color(stroke.color.toColorInt()),
                                        start = p1, end = p2,
                                        strokeWidth = stroke.strokeWidth * scale,
                                        cap = StrokeCap.Round
                                    )
                                    "rect" -> drawRect(
                                        color = Color(stroke.color.toColorInt()),
                                        topLeft = Offset(minOf(p1.x, p2.x), minOf(p1.y, p2.y)),
                                        size = androidx.compose.ui.geometry.Size(abs(p2.x - p1.x), abs(p2.y - p1.y)),
                                        style = Stroke(width = stroke.strokeWidth * scale)
                                    )
                                    "circle" -> {
                                        val radius = kotlin.math.sqrt((p2.x - p1.x).pow(2) + (p2.y - p1.y).pow(2)) / 2f
                                        val center = Offset((p1.x + p2.x) / 2f, (p1.y + p2.y) / 2f)
                                        drawCircle(
                                            color = Color(stroke.color.toColorInt()),
                                            radius = radius,
                                            center = center,
                                            style = Stroke(width = stroke.strokeWidth * scale)
                                        )
                                    }
                                }
                            }

                            stroke.shapeType == null && stroke.points.size > 1 -> {
                                val pts = stroke.points
                                if (pts.size > 1) {
                                    val sp0 = worldToScreen(pts[0], pan, scale)
                                    val path = Path().apply {
                                        moveTo(sp0.x, sp0.y)
                                        for (p in pts.drop(1)) {
                                            val sp = worldToScreen(p, pan, scale)
                                            lineTo(sp.x, sp.y)
                                        }
                                    }
                                    drawPath(
                                        path = path,
                                        color = Color(stroke.color.toColorInt()),
                                        style = Stroke(
                                            width = stroke.strokeWidth * scale,
                                            cap = StrokeCap.Round,
                                            join = StrokeJoin.Round
                                        )
                                    )
                                }

                            }
                        }
                    }
                }

                pointers.forEach { (userId, point) ->
                    val sp = worldToScreen(point, pan, scale)
                    drawCircle(Color.Red, center = sp, radius = 10f) // možeš držati fiksno 10f u screen space-u
                    val label = userNames[userId] ?: userId.take(6)
                    drawContext.canvas.nativeCanvas.drawText(
                        label, sp.x + 12, sp.y - 12,
                        android.graphics.Paint().apply {
                            color = android.graphics.Color.BLACK
                            textSize = 28f
                            isFakeBoldText = true
                        }
                    )
                }



                if (isEraser && currentPoints.isNotEmpty()) {
                    val lastWorld = currentPoints.last()
                    val sp = worldToScreen(lastWorld, pan, scale)
                    val r = (eraserWidth / 2f) * scale
                    drawCircle(Color.White.copy(alpha = 0.6f), radius = r, center = sp)
                    drawCircle(Color.Gray, radius = r, center = sp, style = Stroke(width = 2f))
                }


                if (currentPoints.isNotEmpty() && (!isEraser || eraserMode != EraseMode.STROKE)) {
                    drawPath(
                        path = currentPath,
                        color = colorToUse,
                        style = Stroke(
                            width = (if (isEraser) eraserWidth else strokeWidth) * scale,
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round
                        )
                    )
                }

                if (currentPoints.size == 2 && !isEraser) {
                    val p1s = worldToScreen(currentPoints[0], pan, scale)
                    val p2s = worldToScreen(currentPoints[1], pan, scale)
                    when (toolMode) {
                        ToolMode.SHAPE_RECT -> drawRect(
                            color = colorToUse,
                            topLeft = Offset(minOf(p1s.x, p2s.x), minOf(p1s.y, p2s.y)),
                            size = androidx.compose.ui.geometry.Size(abs(p2s.x - p1s.x), abs(p2s.y - p1s.y)),
                            style = Stroke(width = strokeWidth * scale)
                        )
                        ToolMode.SHAPE_LINE -> drawLine(
                            color = colorToUse,
                            start = p1s, end = p2s,
                            strokeWidth = strokeWidth * scale,
                            cap = StrokeCap.Round
                        )
                        ToolMode.SHAPE_CIRCLE -> {
                            val radius = sqrt((p2s.x - p1s.x).pow(2) + (p2s.y - p1s.y).pow(2)) / 2f
                            val center = Offset((p1s.x + p2s.x) / 2f, (p1s.y + p2s.y) / 2f)
                            drawCircle(
                                color = colorToUse,
                                radius = radius,
                                center = center,
                                style = Stroke(width = strokeWidth * scale)
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

                        TextButton(onClick = {
                            whiteboardViewModel.setToolMode(ToolMode.FREE_ROAM, sessionId)
                            showToolSelector = false
                        }, colors = ButtonDefaults.textButtonColors(
                            containerColor = if (toolMode == ToolMode.FREE_ROAM) Color.LightGray else Color.Transparent
                        )) { Text("Free roam") }

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
                        val allUserIds = listOfNotNull(currentSession?.instructorId) +
                                (currentSession?.studentIds ?: emptyList())
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

fun distanceToStrokeWorld(x: Float, y: Float, s: DrawingStroke, scale: Float): Float {
    val worldTol = screenTolToWorld(scale)
    val edgeTol = maxOf(worldTol, s.strokeWidth * 0.75f)
    // Vrati minimalnu metriku udaljenosti po tipu (isti uvjeti kao u isTouchingStroke),
    // ali BEZ praga, samo “koliko je daleko” (0..∞).
    return when {
        s.shapeType?.startsWith("text:") == true && s.points.size == 1 -> {
            // bbox distance do pravokutnika (0 kada je unutra)
            val textX = s.points[0].x
            val textY = s.points[0].y
            val font = s.shapeType.substringAfter("font=").substringBefore(";").toFloatOrNull()
                ?: (s.strokeWidth * 6f)
            val textH = font
            val textLen = (s.shapeType.substringAfter("text:").substringBefore(";").length).coerceAtLeast(1)
            val textW = 0.6f * font * textLen
            val halfW = textW * 0.5f
            val halfH = textH * 0.5f
            val dx = maxOf(0f, abs(x - textX) - halfW)
            val dy = maxOf(0f, abs(y - textY) - halfH)
            sqrt(dx*dx + dy*dy)
        }
        s.shapeType == "circle" && s.points.size == 2 -> {
            val p1 = s.points[0]; val p2 = s.points[1]
            val cx = (p1.x + p2.x)/2f; val cy = (p1.y + p2.y)/2f
            val r = sqrt((p2.x - p1.x).pow(2) + (p2.y - p1.y).pow(2)) / 2f
            abs(sqrt((x - cx).pow(2) + (y - cy).pow(2)) - r)
        }
        s.shapeType == "rect" && s.points.size == 2 -> {
            val left = minOf(s.points[0].x, s.points[1].x)
            val right = maxOf(s.points[0].x, s.points[1].x)
            val top = minOf(s.points[0].y, s.points[1].y)
            val bottom = maxOf(s.points[0].y, s.points[1].y)
            // dist do rubova okvira (min od 4 ruba), 0 ako si na rubu; izvan “unutrašnjosti” vraća udaljenost do najbližeg ruba
            val dxLeft = abs(x - left)
            val dxRight = abs(x - right)
            val dyTop = abs(y - top)
            val dyBottom = abs(y - bottom)
            val insideHoriz = x in left..right
            val insideVert = y in top..bottom
            when {
                insideHoriz -> minOf(dyTop, dyBottom)
                insideVert -> minOf(dxLeft, dxRight)
                else -> {
                    // do najbližeg kuta
                    val cx = if (x < left) left else right
                    val cy = if (y < top) top else bottom
                    sqrt((x - cx)*(x - cx) + (y - cy)*(y - cy))
                }
            }
        }
        s.shapeType == "line" && s.points.size == 2 -> {
            val (x1, y1) = s.points[0]
            val (x2, y2) = s.points[1]
            pointToSegmentDistance(x, y, x1, y1, x2, y2)
        }
        else -> {
            val pts = s.points
            if (pts.size == 1) {
                val dx = pts[0].x - x; val dy = pts[0].y - y
                sqrt(dx*dx + dy*dy)
            } else {
                var minDist = Float.POSITIVE_INFINITY
                for (i in 0 until pts.size - 1) {
                    val d = pointToSegmentDistance(x, y, pts[i].x, pts[i].y, pts[i+1].x, pts[i+1].y)
                    if (d < minDist) minDist = d
                }
                minDist
            }
        }
    }
}

fun isTouchingStroke(stroke: DrawingStroke, x: Float, y: Float, scale: Float): Boolean {
    if (stroke.color == "#FFFFFF") return false

    val worldTol = screenTolToWorld(scale)
    val edgeTol = maxOf(worldTol, stroke.strokeWidth * 0.75f)

    val bboxMargin = maxOf(worldTol, stroke.strokeWidth)
    val aabb = AabbExpand(strokeAabb(stroke), bboxMargin)
    if (!inAabb(x, y, aabb)) return false


    return when {
        stroke.shapeType?.startsWith("text:") == true && stroke.points.size == 1 -> {
            val textX = stroke.points[0].x
            val textY = stroke.points[0].y
            val fontSizeWorld = stroke.shapeType.substringAfter("font=").substringBefore(";").toFloatOrNull()
                ?: (stroke.strokeWidth * 6f)

            // gruba aproksimacija širine/visine teksta u world prostoru
            val textH = fontSizeWorld
            val textLen = (stroke.shapeType.substringAfter("text:").substringBefore(";").length).coerceAtLeast(1)
            val textW = 0.6f * fontSizeWorld * textLen

            val halfW = textW * 0.5f
            val halfH = textH * 0.5f
            val tol = worldTol

            x in (textX - halfW - tol)..(textX + halfW + tol) &&
                    y in (textY - halfH - tol)..(textY + halfH + tol)

        }

        stroke.shapeType == "circle" && stroke.points.size == 2 -> {
            val p1 = stroke.points[0]
            val p2 = stroke.points[1]
            val centerX = (p1.x + p2.x) / 2
            val centerY = (p1.y + p2.y) / 2
            val radius = sqrt((p2.x - p1.x).pow(2) + (p2.y - p1.y).pow(2)) / 2
            val distance = sqrt((x - centerX).pow(2) + (y - centerY).pow(2))
            abs(distance - radius) < maxOf(edgeTol, stroke.strokeWidth * 0.5f)
        }

        stroke.shapeType == "rect" && stroke.points.size == 2 -> {
            val left = minOf(stroke.points[0].x, stroke.points[1].x)
            val right = maxOf(stroke.points[0].x, stroke.points[1].x)
            val top = minOf(stroke.points[0].y, stroke.points[1].y)
            val bottom = maxOf(stroke.points[0].y, stroke.points[1].y)
            val border = maxOf(edgeTol, stroke.strokeWidth * 0.5f)

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
            val margin = maxOf(edgeTol, stroke.strokeWidth * 0.5f)
            val minX = minOf(x1, x2) - margin
            val maxX = maxOf(x1, x2) + margin
            val minY = minOf(y1, y2) - margin
            val maxY = maxOf(y1, y2) + margin


            distance < maxOf(edgeTol, stroke.strokeWidth * 0.5f)
        }

        else -> {
            val pts = stroke.points
            if (pts.size == 1) {
                // točkasti hit
                val dx = pts[0].x - x
                val dy = pts[0].y - y
                sqrt(dx*dx + dy*dy) < maxOf(edgeTol, stroke.strokeWidth * 0.5f)
            } else {
                // udaljenost do najbližeg segmenta
                var minDist = Float.POSITIVE_INFINITY
                for (i in 0 until pts.size - 1) {
                    val d = pointToSegmentDistance(x, y, pts[i].x, pts[i].y, pts[i+1].x, pts[i+1].y)
                    if (d < minDist) minDist = d
                }
                minDist < maxOf(edgeTol, stroke.strokeWidth * 0.5f)
            }
        }

    }
}

private fun pointToSegmentDistance(px: Float, py: Float, x1: Float, y1: Float, x2: Float, y2: Float): Float {
    val dx = x2 - x1
    val dy = y2 - y1
    if (dx == 0f && dy == 0f) {
        val dxp = px - x1
        val dyp = py - y1
        return sqrt(dxp*dxp + dyp*dyp)
    }
    val t = ((px - x1)*dx + (py - y1)*dy) / (dx*dx + dy*dy)
    val clamped = t.coerceIn(0f, 1f)
    val projX = x1 + clamped*dx
    val projY = y1 + clamped*dy
    val ddx = px - projX
    val ddy = py - projY
    return sqrt(ddx*ddx + ddy*ddy)
}

private fun strokeAabb(s: DrawingStroke): Aabb {
    val xs = s.points.map { it.x }
    val ys = s.points.map { it.y }
    return Aabb(xs.minOrNull() ?: 0f, ys.minOrNull() ?: 0f, xs.maxOrNull() ?: 0f, ys.maxOrNull() ?: 0f)
}

private fun AabbExpand(a: Aabb, m: Float) = Aabb(a.minX - m, a.minY - m, a.maxX + m, a.maxY + m)

private fun inAabb(x: Float, y: Float, a: Aabb) =
    x >= a.minX && x <= a.maxX && y >= a.minY && y <= a.maxY



