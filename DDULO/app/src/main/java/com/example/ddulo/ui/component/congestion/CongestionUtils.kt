package com.example.ddulo.ui.component.congestion

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import com.example.ddulo.data.response.StationCongestionResult

/**
 * 색상 매핑
 * API -> List 변환 함수
 */
// 열차 내 혼잡도: 0% = #C8C8FF, 100% = #00009B (101등분 RGB 매핑)
private val trainCongestionStartColor = Color(0xFFC8C8FF)
private val trainCongestionEndColor = Color(0xFF00009B)
// 플랫폼 혼잡도: 0% = #FFC8C8, 100% = #9B0000 (101등분 RGB 매핑)
private val platformCongestionStartColor = Color(0xFFFFC8C8)
private val platformCongestionEndColor = Color(0xFF9B0000)

fun getTrainCongestionColor(percentage: Int): Color {
    val fraction = percentage.coerceIn(0, 100) / 100f
    return lerp(trainCongestionStartColor, trainCongestionEndColor, fraction)
}

fun getPlatformCongestionColor(percentage: Int): Color {
    val fraction = percentage.coerceIn(0, 100) / 100f
    return lerp(platformCongestionStartColor, platformCongestionEndColor, fraction)
}

/** API 응답(StationCongestionResult)을 열차 칸/문별 리스트로 변환 (null 리스트 방어) */
fun StationCongestionResult.toTrainAndPlatformLists(
    numberOfCars: Int = 10
): Pair<List<Int>, List<List<Int>>> {
    val cars = carCongestionsOrEmpty
    val doors = stationCongestionsOrEmpty
    val trainCongestion = (1..numberOfCars).map { carNo ->
        cars.find { it.carNo == carNo }?.congestionLevel ?: 0
    }
    val platformCongestion = (1..numberOfCars).map { carNo ->
        doors
            .filter { it.carNo == carNo }
            .sortedBy { it.doorNo }
            .map { it.congestionLevel }
            .ifEmpty { listOf(0) }
    }
    return trainCongestion to platformCongestion
}