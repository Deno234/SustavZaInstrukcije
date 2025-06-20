package com.example.sustavzainstrukcije.ui.data

data class InstructionSession(
    val id: String = "",
    val instructorId: String = "",
    val studentIds: List<String> = emptyList(),
    val subject: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val startedAt: Long? = null,
    val endedAt: Long? = null,
    val pages: List<String> = emptyList(),
    val currentPageId: String = "",
    val isEditable: Boolean = false
)
