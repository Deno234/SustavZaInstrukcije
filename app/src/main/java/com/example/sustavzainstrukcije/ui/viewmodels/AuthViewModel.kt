package com.example.sustavzainstrukcije.ui.viewmodels

import android.app.Activity.RESULT_OK
import android.app.Application
import android.util.Log
import androidx.activity.result.ActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.example.sustavzainstrukcije.BuildConfig
import com.google.android.gms.auth.api.identity.BeginSignInRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

class AuthViewModel(application: Application) : AndroidViewModel(application) {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private val oneTapClient = Identity.getSignInClient(application)

    private val _errorMessage = MutableSharedFlow<String>()
    private val _loadingState = MutableStateFlow(false)

    var currentUserId: String? = null
    private lateinit var navController: NavController

    fun setNavController(controller: NavController) {
        navController = controller
    }

    fun initiateGoogleSignIn(launcher: (IntentSenderRequest) -> Unit) {
        _loadingState.value = true
        val serverClientId = BuildConfig.SERVER_CLIENT_ID

        val signInRequest = BeginSignInRequest.Builder()
            .setGoogleIdTokenRequestOptions(
                BeginSignInRequest.GoogleIdTokenRequestOptions.builder()
                    .setSupported(true)
                    .setServerClientId(serverClientId)
                    .setFilterByAuthorizedAccounts(false)
                    .build()
            )
            .build()

        oneTapClient.beginSignIn(signInRequest)
            .addOnSuccessListener { result ->
                launcher(IntentSenderRequest.Builder(result.pendingIntent.intentSender).build())
                _loadingState.value = false
            }
            .addOnFailureListener { e ->
                _loadingState.value = false
                Log.e(TAG, "Sign-in initialization failed", e)
                viewModelScope.launch {
                    _errorMessage.emit(when (e) {
                        is ApiException -> "Error code ${e.statusCode}: ${e.message}"
                        else -> "Sign-in initialization failed"
                    })
                }
            }
    }

    fun handleSignInResult(result: ActivityResult) {
        if (result.resultCode == RESULT_OK) {
            _loadingState.value = true
            try {
                val credential = oneTapClient.getSignInCredentialFromIntent(result.data)
                val idToken = credential.googleIdToken

                idToken?.let { token ->
                    val firebaseCredential = GoogleAuthProvider.getCredential(token, null)
                    auth.signInWithCredential(firebaseCredential)
                        .addOnCompleteListener { task ->
                            _loadingState.value = false
                            if (task.isSuccessful) {
                                checkUserInFirestore(auth.currentUser?.uid)
                            } else {
                                handleAuthError(task.exception)
                            }
                        }
                } ?: run {
                    _loadingState.value = false
                    viewModelScope.launch {
                        _errorMessage.emit("No ID token found")
                    }
                }
            } catch (e: ApiException) {
                _loadingState.value = false
                handleAuthError(e)
            }
        }
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
                    navigateTo(if (document.exists()) "home" else "googleRegistration")
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

    companion object {
        private const val TAG = "AuthViewModel"
    }
}

