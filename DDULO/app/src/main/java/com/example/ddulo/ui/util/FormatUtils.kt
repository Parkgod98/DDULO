package com.example.ddulo.ui.util

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

fun formatTimeForDisplay(isoDateTime: String): String {
    return try {
        val tIndex = isoDateTime.indexOf('T')
        if (tIndex >= 0 && isoDateTime.length >= tIndex + 6) {
            isoDateTime.substring(tIndex + 1, tIndex + 6) // "HH:mm"
        } else {
            isoDateTime
        }
    } catch (_: Exception) {
        isoDateTime
    }
}

fun formatArrivalSec(arrivalSec: Int): String {
    val min = arrivalSec / 60
    val sec = arrivalSec % 60
    return if (min > 0) "${min}분 ${sec}초" else "${sec}초"
}

/**
 * arrivalSec(현재로부터 초)를 도착 예정 시각 HH:mm 문자열로 변환.
 */
fun formatArrivalTime(arrivalSec: Int): String {
    val arrivalMillis = System.currentTimeMillis() + arrivalSec * 1000L
    val instant = Instant.ofEpochMilli(arrivalMillis)
    val localTime = instant.atZone(ZoneId.systemDefault()).toLocalTime()
    return localTime.format(DateTimeFormatter.ofPattern("HH:mm"))
}