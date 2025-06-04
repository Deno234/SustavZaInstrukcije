package com.example.sustavzainstrukcije.ui.data

data class DrawingStroke(
    val id: String = "",
    val userId: String = "",
    val points: List<Point> = emptyList(),
    val color: String = "#000000",
    val strokeWidth: Float = 5f,
    val timestamp: Long = System.currentTimeMillis()
)
