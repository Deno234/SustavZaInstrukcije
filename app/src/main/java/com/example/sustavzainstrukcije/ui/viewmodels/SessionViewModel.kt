package com.example.sustavzainstrukcije.ui.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import com.example.sustavzainstrukcije.ui.data.InstructionSession
import com.example.sustavzainstrukcije.ui.data.SessionInvitation
import com.example.sustavzainstrukcije.ui.utils.isNowWithinAnyInterval
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.database
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import java.util.UUID

class SessionViewModel : ViewModel() {
    private val db = Firebase.database
    private val firestore = Firebase.firestore
    private val auth = Firebase.auth

    private val _sessions = MutableStateFlow<List<InstructionSession>>(emptyList())
    val sessions: StateFlow<List<InstructionSession>> = _sessions.asStateFlow()

    private val _currentSession = MutableStateFlow<InstructionSession?>(null)
    val currentSession: StateFlow<InstructionSession?> = _currentSession.asStateFlow()

    private val _invitations = MutableStateFlow<List<SessionInvitation>>(emptyList())
    val invitations: StateFlow<List<SessionInvitation>> = _invitations.asStateFlow()

    private val _onlineUsers = MutableStateFlow<List<String>>(emptyList())
    val onlineUsers: StateFlow<List<String>> = _onlineUsers

    private val _onlineUsersMap = MutableStateFlow<Map<String, List<String>>>(emptyMap())
    val onlineUsersMap: StateFlow<Map<String, List<String>>> = _onlineUsersMap

    private val _userNames = MutableStateFlow<Map<String, String>>(emptyMap())
    val userNames: StateFlow<Map<String, String>> = _userNames

    private val _lastVisitedMap = MutableStateFlow<Map<String, Long>>(emptyMap())
    val lastVisitedMap: StateFlow<Map<String, Long>> = _lastVisitedMap



    // Kreiranje novog sessiona
    fun createSession(studentIds: List<String>, subject: String) {
        val currentUserId = auth.currentUser?.uid ?: return
        val sessionId = UUID.randomUUID().toString()

        val session = InstructionSession(
            id = sessionId,
            instructorId = currentUserId,
            studentIds = studentIds,
            subject = subject,
        )

        // Spremanje sjednice u Firestore
        firestore.collection("sessions").document(sessionId)
            .set(session)
            .addOnSuccessListener {
                Log.d("SessionViewModel", "Session created: $sessionId")

                studentIds.forEach { studentId ->
                    createSessionInvitation(sessionId, currentUserId, studentId, subject)
                }
            }
            .addOnFailureListener { e ->
                Log.e("SessionViewModel", "Error creating session", e)
            }
    }

    private fun createSessionInvitation(sessionId: String, instructorId: String, studentId: String, subject: String) {
        val invitationId = UUID.randomUUID().toString()
        val invitation = SessionInvitation(
            id = invitationId,
            sessionId = sessionId,
            instructorId = instructorId,
            studentId = studentId,
            subject = subject
        )

        firestore.collection("invitations").document(invitationId)
            .set(invitation)
            .addOnSuccessListener {
                Log.d("SessionViewModel", "Invitation sent to student: $studentId")
            }
    }

    // Dohvaćanje sjednice za instruktora
    fun getInstructorSessions() {
        val currentUserId = auth.currentUser?.uid ?: return

        firestore.collection("sessions")
            .whereEqualTo("instructorId", currentUserId)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, e ->
                if (e != null) return@addSnapshotListener

                firestore.collection("users")
                    .document(currentUserId)
                    .collection("hiddenSessions")
                    .addSnapshotListener { hiddenSnapshot, _ ->
                        val hiddenIds = hiddenSnapshot?.documents?.map { it.id }?.toSet() ?: emptySet()

                        val sessionsList = snapshot?.documents?.mapNotNull { doc ->
                            doc.toObject(InstructionSession::class.java)?.copy(id = doc.id)
                        }?.filterNot { hiddenIds.contains(it.id) } ?: emptyList()

                        _sessions.value = sessionsList
                    }
            }
    }


    // Dohvaćanje pozivnica za studenta
    fun getStudentInvitations() {
        val currentUserId = auth.currentUser?.uid ?: return

        firestore.collection("invitations")
            .whereEqualTo("studentId", currentUserId)
            .whereEqualTo("status", "pending")
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.e("SessionViewModel", "Error fetching invitations", e)
                    return@addSnapshotListener
                }

                val invitationsList = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(SessionInvitation::class.java)?.copy(id = doc.id)
                } ?: emptyList()

                _invitations.value = invitationsList
            }
    }

    fun acceptInvitation(invitationId: String, onInvitationAccepted: () -> Unit) {
        firestore.collection("invitations").document(invitationId)
            .update("status", "accepted")
            .addOnSuccessListener {
                Log.d("SessionViewModel", "Invitation accepted")
                onInvitationAccepted()
            }
    }

    // Dohvaćanje svih sjednica za studenta (prihvaćenih i pending)
    fun getAllStudentSessions() {
        val currentUserId = auth.currentUser?.uid ?: return

        firestore.collection("invitations")
            .whereEqualTo("studentId", currentUserId)
            .whereEqualTo("status", "accepted")
            .addSnapshotListener { invitationSnapshot, e ->
                if (e != null) {
                    Log.e("SessionViewModel", "Error fetching invitations", e)
                    return@addSnapshotListener
                }

                val acceptedSessionIds = invitationSnapshot?.documents?.mapNotNull {
                    it.getString("sessionId")
                } ?: emptyList()

                if (acceptedSessionIds.isEmpty()) {
                    _sessions.value = emptyList()
                    return@addSnapshotListener
                }

                firestore.collection("users")
                    .document(currentUserId)
                    .collection("hiddenSessions")
                    .addSnapshotListener { hiddenSnapshot, _ ->
                        val hiddenIds = hiddenSnapshot?.documents?.map { it.id }?.toSet() ?: emptySet()

                        firestore.collection("sessions")
                            .whereIn(FieldPath.documentId(), acceptedSessionIds.take(10))
                            .orderBy("createdAt", Query.Direction.DESCENDING)
                            .addSnapshotListener { snapshot, e2 ->
                                if (e2 != null) {
                                    Log.e("SessionViewModel", "Error fetching student sessions", e2)
                                    return@addSnapshotListener
                                }

                                val sessionsList = snapshot?.documents?.mapNotNull { doc ->
                                    doc.toObject(InstructionSession::class.java)?.copy(id = doc.id)
                                }?.filterNot { hiddenIds.contains(it.id) } ?: emptyList()

                                _sessions.value = sessionsList

                                listenToOnlineUsersForSessions(sessionsList.map { it.id })
                            }
                    }
            }
    }





    fun listenToOnlineUsers(sessionId: String) {
        val ref = db.getReference("online_users/$sessionId")
        ref.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val instructorId = _currentSession.value?.instructorId ?: return
                val ids = snapshot.children.mapNotNull { it.key }
                Log.d("WB", "Firebase ONLINE IDS update: $ids")
                _onlineUsers.value = ids
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    fun setUserOnline(sessionId: String) {
        val userId = auth.currentUser?.uid ?: return
        val ref = db.getReference("online_users/$sessionId/$userId")
        ref.setValue(true)
        ref.onDisconnect().removeValue()

        firestore.collection("sessions").document(sessionId)
            .get().addOnSuccessListener { snapshot ->
                val instructorId = snapshot.getString("instructorId")
                if (instructorId == userId) {
                    updateSessionEditable(sessionId, true)
                }
            }
    }

    private fun updateSessionEditable(sessionId: String, editable: Boolean) {
        firestore.collection("sessions").document(sessionId)
            .update("isEditable", editable)
    }

    fun loadSession(sessionId: String) {
        firestore.collection("sessions").document(sessionId)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.e("SessionViewModel", "Error loading session", e)
                    return@addSnapshotListener
                }

                val session = snapshot?.toObject(InstructionSession::class.java)?.copy(id = snapshot.id)
                _currentSession.value = session
            }
    }

    fun setUserOffline(sessionId: String) {
        val userId = auth.currentUser?.uid ?: return
        db.getReference("online_users/$sessionId/$userId").removeValue()
    }

    fun listenToOnlineUsersForSessions(sessionIds: List<String>) {
        sessionIds.forEach { sessionId ->
            db.getReference("online_users/$sessionId")
                .addValueEventListener(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val userIds = snapshot.children.mapNotNull { it.key }
                        _onlineUsersMap.value = _onlineUsersMap.value.toMutableMap().apply {
                            this[sessionId] = userIds
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {}
                })
        }
    }

    fun loadUserNames() {
        Firebase.firestore.collection("users")
            .get()
            .addOnSuccessListener { snapshot ->
                val map = snapshot.documents.associate {
                    val uid = it.id
                    val name = it.getString("name") ?: "User"
                    uid to name
                }
                _userNames.value = map
            }
    }

    fun fetchLastVisitedTimestamps(sessionIds: List<String>) {
        val currentUserId = auth.currentUser?.uid ?: return
        val collection = firestore.collection("users")
            .document(currentUserId)
            .collection("lastVisited")

        collection.get().addOnSuccessListener { snapshot ->
            val map = snapshot.documents.mapNotNull { doc ->
                val sessionId = doc.id
                val timestamp = doc.getLong("timestamp")
                if (timestamp != null && sessionId in sessionIds) sessionId to timestamp else null
            }.toMap()
            _lastVisitedMap.value = map
        }
    }

    fun loadLastVisitedSessions() {
        val userId = auth.currentUser?.uid ?: return

        firestore.collection("users")
            .document(userId)
            .collection("lastVisited")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("SessionViewModel", "Failed to load lastVisited", error)
                    return@addSnapshotListener
                }

                val visitedMap = snapshot?.documents?.associate {
                    it.id to (it.getLong("timestamp") ?: 0L)
                } ?: emptyMap()

                _lastVisitedMap.value = visitedMap
            }
    }

    fun declineInvitation(invitationId: String, onComplete: () -> Unit = {}) {
        Firebase.firestore.collection("invitations")
            .document(invitationId)
            .delete()
            .addOnSuccessListener {
                onComplete()
            }
    }

    suspend fun checkInstructorIsWithinWorkingHours(): Boolean {
        val userId = auth.currentUser?.uid ?: return false
        val snap = Firebase.firestore.collection("users").document(userId).get().await()
        val hours = snap.get("availableHours") as? Map<String, List<String>> ?: emptyMap()
        return isNowWithinAnyInterval(hours)
    }

    fun hideSessionForCurrentUser(sessionId: String) {
        val userId = auth.currentUser?.uid ?: return

        firestore.collection("users")
            .document(userId)
            .collection("hiddenSessions")
            .document(sessionId)
            .set(mapOf("hidden" to true))
            .addOnSuccessListener {
                Log.d("SessionViewModel", "Session $sessionId hidden for $userId")
            }
            .addOnFailureListener { e ->
                Log.e("SessionViewModel", "Failed to hide session", e)
            }
    }




}
