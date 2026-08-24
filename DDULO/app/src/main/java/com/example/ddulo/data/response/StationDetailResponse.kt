package com.example.ddulo.data.response

data class StationDetailResponse(
    val station: StationInfo,
    val upBound: List<TrainBound>,
    val downBound: List<TrainBound>
)

data class StationInfo(
    val stationCode: String,
    val stationName: String,
    val lineName: String,
    val prevStations: List<AdjacentStation>,
    val nextStations: List<AdjacentStation>
)

data class AdjacentStation(
    val stationCode: String,
    val stationName: String
)

data class TrainBound(
    val direction: String,
    val arrivalSec: Int,
    val currentStation: String,
    val destination: String,
    val isBoardable: Boolean,
    val carCongestions: List<CarCongestion>
)
