package com.example.ddulo.ui.preview

import com.example.ddulo.data.response.BoardingPosition
import com.example.ddulo.data.response.CarCongestion
import com.example.ddulo.data.response.DoorCongestion
import com.example.ddulo.data.response.StationCongestionResult

object FakeCongestionPreviewData {
    fun create(): StationCongestionResult {
        return StationCongestionResult(
            carCongestions = (1..10).map {
                CarCongestion(it, (it * 10).coerceIn(0, 100))
            },
            stationCongestions = (1..10).flatMap { carNo ->
                listOf(
                    DoorCongestion(carNo, 1, 50),
                    DoorCongestion(carNo, 2, 40)
                )
            },
            bestBoardings = listOf(BoardingPosition(7, 1)),
            comfortBoarding = listOf(BoardingPosition(5, 3))
        )
    }
}
