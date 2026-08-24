package com.example.ddulo.ui.component.routeList

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ddulo.data.local.entity.FavoriteType
import com.example.ddulo.data.response.PathPredictionResponse
import com.example.ddulo.data.response.RouteResultResponse
import com.example.ddulo.domain.model.FavoriteRoute
import com.example.ddulo.domain.model.RouteResult
import com.example.ddulo.viewmodel.FavoriteViewModel
import com.google.gson.Gson
import java.util.concurrent.TimeUnit

@Composable
fun RouteCard(
    route: RouteResult,
    pred: PathPredictionResponse,
    detail: RouteResultResponse,
    favoriteViewModel: FavoriteViewModel,
    onClick: () -> Unit
) {
    val favorites by favoriteViewModel.favorites.collectAsState(initial = emptyList())

    // ⭐️ 출발역/도착역 이름을 기준으로 즐겨찾기 여부 확인
    val startStation = route.legs.firstOrNull()?.startStation ?: ""
    val endStation = route.legs.lastOrNull()?.endStation ?: ""

    val favoriteEntity = favorites.find { fav ->
        if (fav.type != FavoriteType.ROUTE) return@find false
        try {
            val favRoute = Gson().fromJson(fav.payload, FavoriteRoute::class.java)
            favRoute.departureName == startStation && favRoute.destinationName == endStation
        } catch (e: Exception) {
            false
        }
    }
    val isFavorite = favoriteEntity != null

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            RouteCardHeader(
                route = route,
                pred = pred,
                isFavorite = isFavorite,
                onFavoriteClick = {
                    if (isFavorite) {
                        favoriteViewModel.removeFavoriteById(favoriteEntity!!.id)
                    } else {
                        // ⭐️ 경량화된 모델로 저장
                        favoriteViewModel.addRouteFavorite(route)
                    }
                }
            )
            RouteTimelineBar(legs = route.legs)
            RouteStationList(legs = route.legs)
        }
    }
}

@Composable
private fun RouteCardHeader(
    route: RouteResult,
    pred: PathPredictionResponse?,
    isFavorite: Boolean,
    onFavoriteClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // 총 소요시간
            Text(
                text = "${TimeUnit.SECONDS.toMinutes(route.totalTime.toLong())}분",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.ExtraBold
            )

            if (pred != null && pred.predictions.isNotEmpty()) {
                // 첫 번째 줄: 무조건 0번 인덱스
                val firstPred = pred.predictions[0]
                Text(
                    text = "${formatTime(firstPred.estimatedBoardingTime)} - ${formatTime(firstPred.estimatedArrivalTime)} | 대기시간 ${firstPred.waitingTimeSeconds / 60}분",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // 두 번째 줄: 1번 인덱스 확인 후 조건부 처리
                if (pred.predictions.size > 1) {
                    val secondPred = pred.predictions[1]
                    val finalSecondPred = if (secondPred.boardingProbability != 0) {
                        secondPred
                    } else if (pred.predictions.size > 2) {
                        pred.predictions[2]
                    } else {
                        null
                    }

                    finalSecondPred?.let { p ->
                        Text(
                            text = "${formatTime(p.estimatedBoardingTime)} - ${formatTime(p.estimatedArrivalTime)} | 대기시간 ${p.waitingTimeSeconds / 60}분 (탑승가능 ${p.boardingProbability}%)",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Red
                        )
                    }
                }
            }
        }

        // 즐겨찾기 버튼
        IconButton(onClick = onFavoriteClick) {
            Icon(
                imageVector = if (isFavorite) Icons.Filled.Star else Icons.Outlined.StarBorder,
                contentDescription = "즐겨찾기",
                tint = if (isFavorite) Color(0xFF00C775) else MaterialTheme.colorScheme.outline,
                modifier = Modifier.size(32.dp)
            )
        }
    }
}

private fun formatTime(timeStr: String): String {
    return try {
        if (timeStr.contains("T")) {
            timeStr.substringAfter("T").substring(0, 5)
        } else {
            timeStr
        }
    } catch (e: Exception) {
        timeStr
    }
}
