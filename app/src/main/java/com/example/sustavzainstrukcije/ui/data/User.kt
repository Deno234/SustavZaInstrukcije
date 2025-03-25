package com.example.sustavzainstrukcije.ui.data

data class User(
    val id: String = "",
    val name: String = "",
    val email: String = "",
    val role: String = "student",
    val subjects: List<String> = emptyList(),
    val availableHours: Map<String, List<String>> = emptyMap()
)
