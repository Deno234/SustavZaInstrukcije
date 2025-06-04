package com.example.sustavzainstrukcije.ui.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sustavzainstrukcije.ui.data.User // Tvoja User data klasa [2]
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.toObject
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch

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

    private val _specificUser = MutableStateFlow<User?>(null)
    val specificUser: StateFlow<User?> = _specificUser

    fun fetchUserByIdOnce(userId: String) {
        viewModelScope.launch {
            usersCollectionRef.document(userId).get()
                .addOnSuccessListener { documentSnapshot ->
                    if (documentSnapshot.exists()) {
                        _specificUser.value = documentSnapshot.toObject<User>()
                    } else {
                        _specificUser.value = null
                    }
                }
                .addOnFailureListener { exception ->
                    Log.e("UserViewModel", "Error getting user data", exception)
                    _specificUser.value = null
                }
        }
    }
}
