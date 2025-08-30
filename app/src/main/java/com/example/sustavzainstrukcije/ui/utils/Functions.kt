package com.example.sustavzainstrukcije.ui.utils

import android.util.Log
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

fun generateChatId(userId1: String, userId2: String): String {
    return if (userId1 < userId2) "${userId1}_${userId2}" else "${userId2}_${userId1}"
}

private val TIME_FMT = DateTimeFormatter.ofPattern("HH:mm", Locale.ROOT)

fun dayKeyForToday(): String = when (LocalDate.now().dayOfWeek) {
    DayOfWeek.MONDAY -> "Monday"
    DayOfWeek.TUESDAY -> "Tuesday"
    DayOfWeek.WEDNESDAY -> "Wednesday"
    DayOfWeek.THURSDAY -> "Thursday"
    DayOfWeek.FRIDAY -> "Friday"
    DayOfWeek.SATURDAY -> "Saturday"
    DayOfWeek.SUNDAY -> "Sunday"
}

private fun parseLocalTime(hhmm: String) = LocalTime.parse(hhmm, TIME_FMT)

fun isNowWithinAnyInterval(hours: Map<String, List<String>>): Boolean {

    val key = dayKeyForToday()
    val slots = hours[key].orEmpty()
    val now = LocalTime.now()
    return slots.any { slot ->
        val startStr = slot.substringBefore(" - ").trim()
        val endStr = slot.substringAfter(" - ").trim()
        val start = parseLocalTime(startStr)
        val end = parseLocalTime(endStr)

        Log.d("HOURS", "key=${dayKeyForToday()} slots=${hours[dayKeyForToday()]}")
        Log.d("HOURS", "now=${LocalTime.now()} start=$start end=$end in=${!now.isBefore(start) && !now.isAfter(end)}")
        // inkluzivno: start <= now <= end
        !now.isBefore(start) && !now.isAfter(end)
    }
}

fun millisUntilEndOfCurrentInterval(availableHours: Map<String, List<String>>): Long? {
    val key = dayKeyForToday()
    val slots = availableHours[key].orEmpty()
    if (slots.isEmpty()) return null
    val now = LocalTime.now()
    val matching = slots.firstOrNull { slot ->
        val startStr = slot.substringBefore(" - ").trim()
        val endStr = slot.substringAfter(" - ").trim()
        val start = parseLocalTime(startStr)
        val end = parseLocalTime(endStr)
        !now.isBefore(start) && !now.isAfter(end)
    } ?: return null
    val end = parseLocalTime(matching.substringAfter(" - ").trim())
    val seconds = java.time.Duration.between(now, end).seconds
    return (seconds.coerceAtLeast(0)) * 1000
}