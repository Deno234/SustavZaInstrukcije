package com.example.sustavzainstrukcije.ui.utils

fun generateChatId(userId1: String, userId2: String): String {
    return if (userId1 < userId2) "${userId1}_${userId2}" else "${userId2}_${userId1}"
}