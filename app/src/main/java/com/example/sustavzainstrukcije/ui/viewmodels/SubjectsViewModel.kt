package com.example.sustavzainstrukcije.ui.viewmodels

import androidx.lifecycle.ViewModel
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.tasks.await

class SubjectsViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()

    private val _subjects = MutableStateFlow<List<String>>(emptyList())
    val subjects: StateFlow<List<String>> = _subjects

    init {
        db.collection("subjects")
            .addSnapshotListener { snap, err ->
                if (err != null) return@addSnapshotListener
                val names = snap?.documents?.mapNotNull { it.getString("name") } ?: emptyList()
                _subjects.value = names.sorted()
            }
    }

    private fun normalize(s: String) = s.trim().lowercase()

    suspend fun addSubjectIfMissing(displayName: String): String {
        val name = displayName.trim()
        val normalized = normalize(name)
        val q = db.collection("subjects")
            .whereEqualTo("normalized", normalized)
            .get().await()
        if (q.isEmpty) {
            db.collection("subjects")
                .add(mapOf("name" to name, "normalized" to normalized))
                .await()
        }
        return name
    }

    suspend fun addSubjectsToUser(userId: String, vararg toAdd: String) {
        db.collection("users").document(userId)
            .set(
                mapOf("subjects" to com.google.firebase.firestore.FieldValue.arrayUnion(*toAdd)),
                SetOptions.merge()
            )
            .await()
    }

    suspend fun removeSubjectsFromUser(userId: String, vararg toRemove: String) {
        db.collection("users").document(userId)
            .update("subjects", com.google.firebase.firestore.FieldValue.arrayRemove(*toRemove))
            .await()
    }
}