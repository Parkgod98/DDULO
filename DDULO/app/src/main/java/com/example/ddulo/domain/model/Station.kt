package com.example.ddulo.domain.model

data class Station(
    val stationId: String,
    val stationName: String,
    val lineName: String,
    val latitude: Double,
    val longitude: Double,
    val distanceMeters: Double? = null
) {
    // 기존 UI 코드(name, code)와의 호환성을 위해 추가
    val name: String get() = stationName
    val code: String get() = lineName
}
