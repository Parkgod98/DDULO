package com.example.ddulo.data.mapper

import com.example.ddulo.data.response.*
import com.example.ddulo.domain.model.*

fun StationCongestionResult.toDomain(): StationCongestionResult {
    return StationCongestionResult(
        carCongestions = carCongestions.orEmpty().map { it.toDomain() },
        stationCongestions = stationCongestions.orEmpty().map { it.toDomain() },
        bestBoardings = bestBoardings.orEmpty().map { it.toDomain() },
        comfortBoarding = comfortBoarding.orEmpty().map { it.toDomain() }
    )
}

fun CarCongestion.toDomain(): CarCongestion =
    CarCongestion(
        carNo = carNo,
        congestionLevel = congestionLevel
    )

fun DoorCongestion.toDomain(): DoorCongestion =
    DoorCongestion(
        carNo = carNo,
        doorNo = doorNo,
        congestionLevel = congestionLevel
    )

fun BoardingPosition.toDomain(): BoardingPosition =
    BoardingPosition(
        carNo = carNo,
        doorNo = doorNo
    )

