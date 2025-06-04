package com.example.sustavzainstrukcije.ui.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import com.example.sustavzainstrukcije.ui.data.InstructionSession
import com.example.sustavzainstrukcije.ui.data.SessionInvitation
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.database.database
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

    // Dohvaćanje sessiona za studenta
    fun getStudentSessions() {
        val currentUserId = auth.currentUser?.uid ?: return

        firestore.collection("sessions")
            .whereEqualTo("studentId", currentUserId)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.e("SessionViewModel", "Error fetching student sessions", e)
                    return@addSnapshotListener
                }

                val sessionsList = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(InstructionSession::class.java)?.copy(id = doc.id)
                } ?: emptyList()

                _sessions.value = sessionsList
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
}
