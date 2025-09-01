package com.example.sustavzainstrukcije.ui.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import com.example.sustavzainstrukcije.ui.data.User
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
        comment: String
    ) {
        val docId = "${studentId}_${instructorId}_$subject"
        val data = mapOf(
            "instructorId" to instructorId,
            "studentId" to studentId,
            "subject" to subject,
            "rating" to rating,
            "comment" to comment
        )
        firestore.collection("ratings").document(docId).set(data)
    }


    fun listenToInstructorRatingsForSubject(instructorId: String, subject: String) {
        firestore.collection("ratings")
            .whereEqualTo("instructorId", instructorId)
            .whereEqualTo("subject", subject)
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                snapshot?.let {
                    val ratings = it.documents.mapNotNull { d -> d.getLong("rating")?.toInt() }
                    val avg = if (ratings.isNotEmpty()) ratings.average() else 0.0
                    val key = instructorId to subject
                    _ratingsByInstructorAndSubject.value += (key to (avg to ratings.size))
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


}