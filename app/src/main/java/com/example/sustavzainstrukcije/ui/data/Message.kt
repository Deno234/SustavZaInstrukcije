package com.example.sustavzainstrukcije.ui.data

data class Message(
    val text: String = "",
    val senderId: String = "",
    val receiverId: String = "",
    val timestamp: Long = 0L,
    val readBy: Map<String, Boolean> = emptyMap()
)
