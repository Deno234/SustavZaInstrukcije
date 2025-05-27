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
import com.google.firebase.firestore.SetOptions
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AuthViewModel(application: Application) : AndroidViewModel(application) {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    private val _errorMessage = MutableSharedFlow<String>()
    val errorMessage = _errorMessage.asSharedFlow()

    private val _loadingState = MutableStateFlow(false)
    val loadingState: StateFlow<Boolean> = _loadingState.asStateFlow()

    private val _userData = MutableStateFlow<User?>(null)
    val userData: StateFlow<User?> = _userData.asStateFlow()

    val credentialManager: CredentialManager = CredentialManager.create(application)

    var currentUserId: String? = auth.currentUser?.uid
        private set

    private var navController: NavController? = null

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
                            Log.d(TAG, "Firebase signInWithCredential successful.")
                            this.currentUserId = auth.currentUser?.uid
                            checkUserInFirestore(this.currentUserId)
                        } else {
                            Log.w(TAG, "Firebase signInWithCredential failed.", task.exception)
                            handleAuthError(task.exception)
                        }
                    }
            } else {
                _loadingState.value = false
                Log.w(TAG, "Invalid credential type received from Google Sign-In.")
                viewModelScope.launch {
                    _errorMessage.emit("Neispravan tip vjerodajnice.")
                }
            }
        } catch (e: GoogleIdTokenParsingException) {
            _loadingState.value = false
            Log.e(TAG, "GoogleIdTokenParsingException", e)
            handleAuthError(e)
        }
    }

    fun handleSignInError(e: GetCredentialException) {
        _loadingState.value = false
        Log.e(TAG, "GetCredentialException", e)
        handleAuthError(e)
    }

    fun checkUserInFirestore(userId: String?) {
        if (userId == null || userId.isEmpty()) {
            Log.e(TAG, "checkUserInFirestore called with null or empty userId.")
            viewModelScope.launch {
                _errorMessage.emit("Autentifikacija korisnika nije uspjela.")
            }
            _loadingState.value = false
            return
        }
        _loadingState.value = true
        db.collection("users").document(userId).get()
            .addOnSuccessListener { document ->
                _loadingState.value = false
                viewModelScope.launch {
                    if (document.exists()) {
                        _userData.value = document.toObject(User::class.java)?.copy(id = userId)
                        Log.d(TAG, "User $userId exists in Firestore. UserData: ${_userData.value}")
                        updateFcmToken()
                        navigateTo("home")
                    } else {
                        Log.d(TAG, "User $userId does not exist in Firestore.")
                        navigateTo("googleRegistration")
                    }
                }
            }
            .addOnFailureListener { exception ->
                _loadingState.value = false
                Log.e(TAG, "Error fetching user from Firestore for $userId.", exception)
                viewModelScope.launch {
                    _errorMessage.emit("Greška u bazi: ${exception.localizedMessage}")
                }
            }
    }

    private fun handleAuthError(exception: Exception?) {
        val errorMessage = when (exception) {
            is ApiException -> "Google prijava nije uspjela: ${exception.statusCode}"
            else -> "Autentifikacija nije uspjela: ${exception?.localizedMessage}"
        }
        Log.e(TAG, errorMessage, exception)
        viewModelScope.launch {
            _errorMessage.emit(errorMessage)
        }
    }

    private fun navigateTo(destination: String) {
        if (navController == null) {
            Log.e(TAG, "NavController not set in AuthViewModel. Cannot navigate to $destination.")
            return
        }
        Log.d(TAG, "Navigating to: $destination")
        navController?.navigate(destination) {
            if (destination == "home") {
                popUpTo(navController?.graph?.startDestinationId ?: 0) { inclusive = true }
                launchSingleTop = true
            }
        }
    }

    fun fetchCurrentUserData() {
        val firebaseUser = auth.currentUser
        this.currentUserId = firebaseUser?.uid

        firebaseUser?.uid?.let { userId ->
            if (userId.isNotEmpty()) {
                db.collection("users").document(userId).get()
                    .addOnSuccessListener { document ->
                        if (document.exists()) {
                            _userData.value = document.toObject(User::class.java)?.copy(id = userId)
                            Log.d(TAG, "Fetched current user data: ${_userData.value}")
                            updateFcmToken()
                        } else {
                            Log.d(TAG, "No document found for current user $userId during fetch.")
                            _userData.value = null
                        }
                    }
                    .addOnFailureListener { e ->
                        Log.e(TAG, "Error fetching current user data for $userId.", e)
                        _userData.value = null
                    }
            }
        } ?: run {
            Log.d(TAG, "No current Firebase user to fetch data for.")
            _userData.value = null
            this.currentUserId = null
        }
    }

    fun updateInstructorProfile(name: String, subjects: List<String>, availableHours: Map<String, List<String>>) {
        val userId = this.currentUserId ?: run {
            Log.w(TAG, "Cannot update profile: currentUserId is null.")
            viewModelScope.launch { _errorMessage.emit("Niste prijavljeni.") }
            return
        }
        val updates = mapOf(
            "name" to name,
            "subjects" to subjects,
            "availableHours" to availableHours
        )
        db.collection("users").document(userId).update(updates)
            .addOnSuccessListener {
                Log.d(TAG, "Instructor profile updated for $userId.")
                fetchCurrentUserData()
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error updating instructor profile for $userId.", e)
                viewModelScope.launch { _errorMessage.emit("Greška pri ažuriranju profila.") }
            }
    }

    /**
     * Dohvaća trenutni FCM token i sprema/ažurira ga u Firestore-u za prijavljenog korisnika.
     * Ovu funkciju treba pozvati MainActivity nakon što je dobivena dozvola za notifikacije,
     * ili AuthViewModel nakon uspješne prijave/registracije.
     */
    fun updateFcmToken() {
        val userId = this.currentUserId
        if (userId == null || userId.isEmpty()) {
            Log.w(TAG, "Cannot update FCM token: currentUserId is not available.")
            return
        }

        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                Log.w(TAG, "Fetching FCM registration token failed", task.exception)
                return@addOnCompleteListener
            }
            val token = task.result
            Log.d(TAG, "Fetched FCM token: $token for user $userId")

            if (token != null) {
                val userDocRef = db.collection("users").document(userId)
                // Koristi se SetOptions.merge() da se ne prebrišu ostala polja u dokumentu.
                // Primjer za jedan token:
                val tokenUpdate = mapOf("fcmToken" to token)
                // Primjer za listu tokena:
                // val tokenUpdate = mapOf("fcmTokens" to FieldValue.arrayUnion(token))

                userDocRef.set(tokenUpdate, SetOptions.merge())
                    .addOnSuccessListener { Log.d(TAG, "FCM token explicitly updated for user $userId") }
                    .addOnFailureListener { e -> Log.w(TAG, "Error updating FCM token for user $userId", e) }
            } else {
                Log.w(TAG, "Fetched FCM token is null for user $userId")
            }
        }
    }

    fun completeGoogleRegistration(userId: String, name: String, email: String, role: String /* ... ostali podaci */) {
        if (userId.isEmpty()) {
            Log.e(TAG, "Cannot complete registration: userId is empty.")
            viewModelScope.launch { _errorMessage.emit("ID korisnika nedostaje.") }
            return
        }
        val user = User(
            id = userId,
            name = name,
            email = email,
            role = role
        )
        _loadingState.value = true
        db.collection("users").document(userId).set(user)
            .addOnSuccessListener {
                _loadingState.value = false
                Log.d(TAG, "User document created/updated for $userId after Google registration.")
                _userData.value = user
                this.currentUserId = userId
                updateFcmToken()
                navigateTo("home")
            }
            .addOnFailureListener { e ->
                _loadingState.value = false
                Log.e(TAG, "Error creating/updating user document for $userId after Google registration.", e)
                viewModelScope.launch { _errorMessage.emit("Greška pri spremanju podataka.") }
            }
    }


    companion object {
        private const val TAG = "AuthViewModel"
    }
}
