package com.example.sustavzainstrukcije.ui.data

data class InstructionSession(
    val id: String = "",
    val instructorId: String = "",
    val studentId: String = "",
    val subject: String = "",
    val status: String = "pending", // pending, active, completed
    val createdAt: Long = System.currentTimeMillis(),
    val startedAt: Long? = null,
    val endedAt: Long? = null,
    val pages: List<String> = emptyList(), // Lista ID-jeva stranica
    val currentPageId: String = ""
)
