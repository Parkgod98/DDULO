package com.example.ddulo.ui.component.routeDetail

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.ddulo.data.response.RouteResultResponse
import com.example.ddulo.data.response.StationDetailResponse
import com.example.ddulo.data.response.StationResult
import com.example.ddulo.data.response.TransferStationGroup
import com.example.ddulo.domain.repository.StationRepository
import com.example.ddulo.ui.util.lineColorFromLineName

private val RouteDetailMainBackgroundColor = Color.White
private val ScreenPaddingHorizontal = 16.dp
private val MainCardTopRadius = 16.dp
private val MainCardElevation = 4.dp
private val MainCardContentPaddingEnd = 8.dp
private val MainCardContentPaddingTop = 16.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RouteDetailMainCard(
    data: RouteResultResponse,
    scrollState: ScrollState,
    fetchedAt: Long = 0L,
    elapsedSec: Int = 0
) {
    val startStation = data.startStation.firstOrNull() ?: return
    val endStation = data.endStation

    val startLineColor = lineColorFromLineName(startStation.lineName)

    val stationRepository = remember { StationRepository() }
    val stationDetailCache = remember { mutableStateMapOf<String, StationDetailResponse>() }

    // 새로고침 시(fetchedAt 변경 시) 캐시 초기화
    LaunchedEffect(fetchedAt) {
        if (fetchedAt > 0) {
            stationDetailCache.clear()
        }
    }

    val stationCodesToLoad = remember(data) {
        buildList {
            add(startStation.stationCode)
            addAll(
                data.transferStationOrEmpty.flatMap { group ->
                    group.optionsOrEmpty.map { it.stationCode }
                }
            )
        }.distinct()
    }

    stationCodesToLoad.forEach { stationCode ->
        LaunchedEffect(stationCode, fetchedAt) {
            if (stationDetailCache.containsKey(stationCode)) return@LaunchedEffect
            runCatching { stationRepository.getStationDetail(stationCode) }
                .onSuccess { stationDetailCache[stationCode] = it }
        }
    }

    var showTimetableDetail by remember { mutableStateOf<StationDetailResponse?>(null) }

    if (showTimetableDetail != null) {
        ModalBottomSheet(
            onDismissRequest = { showTimetableDetail = null },
            modifier = Modifier.fillMaxHeight()
        ) {
            TimetableBottomSheetContent(
                detail = showTimetableDetail!!,
                modifier = Modifier.fillMaxSize()
            )
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(),
        shape = RoundedCornerShape(MainCardTopRadius),
        colors = CardDefaults.cardColors(containerColor = RouteDetailMainBackgroundColor),
        elevation = CardDefaults.cardElevation(defaultElevation = MainCardElevation)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .verticalScroll(scrollState)
                .padding(top = MainCardContentPaddingTop, start = ScreenPaddingHorizontal, end = MainCardContentPaddingEnd)
        ) {
            Box(modifier = Modifier.fillMaxWidth()) {
                BoardingRecommendationCard(
                    estimatedBoardingTime = data.estimatedBoardingTime,
                    boardingProbability = data.boardingProbability,
                    lineName = startStation.lineName,
                    nextStationName = startStation.nextStationName
                )
            }

            RecommendationSectionDivider()

            TimelineRow(
                left = { rowHeight ->
                    LineNumberComponent(
                        rowHeight = rowHeight,
                        lineLabel = startStation.lineName.replace(Regex("호선|선"), "").trim().take(1).ifEmpty { "2" },
                        topLineColor = TimelineLineColorNone,
                        bottomLineColor = startLineColor
                    )
                },
                right = {
                    StationHeaderOnly(
                        station = startStation,
                        segmentType = SegmentType.BOARDING,
                        stationDetail = stationDetailCache[startStation.stationCode],
                        onTimetableClick = { showTimetableDetail = it }
                    )
                }
            )
            TimelineRow(
                left = { rowHeight ->
                    LineLineComponent(rowHeight = rowHeight, lineColor = startLineColor)
                },
                right = {
                    StationInfoContent(
                        station = startStation,
                        stationDetail = stationDetailCache[startStation.stationCode],
                        elapsedSec = elapsedSec
                    )
                }
            )

            data.transferStationOrEmpty.forEachIndexed { index, group ->
                val transferStation = group.optionsOrEmpty.firstOrNull() ?: return@forEachIndexed
                val prevColor = if (index == 0) {
                    startLineColor
                } else {
                    val prevGroup = data.transferStationOrEmpty[index - 1]
                    val prevStation = prevGroup.optionsOrEmpty.firstOrNull()
                    prevStation?.let { lineColorFromLineName(it.lineName) } ?: startLineColor
                }
                val transferLineColor = lineColorFromLineName(transferStation.lineName)
                TimelineDivider(lineColor = prevColor)
                TimelineRow(
                    left = { rowHeight ->
                        LineNumberComponent(
                            rowHeight = rowHeight,
                            lineLabel = transferStation.lineName.replace(Regex("호선|선"), "").trim().take(1).ifEmpty { "${index + 2}" },
                            topLineColor = prevColor,
                            bottomLineColor = transferLineColor
                        )
                    },
                    right = {
                        StationHeaderOnly(
                            station = transferStation,
                            segmentType = SegmentType.TRANSFER,
                            stationDetail = stationDetailCache[transferStation.stationCode],
                            onTimetableClick = { showTimetableDetail = it }
                        )
                    }
                )
                TimelineRow(
                    left = { rowHeight ->
                        LineLineComponent(rowHeight = rowHeight, lineColor = transferLineColor)
                    },
                    right = {
                        StationInfoContent(
                            station = transferStation,
                            stationDetail = stationDetailCache[transferStation.stationCode],
                            elapsedSec = elapsedSec
                        )
                    }
                )
            }

            val lastTransferLineColor = if (data.transferStationOrEmpty.isEmpty()) {
                startLineColor
            } else {
                val lastGroup = data.transferStationOrEmpty.last()
                val lastStation = lastGroup.optionsOrEmpty.firstOrNull()
                lastStation?.let { lineColorFromLineName(it.lineName) } ?: startLineColor
            }
            TimelineDivider(lineColor = lastTransferLineColor)

            TimelineRow(
                left = { rowHeight ->
                    LineNumberComponent(
                        rowHeight = rowHeight,
                        lineLabel = "하차",
                        topLineColor = lastTransferLineColor,
                        bottomLineColor = TimelineLineColorNone
                    )
                },
                right = {
                    StationHeaderOnly(
                        station = endStation,
                        segmentType = SegmentType.DISEMBARK
                    )
                }
            )
        }
    }
}
