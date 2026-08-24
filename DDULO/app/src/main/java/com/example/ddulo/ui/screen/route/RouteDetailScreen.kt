package com.example.ddulo.ui.screen.route

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.ddulo.data.response.*
import com.example.ddulo.ui.component.congestion.CongestionDiagram
import com.example.ddulo.ui.component.routeDetail.RouteDetailHeader
import com.example.ddulo.ui.component.routeDetail.RouteDetailMainCard
import com.example.ddulo.ui.component.routeDetail.SegmentType
import com.example.ddulo.ui.theme.DDULOTheme
import com.example.ddulo.viewmodel.RouteViewModel
import com.example.ddulo.viewmodel.SharedViewModel
import com.example.ddulo.viewmodel.state.RouteSearchState
import kotlinx.coroutines.delay

private val RouteDetailBackgroundColor = Color(0xFF4CE5B1)
private val ScreenPaddingHorizontal = 16.dp
private val ScreenPaddingVertical = 8.dp
private val MainCardTopRadius = 16.dp
private val MainCardBottomMargin = 24.dp
private val SectionPaddingVertical = 12.dp

/**
 * 경로 상세 화면
 */
@Composable
fun RouteDetailScreen(
    sharedViewModel: SharedViewModel,
    routeViewModel: RouteViewModel,
    navController: NavController
) {
    val routeDetail = sharedViewModel.selectedRouteDetail
    val routeState = routeViewModel.state

    LaunchedEffect(routeDetail) {
        if (routeDetail == null) {
            navController.popBackStack()
        }
    }

    if (routeDetail == null) {
        Box(
            modifier = Modifier.fillMaxSize().background(RouteDetailBackgroundColor),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = Color.White)
        }
    } else {
        RouteDetailContent(
            data = routeDetail,
            routeViewModel = routeViewModel,
            routeState = routeState,
            onBack = { navController.popBackStack() }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RouteDetailContent(
    data: RouteResultResponse,
    routeViewModel: RouteViewModel,
    routeState: RouteSearchState,
    onBack: () -> Unit
) {
    val startStation = data.startStation.firstOrNull()
    val pathSummary = if (startStation != null) {
        "${startStation.stationName} ${startStation.lineName} \u2192 ${data.endStation.stationName} ${data.endStation.lineName}"
    } else {
        "\u2192 ${data.endStation.stationName} ${data.endStation.lineName}"
    }
    val totalMinutes = data.totalTimeSecond / 60
    val scrollState = rememberScrollState()

    val isRefreshing = routeState is RouteSearchState.Loading
    val pullToRefreshState = rememberPullToRefreshState()

    // 실시간 시간 감소 로직
    var currentTime by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            currentTime = System.currentTimeMillis()
        }
    }

    // API 응답 시점 저장 (새로고침 시 업데이트됨)
    val fetchedAt = (routeState as? RouteSearchState.Success)?.fetchedAt ?: System.currentTimeMillis()
    val elapsedSec = ((currentTime - fetchedAt) / 1000).toInt()

    val displayData = remember(data, routeState, currentTime) {
        if (routeState is RouteSearchState.Success) {
            val res = routeState.routeResultResponse
            res.copy(
                startStation = res.startStation.map { it.copy(estimatedWaitingSec = (it.estimatedWaitingSec - elapsedSec).coerceAtLeast(0)) },
                transferStation = res.transferStation?.map { g -> g.copy(options = g.optionsOrEmpty.map { it.copy(estimatedWaitingSec = (it.estimatedWaitingSec - elapsedSec).coerceAtLeast(0)) }) }
            )
        } else {
            data
        }
    }

    // Scaffold 없이 Box 레이아웃을 사용하여 기존 UI 테마(색상) 보존
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(RouteDetailBackgroundColor)
    ) {
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = { routeViewModel.refresh() },
            state = pullToRefreshState,
            modifier = Modifier.fillMaxSize()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = ScreenPaddingVertical)
            ) {
                RouteDetailHeader(
                    totalMinutes = totalMinutes,
                    pathSummary = pathSummary,
                    onBack = onBack
                )

                Box(
                    modifier = Modifier
                        .weight(1f, fill = true)
                        .fillMaxWidth()
                        .padding(horizontal = ScreenPaddingHorizontal)
                        .padding(bottom = MainCardBottomMargin)
                ) {
                    RouteDetailMainCard(
                        data = displayData,
                        scrollState = scrollState,
                        fetchedAt = fetchedAt,
                        elapsedSec = elapsedSec
                    )
                }
            }
        }

        // 플로팅 버튼 (기존 UI 레이아웃과 독립적으로 우측 하단 배치)
        FloatingActionButton(
            onClick = { routeViewModel.refresh() },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp),
            containerColor = Color.White,
            contentColor = RouteDetailBackgroundColor,
            shape = CircleShape
        ) {
            Icon(Icons.Default.Refresh, contentDescription = "새로고침")
        }
    }
}

/**
 * 대체 경로 세그먼트 카드 (호선 라벨 + 역명 + 혼잡도).
 */
@Composable
private fun RouteSegmentCard(
    station: StationResult,
    segmentType: SegmentType,
    lineLabel: String?,
    showLineIndicatorInCard: Boolean = true
) {
    val firstResult = station.resultsOrEmpty.firstOrNull()
    val isDisembark = segmentType == SegmentType.DISEMBARK

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = Color.Transparent
    ) {
        Column(modifier = Modifier.padding(vertical = SectionPaddingVertical)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (showLineIndicatorInCard && lineLabel != null) {
                    Surface(
                        shape = CircleShape,
                        color = when (segmentType) {
                            SegmentType.BOARDING -> MaterialTheme.colorScheme.primary
                            SegmentType.TRANSFER -> MaterialTheme.colorScheme.tertiary
                            SegmentType.DISEMBARK -> MaterialTheme.colorScheme.secondary
                        }
                    ) {
                        Box(
                            modifier = Modifier.size(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = lineLabel,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }
                }
                if (showLineIndicatorInCard && segmentType == SegmentType.DISEMBARK) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Box(
                            modifier = Modifier.size(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "하차",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    val actionText = when (segmentType) {
                        SegmentType.BOARDING -> "승차"
                        SegmentType.TRANSFER -> "환승"
                        SegmentType.DISEMBARK -> "하차"
                    }
                    val subText = when (segmentType) {
                        SegmentType.BOARDING -> station.nextStationName?.let { "$it 방면" } ?: ""
                        SegmentType.TRANSFER -> station.nextStationName?.let { "$it 방면" } ?: ""
                        SegmentType.DISEMBARK -> ""
                    }
                    Text(
                        text = "${station.stationName} $actionText $subText".trim(),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium
                    )
                }
                if (!isDisembark) {
                    TextButton(onClick = { /* TODO: 시간표 */ }) {
                        Text("시간표", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }

            if (!isDisembark && firstResult != null) {
                Spacer(Modifier.height(SectionPaddingVertical))
                CongestionDiagram(
                    stationCongestionResult = firstResult,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 640)
@Composable
private fun RouteDetailContentPreview() {
    val sampleStartStation = StationResult(
        stationCode = "222",
        stationName = "강남",
        lineName = "2호선",
        nextStationName = "역삼",
        estimatedWaitingSec = 180,
        isBoardable = true,
        results = listOf(
            StationCongestionResult(
                carCongestions = (1..10).map { CarCongestion(it, (it * 8).coerceIn(0, 100)) },
                stationCongestions = (1..10).flatMap { carNo ->
                    listOf(
                        DoorCongestion(carNo, 1, (carNo * 6).coerceIn(0, 100)),
                        DoorCongestion(carNo, 2, (100 - carNo * 6).coerceIn(0, 100))
                    )
                },
                bestBoardings = listOf(BoardingPosition(7, 1), BoardingPosition(4, 1)),
                comfortBoarding = listOf(BoardingPosition(5, 3), BoardingPosition(5, 4))
            )
        )
    )
    val sampleEndStation = StationResult(
        stationCode = "227",
        stationName = "사당",
        lineName = "2호선",
        nextStationName = null,
        estimatedWaitingSec = 0,
        isBoardable = false,
        results = null
    )
    val sampleData = RouteResultResponse(
        totalTimeSecond = 1320,
        estimatedBoardingTime = "2025-02-06T14:30:00",
        boardingProbability = 85.0,
        startStation = listOf(sampleStartStation),
        transferStation = listOf(
            TransferStationGroup(
                transferOrder = 1,
                options = listOf(
                    StationResult(
                        stationCode = "226",
                        stationName = "신도림",
                        lineName = "2호선",
                        nextStationName = "대림",
                        estimatedWaitingSec = 120,
                        isBoardable = true,
                        results = listOf(
                            StationCongestionResult(
                                carCongestions = (1..10).map { CarCongestion(it, (it * 10).coerceIn(0, 100)) },
                                stationCongestions = (1..10).flatMap { carNo ->
                                    listOf(
                                        DoorCongestion(carNo, 1, (carNo * 5).coerceIn(0, 100)),
                                        DoorCongestion(carNo, 2, (100 - carNo * 5).coerceIn(0, 100))
                                    )
                                },
                                bestBoardings = listOf(BoardingPosition(3, 2)),
                                comfortBoarding = listOf(BoardingPosition(5, 1), BoardingPosition(6, 2))
                            )
                        )
                    )
                )
            )
        ),
        endStation = sampleEndStation
    )
    DDULOTheme {
        // Preview dummy context
    }
}
