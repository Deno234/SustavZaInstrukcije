package com.example.sustavzainstrukcije.ui.data

data class RatingComment(
    val studentId: String,
    val studentName: String? = null,
    val text: String,
    val rating: Int,
    val subject: String
)

