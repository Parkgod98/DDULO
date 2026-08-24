package com.example.ddulo.ui.util

import androidx.compose.ui.graphics.Color

fun getLineColor(lineName: String): Color {
    return when {
        lineName.contains("1호선") -> Color(0xFF0052A4)
        lineName.contains("2호선") -> Color(0xFF00A84D)
        lineName.contains("3호선") -> Color(0xFFEF7C1C)
        lineName.contains("4호선") -> Color(0xFF00A5DE)
        lineName.contains("5호선") -> Color(0xFF996CAC)
        lineName.contains("6호선") -> Color(0xFFCD7C2F)
        lineName.contains("7호선") -> Color(0xFF747F00)
        lineName.contains("8호선") -> Color(0xFFE6186C)
        lineName.contains("9호선") -> Color(0xFFBDB092)
        else -> Color.Gray
    }
}

fun lineColorFromLineName(lineName: String): Color {
    val normalized = lineName.replace(" ", "")
    return when {
        normalized.contains("2호선") || normalized.startsWith("2") -> Color(0xFF00A242)
        normalized.contains("7호선") || normalized.startsWith("7") -> Color(0xFF6A6E2D)
        else -> Color(0xFF4CE5B1)
    }
}