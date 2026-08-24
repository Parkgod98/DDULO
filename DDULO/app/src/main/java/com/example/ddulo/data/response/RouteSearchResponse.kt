package com.example.ddulo.data.response

/* ================= Route Search ================= */

data class RouteSearchResponse(
    val fastestPathResponse: FastestPathResponse,
    val pathPredictionResponse: PathPredictionResponse,
    val resultResponse: RouteResultResponse
)

/* ================= Fastest Path ================= */

data class FastestPathResponse(
    val totalTime: Int,
    val transferCount: Int,
    val legs: List<RouteLegResponse>
)

data class RouteLegResponse(
    val startStation: String,
    val endStation: String,
    val lineName: String,
    val direction: String,
    val sectionTime: Int
)

/* ================= Path Prediction ================= */

data class PathPredictionResponse(
    val predictions: List<PathPrediction>
)

data class PathPrediction(
    val estimatedBoardingTime: String,
    val estimatedArrivalTime: String,
    val waitingTimeSeconds: Long,
    val boardingProbability: Int
)

/* ================= Route Result ================= */

data class RouteResultResponse(
    val totalTimeSecond: Int,
    val estimatedBoardingTime: String,
    val boardingProbability: Double,
    val startStation: List<StationResult>,
    val transferStation: List<TransferStationGroup>? = null,
    val endStation: StationResult
) {
    val transferStationOrEmpty: List<TransferStationGroup>
        get() = transferStation.orEmpty()
}

/* ================= Transfer Station ================= */

data class TransferStationGroup(
    val transferOrder: Int,
    val options: List<StationResult>
) {
    val optionsOrEmpty: List<StationResult>
        get() = options.orEmpty()
}

/* ================= Station ================= */

data class StationResult(
    val stationCode: String,
    val stationName: String,
    val lineName: String,
    val nextStationName: String?,
    val estimatedWaitingSec: Int,
    val isBoardable: Boolean,
    val results: List<StationCongestionResult>? = null
) {
    val resultsOrEmpty: List<StationCongestionResult>
        get() = results.orEmpty()
}

/* ================= Congestion ================= */

data class StationCongestionResult(
    val carCongestions: List<CarCongestion>? = null,
    val stationCongestions: List<DoorCongestion>? = null,
    val bestBoardings: List<BoardingPosition>? = null,
    val comfortBoarding: List<BoardingPosition>? = null
) {
    val carCongestionsOrEmpty get() = carCongestions.orEmpty()
    val stationCongestionsOrEmpty get() = stationCongestions.orEmpty()
    val bestBoardingsOrEmpty get() = bestBoardings.orEmpty()
    val comfortBoardingOrEmpty get() = comfortBoarding.orEmpty()
}

data class CarCongestion(
    val carNo: Int,
    val congestionLevel: Int
)

data class DoorCongestion(
    val carNo: Int,
    val doorNo: Int,
    val congestionLevel: Int
)

data class BoardingPosition(
    val carNo: Int,
    val doorNo: Int
)
