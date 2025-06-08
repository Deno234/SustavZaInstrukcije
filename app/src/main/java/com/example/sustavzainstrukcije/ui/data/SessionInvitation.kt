package com.example.sustavzainstrukcije.ui.data

data class SessionInvitation(
    val id: String = "",
    val sessionId: String = "",
    val instructorId: String = "",
    val studentId: String = "",
    val subject: String = "",
    val status: String = "pending",
    val createdAt: Long = System.currentTimeMillis()
)
