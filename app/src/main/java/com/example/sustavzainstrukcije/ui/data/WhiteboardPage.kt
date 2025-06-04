package com.example.sustavzainstrukcije.ui.data

data class WhiteboardPage(
    val id: String = "",
    val sessionId: String = "",
    val pageNumber: Int = 1,
    val strokes: List<DrawingStroke> = emptyList(),
    val createdAt: Long = System.currentTimeMillis(),
    val isActive: Boolean = true,
    val createdBy: String = ""
)
