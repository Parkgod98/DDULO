package com.example.ddulo.ui.component.routeDetail

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.ddulo.data.response.StationDetailResponse
import com.example.ddulo.data.response.StationResult
import com.example.ddulo.ui.component.congestion.CongestionDiagram

private val SectionPaddingVertical = 12.dp

@Composable
fun StationInfoContent(
    station: StationResult,
    stationDetail: StationDetailResponse?,
    elapsedSec: Int = 0
) {
    val firstResult = station.resultsOrEmpty.firstOrNull() ?: return
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = Color.Transparent
    ) {
        Column(modifier = Modifier.padding(vertical = SectionPaddingVertical)) {
            CongestionDiagram(
                stationCongestionResult = firstResult,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(end = 12.dp)
            )

            stationDetail?.let { detail ->
                val bounds = selectBoundsForRoute(detail, station.nextStationName)
                Spacer(Modifier.height(8.dp))
                IncomingTrainInfo(
                    bounds = bounds,
                    best = firstResult.bestBoardingsOrEmpty,
                    comfort = firstResult.comfortBoardingOrEmpty,
                    elapsedSec = elapsedSec
                )
            }
        }
    }
}
