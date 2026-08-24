package com.example.ddulo.ui.component.routeDetail

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ddulo.data.response.BoardingPosition
import com.example.ddulo.data.response.CarCongestion
import com.example.ddulo.data.response.StationDetailResponse
import com.example.ddulo.data.response.TrainBound
import com.example.ddulo.ui.util.formatArrivalSec

fun summarizeCongestionLabel(carCongestions: List<CarCongestion>): String {
    if (carCongestions.isEmpty()) return "정보없음"
    val avg = carCongestions.map { it.congestionLevel }.average()
    return when {
        avg <= 30 -> "여유"
        avg <= 60 -> "보통"
        else -> "혼잡"
    }
}

fun ensureThreeBounds(bounds: List<TrainBound>): List<TrainBound> {
    if (bounds.isEmpty()) {
        val dummy = TrainBound(
            direction = "-",
            arrivalSec = 0,
            currentStation = "-",
            destination = "-",
            isBoardable = false,
            carCongestions = emptyList()
        )
        return listOf(dummy, dummy, dummy)
    }
    if (bounds.size >= 3) return bounds.take(3)
    val first = bounds.first()
    return buildList {
        addAll(bounds)
        while (size < 3) add(first)
    }
}

fun formatBoardingPositions(positions: List<BoardingPosition>): String =
    positions.joinToString(", ") { "${it.carNo}-${it.doorNo}" }

fun selectBoundsForRoute(detail: StationDetailResponse, nextStationName: String?): List<TrainBound> {
    if (nextStationName.isNullOrBlank()) return detail.downBound
    fun matches(bound: TrainBound): Boolean =
        bound.destination.contains(nextStationName) || bound.direction.contains(nextStationName)

    val upMatched = detail.upBound.filter(::matches)
    if (upMatched.isNotEmpty()) return upMatched
    val downMatched = detail.downBound.filter(::matches)
    if (downMatched.isNotEmpty()) return downMatched
    return detail.downBound.ifEmpty { detail.upBound }
}

@Composable
fun TrainArrivalInfoRow(
    destination: String,
    carCongestions: List<CarCongestion>,
    arrivalSec: Int,
    isBoardable: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = destination,
            modifier = Modifier.widthIn(max = 120.dp),
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Surface(
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {
            Text(
                text = summarizeCongestionLabel(carCongestions),
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }

        Text(
            text = formatArrivalSec(arrivalSec),
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1
        )

        Text(
            text = if (isBoardable) "탑승가능" else "탑승불가",
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
            color = if (isBoardable) Color(0xFF3F51FF) else Color(0xFFFF5252),
            fontWeight = FontWeight.Medium,
            maxLines = 1
        )
    }
}

@Composable
fun ThreeTrainArrivals(
    bounds: List<TrainBound>,
    elapsedSec: Int = 0,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        ensureThreeBounds(bounds).forEach { train ->
            TrainArrivalInfoRow(
                destination = train.destination,
                carCongestions = train.carCongestions,
                arrivalSec = (train.arrivalSec - elapsedSec).coerceAtLeast(0),
                isBoardable = train.isBoardable
            )
        }
    }
}

@Composable
fun BoardingGateHints(
    best: List<BoardingPosition>,
    comfort: List<BoardingPosition>,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        if (best.isNotEmpty()) {
            Text(
                text = "베스트 탑승 ${formatBoardingPositions(best)}",
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 8.sp),
                color = Color(0xFF4CE5B1)
            )
        }
        if (comfort.isNotEmpty()) {
            Text(
                text = "쾌적한 탑승 ${formatBoardingPositions(comfort)}",
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 8.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun IncomingTrainInfo(
    bounds: List<TrainBound>,
    best: List<BoardingPosition>,
    comfort: List<BoardingPosition>,
    elapsedSec: Int = 0,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Column(modifier = Modifier.weight(1.35f)) {
            ThreeTrainArrivals(bounds = bounds, elapsedSec = elapsedSec)
        }
        Column(modifier = Modifier.weight(0.65f)) {
            BoardingGateHints(best = best, comfort = comfort)
        }
    }
}
