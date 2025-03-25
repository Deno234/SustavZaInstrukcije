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
    private var snapshotListener: ListenerRegistration? = null

    val instructors: StateFlow<List<User>> = _instructors.asStateFlow()
    val loadingState: StateFlow<Boolean> = _loadingState.asStateFlow()

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

                snapshot?.let {
                    _instructors.value = it.documents.mapNotNull { doc ->
                        try {
                            doc.toObject(User::class.java)?.copy(id = doc.id)
                        } catch (e: Exception) {
                            Log.e("InstructorsViewModel", "Error parsing document ${doc.id}", e)
                            null
                        }
                    }
                }
            }
    }

    override fun onCleared() {
        super.onCleared()
        snapshotListener?.remove()
    }

}