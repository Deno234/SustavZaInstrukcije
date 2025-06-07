package com.example.sustavzainstrukcije.ui.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import com.example.sustavzainstrukcije.ui.data.InstructionSession
import com.example.sustavzainstrukcije.ui.data.SessionInvitation
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.database
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
    fun createSession(studentId: String, subject: String) {
        val currentUserId = auth.currentUser?.uid ?: return
        val sessionId = UUID.randomUUID().toString()

        val session = InstructionSession(
            id = sessionId,
            instructorId = currentUserId,
            studentId = studentId,
            subject = subject,
            status = "pending"
        )

        // Spremi session u Firestore
        firestore.collection("sessions").document(sessionId)
            .set(session)
            .addOnSuccessListener {
                Log.d("SessionViewModel", "Session created: $sessionId")
                createSessionInvitation(session)
            }
            .addOnFailureListener { e ->
                Log.e("SessionViewModel", "Error creating session", e)
            }
    }

    private fun createSessionInvitation(session: InstructionSession) {
        val invitationId = UUID.randomUUID().toString()
        val invitation = SessionInvitation(
            id = invitationId,
            sessionId = session.id,
            instructorId = session.instructorId,
            studentId = session.studentId,
            subject = session.subject
        )

        firestore.collection("invitations").document(invitationId)
            .set(invitation)
            .addOnSuccessListener {
                Log.d("SessionViewModel", "Invitation sent to student: ${session.studentId}")
                // Pošalji notifikaciju studentu
                sendSessionInvitationNotification(session)
            }
    }

    private fun sendSessionInvitationNotification(session: InstructionSession) {
        // Implementiranje slanje notifikacije preko FCM-a
        // Slično kao što se šaljeu poruke, ali s tipom "session_invitation"
    }

    // Dohvaćanje sessiona za instruktora
    fun getInstructorSessions() {
        val currentUserId = auth.currentUser?.uid ?: return

        firestore.collection("sessions")
            .whereEqualTo("instructorId", currentUserId)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.e("SessionViewModel", "Error fetching sessions", e)
                    return@addSnapshotListener
                }

                val sessionsList = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(InstructionSession::class.java)?.copy(id = doc.id)
                } ?: emptyList()

                _sessions.value = sessionsList
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

    // Modificiraj acceptInvitation da automatski navigira na session
    fun acceptInvitation(invitationId: String, onInvitationAccepted: () -> Unit) {
        firestore.collection("invitations").document(invitationId)
            .update("status", "accepted")
            .addOnSuccessListener {
                Log.d("SessionViewModel", "Invitation accepted")

                // Dohvati sessionId iz pozivnice i ažuriraj status
                firestore.collection("invitations").document(invitationId)
                    .get()
                    .addOnSuccessListener { doc ->
                        val invitation = doc.toObject(SessionInvitation::class.java)
                        invitation?.let {
                            updateSessionStatus(it.sessionId, "active")
                            onInvitationAccepted() // Samo zvanje callback, ne navigiraj
                        }
                    }
            }
    }

    // Dohvaćanje svih sessiona za studenta (prihvaćenih i pending)
    fun getAllStudentSessions() {
        val currentUserId = auth.currentUser?.uid ?: return

        // Dohvati sve sessione gdje je student pozvan
        firestore.collection("sessions")
            .whereEqualTo("studentId", currentUserId)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.e("SessionViewModel", "Error fetching all student sessions", e)
                    return@addSnapshotListener
                }

                val sessionsList = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(InstructionSession::class.java)?.copy(id = doc.id)
                } ?: emptyList()

                _sessions.value = sessionsList
            }
    }

    private fun updateSessionStatus(sessionId: String, status: String) {
        firestore.collection("sessions").document(sessionId)
            .update(
                mapOf(
                    "status" to status,
                    "startedAt" to System.currentTimeMillis()
                )
            )
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

    fun updateSessionEditable(sessionId: String, editable: Boolean) {
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

}
