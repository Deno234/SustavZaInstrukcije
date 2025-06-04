package com.example.sustavzainstrukcije.ui.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sustavzainstrukcije.ui.data.User // Tvoja User data klasa [2]
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.toObject
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.tasks.await

class UserViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()
    private val usersCollectionRef = db.collection("users")

    /**
     * Dohvaća korisničke podatke za zadani userId kao Flow iz Cloud Firestore-a.
     * Flow će emitirati novu User vrijednost svaki put kad dođe do promjene u dokumentu korisnika.
     */
    fun getUserById(userId: String): Flow<User?> {
        // callbackFlow se koristi za pretvaranje Firestore snapshot listenera u Flow
        return callbackFlow {
            // Referenca na specifični dokument korisnika u kolekciji 'users'
            val userDocumentRef = usersCollectionRef.document(userId)

            // addSnapshotListener registrira listener koji se poziva na svaku promjenu podataka
            val listenerRegistration = userDocumentRef.addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w("UserViewModel", "Listen failed.", error)
                    close(error)
                    return@addSnapshotListener
                }

                if (snapshot != null && snapshot.exists()) {
                    val user = snapshot.toObject<User>()
                    trySend(user) // Emitiraj dohvaćenog korisnika
                } else {
                    Log.d("UserViewModel", "Current data: null")
                    trySend(null) // Korisnik ne postoji ili je dokument prazan
                }
            }

            // awaitClose se poziva kada se Flow zatvori (npr. kada CoroutineScope prestane s radom)
            awaitClose {
                listenerRegistration.remove()
            }
        }
    }

    fun getStudentsForInstructor(): Flow<List<User>> = callbackFlow {
        val currentUserId = Firebase.auth.currentUser?.uid ?: return@callbackFlow

        // Dohvati sve studente (jednostavniji pristup)
        db.collection("users")
            .whereEqualTo("role", "student")
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.e("UserViewModel", "Error fetching students", e)
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                val students = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(User::class.java)?.copy(id = doc.id)
                } ?: emptyList()

                trySend(students)
            }

        awaitClose { }
    }.shareIn(viewModelScope, SharingStarted.WhileSubscribed(), 1)





    private val _specificUser = MutableStateFlow<User?>(null)
    val specificUser: StateFlow<User?> = _specificUser

    fun fetchUserByIdOnce(userId: String) {
        viewModelScope.launch {
            try {
                val doc = db.collection("users").document(userId).get().await()
                if (doc.exists()) {
                    _specificUser.value = doc.toObject(User::class.java)?.copy(id = doc.id)
                } else {
                    _specificUser.value = null
                }
            } catch (e: Exception) {
                Log.e("UserViewModel", "Error fetching user", e)
                _specificUser.value = null
            }
        }
    }
}
