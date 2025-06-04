package com.example.sustavzainstrukcije.ui.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import com.example.sustavzainstrukcije.ui.data.DrawingStroke
import com.example.sustavzainstrukcije.ui.data.Point
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

    fun initializeWhiteboard(sessionId: String) {
        if (initializedSessions.contains(sessionId)) {
            Log.d("WhiteboardViewModel", "Already initialized for session: $sessionId")
            return
        }

        initializedSessions.add(sessionId)
        Log.d("WhiteboardViewModel", "Initializing whiteboard for session: $sessionId")

        firestore.collection("whiteboard_pages")
            .whereEqualTo("sessionId", sessionId)
            .orderBy("pageNumber", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .limit(1)
            .get()
            .addOnSuccessListener { snapshot ->
                Log.d("WhiteboardViewModel", "Query result: ${snapshot.size()} documents found")

                if (snapshot.isEmpty) {
                    Log.d("WhiteboardViewModel", "No pages found at all, creating new one")
                    createNewPage(sessionId)
                } else {
                    val pageDoc = snapshot.documents.first()
                    val page = pageDoc.toObject(WhiteboardPage::class.java)?.copy(id = pageDoc.id)

                    if (page != null) {
                        // Ako stranica nije aktivna, ponovno je aktiviraj
                        if (page.isActive != true) {
                            firestore.collection("whiteboard_pages").document(page.id)
                                .update("isActive", true)
                                .addOnSuccessListener {
                                    Log.d("WhiteboardViewModel", "Reactivated existing page: ${page.id}")
                                }
                        }

                        _currentPage.value = page
                        Log.d("WhiteboardViewModel", "Using page: ${page.id}")
                        listenToStrokes(page.id)
                    } else {
                        Log.e("WhiteboardViewModel", "Page data is null, creating new page")
                        createNewPage(sessionId)
                    }
                }
            }
            .addOnFailureListener { e ->
                Log.e("WhiteboardViewModel", "Error initializing whiteboard", e)
                initializedSessions.remove(sessionId)
            }
    }





    private fun createNewPage(sessionId: String) {
        val pageId = UUID.randomUUID().toString()
        val page = WhiteboardPage(
            id = pageId,
            sessionId = sessionId,
            pageNumber = 1
        )

        firestore.collection("whiteboard_pages").document(pageId)
            .set(page)
            .addOnSuccessListener {
                _currentPage.value = page
                listenToStrokes(pageId)
            }
    }

    private fun listenToStrokes(pageId: String) {
        strokesListener?.remove()
        Log.d("WhiteboardViewModel", "Setting up stroke listener for pageId: $pageId")

        strokesListener = firestore.collection("drawing_strokes")
            .whereEqualTo("pageId", pageId)
            // UKLONI orderBy privremeno da testiraš
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
                            timestamp = (data["timestamp"] as? Number)?.toLong() ?: 0L
                        )
                    } catch (e: Exception) {
                        Log.e("WhiteboardViewModel", "Error parsing stroke: ${e.message}", e)
                        null
                    }
                }?.sortedBy { it.timestamp } ?: emptyList() // Sortiraj u kodu

                Log.d("WhiteboardViewModel", "Loaded ${strokesList.size} strokes for page: $pageId")
                _strokes.value = strokesList
            }
    }




    fun addStroke(points: List<Point>, color: String, strokeWidth: Float) {
        val currentUserId = auth.currentUser?.uid ?: return
        val pageId = _currentPage.value?.id ?: return

        val strokeId = UUID.randomUUID().toString()
        val stroke = DrawingStroke(
            id = strokeId,
            userId = currentUserId,
            points = points,
            color = color,
            strokeWidth = strokeWidth
        )

        val strokeData = mapOf(
            "id" to strokeId,
            "userId" to currentUserId,
            "points" to points.map { mapOf("x" to it.x, "y" to it.y) },
            "color" to color,
            "strokeWidth" to strokeWidth,
            "timestamp" to System.currentTimeMillis(),
            "pageId" to pageId // Dodaj pageId direktno
        )

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
        val currentPageNumber = _currentPage.value?.pageNumber ?: 0

        // Deaktiviraj trenutnu stranicu
        _currentPage.value?.let { page ->
            firestore.collection("whiteboard_pages").document(page.id)
                .update("isActive", false)
        }

        // Kreiraj novu stranicu
        val pageId = UUID.randomUUID().toString()
        val newPage = WhiteboardPage(
            id = pageId,
            sessionId = sessionId,
            pageNumber = currentPageNumber + 1
        )

        firestore.collection("whiteboard_pages").document(pageId)
            .set(newPage)
            .addOnSuccessListener {
                _currentPage.value = newPage
                listenToStrokes(pageId)
            }
    }

    override fun onCleared() {
        super.onCleared()
        strokesListener?.remove()
        initializedSessions.clear()
    }
}

// Extension funkcija za konverziju u Map
fun DrawingStroke.toMap(): Map<String, Any> {
    return mapOf(
        "userId" to userId,
        "points" to points.map { mapOf("x" to it.x, "y" to it.y) },
        "color" to color,
        "strokeWidth" to strokeWidth,
        "timestamp" to timestamp
    )
}
