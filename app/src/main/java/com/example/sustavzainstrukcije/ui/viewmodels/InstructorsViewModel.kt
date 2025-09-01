package com.example.sustavzainstrukcije.ui.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import com.example.sustavzainstrukcije.ui.data.RatingComment
import com.example.sustavzainstrukcije.ui.data.User
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow


class InstructorsViewModel (
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) : ViewModel() {

    private val _instructors = MutableStateFlow<List<User>>(emptyList())
    private val _loadingState = MutableStateFlow(false)
    private val _subjects = MutableStateFlow<List<String>>(emptyList())
    private val _filteredInstructors = MutableStateFlow<Map<String, List<User>>>(emptyMap())
    private val _checkedInstructor = MutableStateFlow<User?>(null)

    private var snapshotListener: ListenerRegistration? = null

    private val _ratingsByInstructorAndSubject =
        MutableStateFlow<Map<Pair<String, String>, Pair<Double, Int>>>(emptyMap())
    val ratingsByInstructorAndSubject = _ratingsByInstructorAndSubject.asStateFlow()

    val loadingState: StateFlow<Boolean> = _loadingState.asStateFlow()
    val subjects: StateFlow<List<String>> = _subjects.asStateFlow()
    val filteredInstructors: StateFlow<Map<String, List<User>>> = _filteredInstructors.asStateFlow()
    val checkedInstructor: StateFlow<User?> = _checkedInstructor.asStateFlow()

    private val _ratingsByInstructor =
        MutableStateFlow<Map<String, Pair<Double, Int>>>(emptyMap())
    val ratingsByInstructor = _ratingsByInstructor.asStateFlow()

    private val _commentsByInstructorAndSubject =
        MutableStateFlow<Map<Pair<String, String>, List<RatingComment>>>(emptyMap())
    val commentsByInstructorAndSubject = _commentsByInstructorAndSubject.asStateFlow()

    private val userNameCache = mutableMapOf<String, String>()

    init {
        setupRealTimeUpdates()
    }

    private fun setupRealTimeUpdates() {
        _loadingState.value = true
        snapshotListener = firestore.collection("users")
            .whereEqualTo("role", "instructor")
            .addSnapshotListener { snapshot, error ->
                _loadingState.value = false
                if (error != null) {
                    Log.e("InstructorsViewModel", "Listen failed", error)
                    return@addSnapshotListener
                }

                snapshot?.let { snap ->
                    val instructorList = snap.documents.mapNotNull { doc ->
                        try {
                            doc.toObject(User::class.java)?.copy(id = doc.id)
                        } catch (e: Exception) {
                            Log.e("InstructorsViewModel", "Error parsing document ${doc.id}", e)
                            null
                        }
                    }

                    _instructors.value = instructorList
                    _subjects.value = instructorList.flatMap { it.subjects }.distinct().sorted()
                    filterInstructors("", null)
                }
            }
    }

    fun filterInstructors(searchQuery: String, selectedSubject: String?) {
        val groupedInstructors = _subjects.value.associateWith { subject ->
            _instructors.value.filter { it.subjects.contains(subject) }
        }

        _filteredInstructors.value = groupedInstructors
            .filterKeys { subject -> selectedSubject == null || subject == selectedSubject }
            .mapValues { (_, instructorList) ->
                instructorList.filter { instructor ->
                    searchQuery.isEmpty() || instructor.name.contains(searchQuery, ignoreCase = true)
                }
            }
            .filterValues { it.isNotEmpty()}
    }

    fun fetchCheckedInstructor(instructorId: String) {
        snapshotListener = firestore.collection("users")
            .document(instructorId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("InstructorsViewModel", "Listen failed", error)
                    _checkedInstructor.value = null
                    return@addSnapshotListener
                }

                if (snapshot != null && snapshot.exists()) {
                    try {
                        val instructor = snapshot.toObject(User::class.java)?.copy(id = snapshot.id)
                        _checkedInstructor.value = instructor
                    } catch (e: Exception) {
                        Log.e("InstructorsViewModel", "Error parsing document ${snapshot.id}", e)
                        _checkedInstructor.value = null
                    }
                }
                else {
                    _checkedInstructor.value = null
                }
            }
    }

    fun addOrUpdateRating(
        instructorId: String,
        studentId: String,
        subject: String,
        rating: Int,
        comment: String,
        studentName: String? = null
    ) {
        val docId = "${studentId}_${instructorId}_$subject"
        val data = mapOf(
            "instructorId" to instructorId,
            "studentId" to studentId,
            "subject" to subject,
            "rating" to rating,
            "comment" to comment,
            "studentName" to studentName
        )
        firestore.collection("ratings").document(docId).set(data)
    }




    fun listenToInstructorRatingsForSubject(instructorId: String, subject: String) {
        firestore.collection("ratings")
            .whereEqualTo("instructorId", instructorId)
            .whereEqualTo("subject", subject)
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                snapshot?.let { snap ->
                    val key = instructorId to subject

                    val ratings = snap.documents.mapNotNull { d -> d.getLong("rating")?.toInt() }
                    val avg = if (ratings.isNotEmpty()) ratings.average() else 0.0
                    _ratingsByInstructorAndSubject.value += (key to (avg to ratings.size))

                    val rawComments = snap.documents.mapNotNull { d ->
                        val text = d.getString("comment") ?: return@mapNotNull null
                        val studentId = d.getString("studentId") ?: return@mapNotNull null
                        val ratingVal = d.getLong("rating")?.toInt() ?: 0
                        val studentName = d.getString("studentName")
                        val subjectNew = d.getString("subject") ?: ""
                        RatingComment(studentId, studentName, text, ratingVal, subjectNew)
                    }

                    val missingIds = rawComments
                        .filter { it.studentName.isNullOrBlank() }
                        .map { it.studentId }
                        .toSet()

                    if (missingIds.isEmpty()) {
                        _commentsByInstructorAndSubject.value += (key to rawComments)
                        return@addSnapshotListener
                    }

                    fetchUserNames(missingIds) { nameMap ->
                        val enriched = rawComments.map { c ->
                            if (c.studentName.isNullOrBlank()) c.copy(studentName = nameMap[c.studentId])
                            else c
                        }
                        _commentsByInstructorAndSubject.value += (key to enriched)

                        snap.documents.forEach { d ->
                            val sid = d.getString("studentId") ?: return@forEach
                            val sn = d.getString("studentName")
                            if (sn.isNullOrBlank()) {
                                nameMap[sid]?.let { resolved ->
                                    d.reference.update("studentName", resolved)
                                        .addOnFailureListener { e ->
                                            Log.w("InstructorsViewModel", "Backfill studentName failed for ${d.id}", e)
                                        }
                                }
                            }
                        }
                    }
                }
            }
    }



    override fun onCleared() {
        super.onCleared()
        snapshotListener?.remove()
    }

    fun listenToAllInstructorRatingsForSubject(subject: String) {
        firestore.collection("ratings")
            .whereEqualTo("subject", subject)
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener

                val ratingsGrouped = snapshot?.documents
                    ?.mapNotNull { doc ->
                        val instructorId = doc.getString("instructorId") ?: return@mapNotNull null
                        val rating = doc.getLong("rating")?.toInt() ?: return@mapNotNull null
                        instructorId to rating
                    }
                    ?.groupBy { it.first }
                    ?.mapValues { (_, ratings) ->
                        val values = ratings.map { it.second }
                        values.average() to values.size
                    }
                    ?: emptyMap()

                val newMap = ratingsGrouped.mapKeys { (instructorId, _) -> instructorId to subject }
                _ratingsByInstructorAndSubject.value += newMap
            }
    }

    fun listenToAllInstructorRatings() {
        firestore.collection("ratings")
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener

                val ratingsGrouped = snapshot?.documents
                    ?.mapNotNull { doc ->
                        val instructorId = doc.getString("instructorId") ?: return@mapNotNull null
                        val rating = doc.getLong("rating")?.toInt() ?: return@mapNotNull null
                        instructorId to rating
                    }
                    ?.groupBy { it.first }
                    ?.mapValues { (_, ratings) ->
                        val values = ratings.map { it.second }
                        values.average() to values.size
                    }
                    ?: emptyMap()

                _ratingsByInstructor.value = ratingsGrouped
            }
    }

    private fun fetchUserNames(
        ids: Set<String>,
        onComplete: (Map<String, String>) -> Unit
    ) {
        if (ids.isEmpty()) {
            onComplete(emptyMap())
            return
        }

        val uncached = ids.filterNot { userNameCache.containsKey(it) }
        if (uncached.isEmpty()) {
            onComplete(ids.associateWith { userNameCache[it].orEmpty() })
            return
        }

        val chunks = uncached.chunked(10)
        val result = mutableMapOf<String, String>()
        var completed = 0

        chunks.forEach { chunk ->
            firestore.collection("users")
                .whereIn(FieldPath.documentId(), chunk)
                .get()
                .addOnSuccessListener { snap ->
                    snap.documents.forEach { doc ->
                        val name = doc.getString("name")
                        if (!name.isNullOrBlank()) {
                            userNameCache[doc.id] = name
                            result[doc.id] = name
                        }
                    }
                }
                .addOnCompleteListener {
                    completed++
                    if (completed == chunks.size) {
                        val finalMap = ids.mapNotNull { id ->
                            val nm = userNameCache[id] ?: result[id]
                            nm?.let { id to it }
                        }.toMap()
                        onComplete(finalMap)
                    }
                }
        }
    }

    fun listenToAllInstructorComments(instructorId: String) {
        firestore.collection("ratings")
            .whereEqualTo("instructorId", instructorId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener

                snapshot?.let { snap ->
                    val groupedBySubject = snap.documents.groupBy { it.getString("subject") ?: "" }

                    val newMap = groupedBySubject.mapValues { (_, docs) ->
                        docs.mapNotNull { d ->
                            val text = d.getString("comment") ?: return@mapNotNull null
                            val studentId = d.getString("studentId") ?: return@mapNotNull null
                            val ratingVal = d.getLong("rating")?.toInt() ?: 0
                            val studentName = d.getString("studentName")
                            val subject = d.getString("subject") ?: ""
                            RatingComment(studentId, studentName, text, ratingVal, subject)

                        }
                    }

                    _commentsByInstructorAndSubject.value += newMap.mapKeys { (subject, list) ->
                        instructorId to subject
                    }
                }
            }
    }



}