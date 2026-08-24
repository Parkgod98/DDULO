package com.example.ddulo.domain.model

import com.example.ddulo.data.response.PathPredictionResponse
import com.example.ddulo.data.response.RouteResultResponse

data class RouteResult(
    val totalTime: Int,
    val transferCount: Int,
    val legs: List<RouteLeg>
)

data class RouteLeg(
    val startStation: String,
    val endStation: String,
    val lineName: String,
    val sectionTime: Int // 초 단위
)

/**
 * 즐겨찾기 경로 정보
 * 재검색을 위해 역 이름을 저장하고, 목록 표시를 위해 저장 시점의 요약 정보를 함께 유지합니다.
 */
data class FavoriteRoute(
    val departureName: String,
    val destinationName: String,
    val totalTime: Int,
    val transferCount: Int
)
