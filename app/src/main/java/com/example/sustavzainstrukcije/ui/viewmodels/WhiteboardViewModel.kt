package com.example.sustavzainstrukcije.ui.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import com.example.sustavzainstrukcije.ui.data.DrawingStroke
import com.example.sustavzainstrukcije.ui.data.EraseMode
import com.example.sustavzainstrukcije.ui.data.Point
import com.example.sustavzainstrukcije.ui.data.ToolMode
import com.example.sustavzainstrukcije.ui.data.WhiteboardPage
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.database.database
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID
import kotlin.math.hypot

class WhiteboardViewModel : ViewModel() {
    private val db = Firebase.database
    private val firestore = Firebase.firestore
    private val auth = Firebase.auth

    private val _currentPage = MutableStateFlow<WhiteboardPage?>(null)
    val currentPage: StateFlow<WhiteboardPage?> = _currentPage.asStateFlow()

    private val _strokes = MutableStateFlow<List<DrawingStroke>>(emptyList())
    val strokes: StateFlow<List<DrawingStroke>> = _strokes.asStateFlow()

    private var strokesListener: ListenerRegistration? = null
    private var initializedSessions = mutableSetOf<String>()

    private var pagesListener: ListenerRegistration? = null

    private val _allPages = MutableStateFlow<List<WhiteboardPage>>(emptyList())
    val allPages: StateFlow<List<WhiteboardPage>> = _allPages.asStateFlow()

    private val _currentPageIndex = MutableStateFlow(0)
    val currentPageIndex: StateFlow<Int> = _currentPageIndex.asStateFlow()

    private val _isEraserActive = MutableStateFlow(false)
    val isEraserActive: StateFlow<Boolean> = _isEraserActive.asStateFlow()

    private val _eraseMode = MutableStateFlow(EraseMode.COLOR) // "color" ili "stroke"
    val eraseMode: StateFlow<EraseMode> = _eraseMode.asStateFlow()

    private val _eraserSize = MutableStateFlow(32f)
    val eraserSize: StateFlow<Float> = _eraserSize.asStateFlow()
    fun setEraserSize(size: Float) { _eraserSize.value = size }

    private val undoStacks = mutableMapOf<String, ArrayDeque<StrokeAction>>()
    private val redoStacks = mutableMapOf<String, ArrayDeque<StrokeAction>>()

    private fun stackKey(userId: String, pageId: String) = "$userId:$pageId"

    private val _toolMode = MutableStateFlow(ToolMode.DRAW)
    val toolMode: StateFlow<ToolMode> = _toolMode.asStateFlow()

    fun setToolMode(mode: ToolMode) {
        _toolMode.value = mode
    }

    fun initializeWhiteboard(sessionId: String) {
        if (initializedSessions.contains(sessionId)) {
            Log.d("WhiteboardViewModel", "Already initialized for session: $sessionId")
            return
        }

        initializedSessions.add(sessionId)
        Log.d("WhiteboardViewModel", "Initializing whiteboard for session: $sessionId")

        listenToAllPages(sessionId)
    }

    private fun listenToAllPages(sessionId: String) {
        pagesListener?.remove()

        pagesListener = firestore.collection("whiteboard_pages")
            .whereEqualTo("sessionId", sessionId)
            .orderBy("pageNumber")
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.e("WhiteboardViewModel", "Error listening to pages", e)
                    return@addSnapshotListener
                }

                val pagesList = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(WhiteboardPage::class.java)?.copy(id = doc.id)
                }?.sortedBy { it.pageNumber } ?: emptyList()

                Log.d("WhiteboardViewModel", "Loaded ${pagesList.size} pages")
                _allPages.value = pagesList

                // Ako nema stranica, kreiraj prvu
                if (pagesList.isEmpty()) {
                    createNewPage(sessionId)
                } else {
                    // Postavi trenutnu stranicu na prvu ako nije postavljena
                    if (_currentPage.value == null) {
                        setCurrentPage(0)
                    }
                }
            }
    }

    fun setCurrentPage(pageIndex: Int) {
        val pages = _allPages.value
        if (pageIndex >= 0 && pageIndex < pages.size) {
            _currentPageIndex.value = pageIndex
            val page = pages[pageIndex]
            _currentPage.value = page

            // Prestani slušati prethodne stroke-ove
            strokesListener?.remove()
            listenToStrokes(page.id)

            Log.d("WhiteboardViewModel", "Switched to page ${page.pageNumber}")
        }
    }

    fun navigateToNextPage() {
        val currentIndex = _currentPageIndex.value
        val totalPages = _allPages.value.size

        if (currentIndex < totalPages - 1) {
            setCurrentPage(currentIndex + 1)
        }
    }

    fun navigateToPreviousPage() {
        val currentIndex = _currentPageIndex.value
        if (currentIndex > 0) {
            setCurrentPage(currentIndex - 1)
        }
    }

    private fun listenToStrokes(pageId: String) {
        strokesListener?.remove()
        Log.d("WhiteboardViewModel", "Setting up stroke listener for pageId: $pageId")

        strokesListener = firestore.collection("drawing_strokes")
            .whereEqualTo("pageId", pageId)
            // .orderBy("timestamp")
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.e("WhiteboardViewModel", "Error listening to strokes: ${e.message}", e)
                    return@addSnapshotListener
                }

                Log.d("WhiteboardViewModel", "Snapshot received: ${snapshot?.size()} documents")

                val strokesList = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        val data = doc.data ?: return@mapNotNull null
                        Log.d("WhiteboardViewModel", "Processing stroke: ${doc.id}")

                        val pointsData = data["points"] as? List<Map<String, Any>> ?: emptyList()
                        val points = pointsData.map { pointMap ->
                            Point(
                                x = (pointMap["x"] as? Number)?.toFloat() ?: 0f,
                                y = (pointMap["y"] as? Number)?.toFloat() ?: 0f
                            )
                        }

                        DrawingStroke(
                            id = doc.id,
                            userId = data["userId"] as? String ?: "",
                            points = points,
                            color = data["color"] as? String ?: "#000000",
                            strokeWidth = (data["strokeWidth"] as? Number)?.toFloat() ?: 5f,
                            timestamp = (data["timestamp"] as? Number)?.toLong() ?: 0L,
                            shapeType = data["shapeType"] as? String
                        )
                    } catch (e: Exception) {
                        Log.e("WhiteboardViewModel", "Error parsing stroke: ${e.message}", e)
                        null
                    }
                }?.sortedBy { it.timestamp } ?: emptyList()

                Log.d("WhiteboardViewModel", "Loaded ${strokesList.size} strokes for page: $pageId")
                _strokes.value = strokesList
            }
    }




    fun addStroke(points: List<Point>, color: String, strokeWidth: Float, shapeType: String? = null) {
        val currentUserId = auth.currentUser?.uid ?: return
        val pageId = _currentPage.value?.id ?: return
        val key = stackKey(currentUserId, pageId)

        val strokeId = UUID.randomUUID().toString()
        val stroke = DrawingStroke(
            id = strokeId,
            userId = currentUserId,
            points = points,
            color = color,
            strokeWidth = strokeWidth,
            shapeType = shapeType
        )

        val strokeData = mapOf(
            "id" to strokeId,
            "userId" to currentUserId,
            "points" to points.map { mapOf("x" to it.x, "y" to it.y) },
            "color" to color,
            "strokeWidth" to strokeWidth,
            "timestamp" to System.currentTimeMillis(),
            "pageId" to pageId,
            "shapeType" to shapeType
        )

        undoStacks.getOrPut(key) { ArrayDeque() }.addLast(StrokeAction.Add(stroke))
        redoStacks.getOrPut(key) { ArrayDeque() }.clear()


        firestore.collection("drawing_strokes").document(strokeId)
            .set(strokeData)
            .addOnSuccessListener {
                Log.d("WhiteboardViewModel", "Stroke added successfully: $strokeId")
            }
            .addOnFailureListener { e ->
                Log.e("WhiteboardViewModel", "Error adding stroke", e)
            }
    }


    fun createNewPage() {
        val sessionId = _currentPage.value?.sessionId ?: return
        val currentUserId = auth.currentUser?.uid ?: return
        val pages = _allPages.value
        val nextPageNumber = (pages.maxOfOrNull { it.pageNumber } ?: 0) + 1

        val pageId = UUID.randomUUID().toString()
        val newPage = WhiteboardPage(
            id = pageId,
            sessionId = sessionId,
            pageNumber = nextPageNumber,
            createdBy = currentUserId
        )

        firestore.collection("whiteboard_pages").document(pageId)
            .set(newPage)
            .addOnSuccessListener {
                Log.d("WhiteboardViewModel", "New page created: $pageId")
                // Ne postavlja se trenutna stranicu ovdje - to će se dogoditi automatski kroz listener
            }
            .addOnFailureListener { e ->
                Log.e("WhiteboardViewModel", "Error creating new page", e)
            }
    }

    private fun createNewPage(sessionId: String) {
        val currentUserId = auth.currentUser?.uid ?: return
        val pageId = UUID.randomUUID().toString()
        val page = WhiteboardPage(
            id = pageId,
            sessionId = sessionId,
            pageNumber = 1,
            createdBy = currentUserId
        )

        firestore.collection("whiteboard_pages").document(pageId)
            .set(page)
            .addOnSuccessListener {
                Log.d("WhiteboardViewModel", "First page created: $pageId")
            }
    }

    fun toggleEraser() {
        _isEraserActive.value = !_isEraserActive.value
        if (_isEraserActive.value) _eraseMode.value = EraseMode.COLOR
        else _toolMode.value = ToolMode.DRAW
    }

    fun setEraseMode(mode: EraseMode) {
        _eraseMode.value = mode
    }

    fun removeStroke(strokeId: String) {
        val currentUserId = auth.currentUser?.uid ?: return
        val pageId = _currentPage.value?.id ?: return
        val key = stackKey(currentUserId, pageId)

        val stroke = _strokes.value.find { it.id == strokeId } ?: return

        undoStacks.getOrPut(key) { ArrayDeque() }.addLast(StrokeAction.Remove(stroke))
        redoStacks.getOrPut(key) { ArrayDeque() }.clear()

        _strokes.value = _strokes.value.filter { it.id != strokeId }
        firestore.collection("drawing_strokes").document(strokeId).delete()
    }


    fun undo() {
        val currentUserId = auth.currentUser?.uid ?: return
        val pageId = _currentPage.value?.id ?: return
        val key = stackKey(currentUserId, pageId)

        val undoStack = undoStacks[key] ?: return
        if (undoStack.isEmpty()) return

        val action = undoStack.removeLast()
        redoStacks.getOrPut(key) { ArrayDeque() }.addLast(action)

        when (action) {
            is StrokeAction.Add -> {
                // Undo ADD => REMOVE stroke
                firestore.collection("drawing_strokes").document(action.stroke.id).delete()
            }
            is StrokeAction.Remove -> {
                // Undo REMOVE => RE-ADD stroke
                val stroke = action.stroke
                val strokeData = stroke.toMap() + mapOf("id" to stroke.id, "pageId" to pageId)
                firestore.collection("drawing_strokes").document(stroke.id).set(strokeData)
            }
        }
    }


    fun redo() {
        val currentUserId = auth.currentUser?.uid ?: return
        val pageId = _currentPage.value?.id ?: return
        val key = stackKey(currentUserId, pageId)

        val redoStack = redoStacks[key] ?: return
        if (redoStack.isEmpty()) return

        val action = redoStack.removeLast()
        undoStacks.getOrPut(key) { ArrayDeque() }.addLast(action)

        when (action) {
            is StrokeAction.Add -> {
                val stroke = action.stroke
                val strokeData = stroke.toMap() + mapOf("id" to stroke.id, "pageId" to pageId)
                firestore.collection("drawing_strokes").document(stroke.id).set(strokeData)
            }
            is StrokeAction.Remove -> {
                firestore.collection("drawing_strokes").document(action.stroke.id).delete()
            }
        }
    }



    fun findStrokeAtPosition(x: Float, y: Float): DrawingStroke? {
        return _strokes.value.firstOrNull { stroke ->
            if (stroke.color == "#FFFFFF") return@firstOrNull false
            stroke.points.any { point ->
                val distanceX = x - point.x
                val distanceY = y - point.y
                hypot(distanceX, distanceY) < stroke.strokeWidth * 2
            }
        }
    }


    override fun onCleared() {
        super.onCleared()
        strokesListener?.remove()
        pagesListener?.remove()
        initializedSessions.clear()
    }
}

// Extension funkcija za konverziju u Map
fun DrawingStroke.toMap(): Map<String, Any> {
    val map = mutableMapOf(
        "userId" to userId,
        "points" to points.map { mapOf("x" to it.x, "y" to it.y) },
        "color" to color,
        "strokeWidth" to strokeWidth,
        "timestamp" to timestamp
    )

    shapeType?.let { map["shapeType"] = it }

    return map
}



sealed class StrokeAction {
    data class Add(val stroke: DrawingStroke) : StrokeAction()
    data class Remove(val stroke: DrawingStroke) : StrokeAction()
}
