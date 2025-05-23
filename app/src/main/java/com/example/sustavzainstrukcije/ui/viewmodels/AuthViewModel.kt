package com.example.sustavzainstrukcije.ui.viewmodels

import android.app.Application
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.GetCredentialException
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.example.sustavzainstrukcije.BuildConfig
import com.example.sustavzainstrukcije.ui.data.User
import com.google.android.gms.common.api.ApiException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AuthViewModel(application: Application) : AndroidViewModel(application) {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    private val _errorMessage = MutableSharedFlow<String>()
    private val _loadingState = MutableStateFlow(false)
    private val _userData = MutableStateFlow<User?>(null)

    val credentialManager = CredentialManager.create(application)
    val userData: StateFlow<User?> = _userData.asStateFlow()

    var currentUserId: String? = null
    private lateinit var navController: NavController

    fun setNavController(controller: NavController) {
        navController = controller
    }

    fun initiateGoogleSignIn(launcher: (GetCredentialRequest) -> Unit) {
        _loadingState.value = true
        val serverClientId = BuildConfig.SERVER_CLIENT_ID

        val googleIdOption = GetGoogleIdOption.Builder()
            .setServerClientId(serverClientId)
            .setFilterByAuthorizedAccounts(false)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        launcher(request)
    }

    fun handleSignInResult(result: GetCredentialResponse) {
        _loadingState.value = true
        try {
            val credential = result.credential
            if (credential is CustomCredential && credential.type ==
                GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                val idToken = googleIdTokenCredential.idToken

                val firebaseCredential = GoogleAuthProvider.getCredential(idToken, null)
                auth.signInWithCredential(firebaseCredential)
                    .addOnCompleteListener { task ->
                        _loadingState.value = false
                        if (task.isSuccessful) {
                            checkUserInFirestore(auth.currentUser?.uid)
                        } else {
                            handleAuthError(task.exception)
                        }
                    }
            } else {
                _loadingState.value = false
                viewModelScope.launch {
                    _errorMessage.emit("Invalid credential type")
                }
            }
        } catch (e: GoogleIdTokenParsingException) {
            _loadingState.value = false
            handleAuthError(e)
        }
    }

    fun handleSignInError(e: GetCredentialException) {
        _loadingState.value = false
        handleAuthError(e)
    }

    fun checkUserInFirestore(userId: String?) {
        currentUserId = userId
        if (userId == null) {
            viewModelScope.launch {
                _errorMessage.emit("User authentication failed")
            }
            return
        }

        db.collection("users").document(userId).get()
            .addOnSuccessListener { document ->
                viewModelScope.launch {
                    if (document.exists()) {
                        _userData.value = document.toObject(User::class.java)?.copy(id = userId)
                        navigateTo("home")
                    } else {
                        navigateTo("googleRegistration")
                    }
                }
            }
            .addOnFailureListener { exception ->
                viewModelScope.launch {
                    _errorMessage.emit("Database error: ${exception.localizedMessage}")
                }
            }
    }

    private fun handleAuthError(exception: Exception?) {
        val errorMessage = when (exception) {
            is ApiException -> "Google sign-in failed: ${exception.statusCode}"
            else -> "Authentication failed: ${exception?.localizedMessage}"
        }
        Log.e(TAG, errorMessage, exception)
        viewModelScope.launch {
            _errorMessage.emit(errorMessage)
        }
    }

    private fun navigateTo(destination: String) {
        navController.navigate(destination) {
            if (destination == "home") {
                popUpTo("login") { inclusive = true }
            }
        }
    }

    fun fetchCurrentUserData() {
        val currentUser = auth.currentUser
        currentUserId = currentUser?.uid

        auth.currentUser?.uid?.let { userId ->
            db.collection("users").document(userId).get()
                .addOnSuccessListener { document ->
                    _userData.value =
                        document.toObject(User::class.java)?.copy(id = userId)
                }
        }
    }

    fun updateInstructorProfile(name: String, subjects: List<String>, availableHours: Map<String, List<String>>) {
        val currentUserId = currentUserId ?: return
        val updates = mapOf(
            "name" to name,
            "subjects" to subjects,
            "availableHours" to availableHours
        )
        db.collection("users").document(currentUserId).update(updates)
            .addOnSuccessListener {
                fetchCurrentUserData()
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error updating instructor profile", e)
            }
    }

    companion object {
        private const val TAG = "AuthViewModel"
    }
}

