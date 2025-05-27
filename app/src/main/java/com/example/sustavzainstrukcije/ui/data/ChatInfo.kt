package com.example.sustavzainstrukcije.ui.data

data class ChatInfo(
    val chatId: String = "",
    val otherUserId: String = "",
    val lastMessageTimestamp: Long = 0L,
    val lastMessageText: String = "",
    val unreadCount: Int = 0
)
