package com.example.ddulo.ui.component.station

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.ddulo.data.local.entity.FavoriteType
import com.example.ddulo.data.response.AdjacentStation
import com.example.ddulo.data.response.CarCongestion
import com.example.ddulo.data.response.StationDetailResponse
import com.example.ddulo.data.response.StationInfo
import com.example.ddulo.data.response.TrainBound
import com.example.ddulo.domain.model.Station
import com.example.ddulo.ui.util.getLineColor
import com.example.ddulo.viewmodel.FavoriteViewModel
import com.example.ddulo.viewmodel.StationDetailViewModel
import com.example.ddulo.viewmodel.state.StationDetailState
import com.google.gson.Gson

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StationSheetContent(
    stationGroup: List<Station>,
    stationDetailViewModel: StationDetailViewModel,
    favoriteViewModel: FavoriteViewModel,
    onSetDeparture: (Station) -> Unit,
    onSetDestination: (Station) -> Unit,
    isExpanded: Boolean
) {
    val favorites by favoriteViewModel.favorites.collectAsState(initial = emptyList())
    val favoriteEntity = favorites.find { fav ->
        fav.type == FavoriteType.STATION &&
                Gson().fromJson(fav.payload, Array<Station>::class.java).toList().map { it.stationId } ==
                stationGroup.map { it.stationId }
    }
    val isFavorite = favoriteEntity != null

    val baseStation = remember(stationGroup) { stationGroup.firstOrNull() }
    val stationTabs = remember(stationGroup) { stationGroup.distinctBy { it.stationId } }
    val currentStation = remember(stationTabs, stationDetailViewModel.selectedStationId) {
        stationTabs.find { it.stationId == stationDetailViewModel.selectedStationId } ?: baseStation
    }

    LaunchedEffect(stationTabs) {
        stationTabs.firstOrNull()?.let { stationDetailViewModel.selectStation(it.stationId) }
    }

    if (baseStation == null || stationGroup.isEmpty()) return

    val screenHeight = LocalConfiguration.current.screenHeightDp.dp

    val sheetHeight = if (isExpanded) {
        screenHeight * 1f
    } else {
        screenHeight * 0.75f - 44.dp
    }

    // 🔑 핵심 구조: Box(fillMaxHeight)로 시트 전체 영역을 잡고 푸터를 하단에 고정
    Box(
        modifier = Modifier
            .height(sheetHeight)
            .fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 100.dp) // 푸터 높이만큼 바닥 여백 확보
        ) {
            // 1. 헤더 (고정)
            StationSheetHeader(
                stationName = baseStation.name,
                stationTabs = stationTabs,
                selectedStationId = stationDetailViewModel.selectedStationId,
                onStationSelect = { stationDetailViewModel.selectStation(it) }
            )

            HorizontalDivider(thickness = 0.5.dp)

            // 2. 가변 컨텐츠 영역 (weight 1f)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(
                        if (isExpanded) Modifier.weight(1f)
                        else Modifier.wrapContentHeight()
                    )
            ) {
                when (val state = stationDetailViewModel.state) {
                    is StationDetailState.Loading -> {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }
                    is StationDetailState.Error -> {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(state.message, color = MaterialTheme.colorScheme.error)
                        }
                    }
                    is StationDetailState.Success -> {
                        val detail = state.data
                        Column {
                            StationLineBanner(
                                lineName = detail.station.lineName,
                                stationName = detail.station.stationName,
                                prevStations = detail.station.prevStations,
                                nextStations = detail.station.nextStations
                            )
                            HorizontalDivider(thickness = 0.5.dp)

                            StationDetailContentBody(
                                detail = state.data,
//                                detail = previewStationDetail, // test 데이터
                                isExpanded = isExpanded
                            )
                        }
                    }
                    StationDetailState.Idle -> Unit
                }
            }
        }

        // 3. 푸터 (항상 시트 하단에 레이어로 고정)
        StationSheetFooter(
            modifier = Modifier.align(Alignment.BottomCenter),
            isFavorite = isFavorite,
            onFavoriteClick = {
                if (isFavorite) favoriteViewModel.removeFavoriteById(favoriteEntity!!.id)
                else favoriteViewModel.addStationFavorite(stationGroup)
            },
            onSetDeparture = { currentStation?.let { onSetDeparture(it) } },
            onSetDestination = { currentStation?.let { onSetDestination(it) } }
        )
    }
}

@Composable
fun StationSheetHeader(
    stationName: String,
    stationTabs: List<Station>,
    selectedStationId: String?,
    onStationSelect: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = stationName,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            stationTabs.forEach { s ->
                val isSelected = selectedStationId == s.stationId
                TabBadge(
                    lineName = s.lineName,
                    isSelected = isSelected,
                    onClick = { onStationSelect(s.stationId) }
                )
            }
        }
    }
}

@Composable
fun TabBadge(lineName: String, isSelected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.small,
        color = if (isSelected) getLineColor(lineName) else Color.Transparent,
        border = if (!isSelected) BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant) else null
    ) {
        Box(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
            Text(
                text = lineName,
                style = MaterialTheme.typography.labelMedium,
                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun StationDetailContentBody(
    detail: StationDetailResponse,
    isExpanded: Boolean
) {
    var selectedDirectionTabIndex by remember { mutableIntStateOf(0) }
    val directionTabs = listOf("상행 (외선)", "하행 (내선)")
    val currentBound = if (selectedDirectionTabIndex == 0) detail.upBound else detail.downBound

    // ✅ 상태에 따라 출력할 리스트 아이템 개수 제한 (Half일 때 1개)
    val displayItems = if (isExpanded) currentBound else currentBound.take(1)

    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(
            selectedTabIndex = selectedDirectionTabIndex,
            containerColor = Color.Transparent,
            divider = {}
        ) {
            directionTabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedDirectionTabIndex == index,
                    onClick = { selectedDirectionTabIndex = index },
                    text = { Text(title, style = MaterialTheme.typography.titleSmall) }
                )
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            userScrollEnabled = isExpanded
        ) {
            if (displayItems.isEmpty()) {
                item { Text("도착 정보가 없습니다.", modifier = Modifier.padding(16.dp)) }
            } else {
                items(displayItems) { train ->
                    TrainArrivalCard(train)
                }
            }
        }
    }
}

@Composable
fun StationSheetFooter(
    modifier: Modifier = Modifier,
    isFavorite: Boolean,
    onFavoriteClick: () -> Unit,
    onSetDeparture: () -> Unit,
    onSetDestination: () -> Unit
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(100.dp),
        shadowElevation = 12.dp,
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onSetDeparture,
                    shape = MaterialTheme.shapes.medium,
                    contentPadding = PaddingValues(horizontal = 24.dp)
                ) {
                    Text("출발", fontWeight = FontWeight.Bold)
                }
                OutlinedButton(
                    onClick = onSetDestination,
                    shape = MaterialTheme.shapes.medium,
                    contentPadding = PaddingValues(horizontal = 24.dp)
                ) {
                    Text("도착", fontWeight = FontWeight.Bold)
                }
            }

            IconButton(onClick = onFavoriteClick) {
                Icon(
                    imageVector = if (isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                    contentDescription = "즐겨찾기",
                    tint = if (isFavorite) Color(0xFF00C775) else MaterialTheme.colorScheme.outline,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}

// ========================== 프리뷰 =========================
val previewStation = Station(
    stationId = "101010",
    stationName = "강남",
    lineName = "2호선",
    latitude = 37.4979,
    longitude = 127.0276,
    distanceMeters = 120.0
)

val previewPrevStations = listOf(
    AdjacentStation(
        stationCode = "100201",
        stationName = "역삼"
    )
)

val previewNextStations = listOf(
    AdjacentStation(
        stationCode = "100203",
        stationName = "교대"
    )
)


val previewStationGroup = listOf(
    previewStation
)

val previewTrain = TrainBound(
    direction = "상행",
    arrivalSec = 180,
    currentStation = "역삼",
    destination = "홍대입구",
    isBoardable = true,
    carCongestions = List(10) { index ->
        CarCongestion(carNo = index + 1, congestionLevel = (index + 1) * 10)
        // 1번칸 10%, 2번칸 20%, … 10번칸 100%
    }
)

val previewStationDetail = StationDetailResponse(
    station = StationInfo(
        stationCode = "101010",
        stationName = "강남",
        lineName = "2호선",
        prevStations = previewPrevStations,
        nextStations = previewNextStations
    ),
    upBound = listOf(
        previewTrain,
        previewTrain.copy(arrivalSec = 420),
        previewTrain.copy(arrivalSec = 820)
    ),
    downBound = emptyList()
)

@Preview(showBackground = true, widthDp = 360)
@Composable
fun TrainArrivalCardPreview() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 예시 1: 기본 previewTrain
        TrainArrivalCard(previewTrain)

        // 예시 2: 도착 시간 변경 + 혼잡도 다르게
        TrainArrivalCard(
            previewTrain.copy(
                arrivalSec = 90,
                carCongestions = List(10) { index ->
                    CarCongestion(carNo = index + 1, congestionLevel = (10 - index) * 10)
                },
                destination = "서울대입구"
            )
        )

        // 예시 3: 곧 도착
        TrainArrivalCard(
            previewTrain.copy(
                arrivalSec = 30,
                carCongestions = List(8) { index ->
                    CarCongestion(carNo = index + 1, congestionLevel = (10 - index) * 10)
                },
                destination = "잠실"
            )
        )
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 600)
@Composable
fun StationLineBannerPreview() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 예시 1: 강남역, 2호선
        StationLineBanner(
            lineName = "2호선",
            stationName = "강남",
            prevStations = previewPrevStations,
            nextStations = previewNextStations
        )

        // 예시 2: 잠실역, 8호선
        StationLineBanner(
            lineName = "8호선",
            stationName = "잠실",
            prevStations = previewPrevStations,
            nextStations = previewNextStations
        )

        // 예시 3: 홍대입구역, 2호선
        StationLineBanner(
            lineName = "2호선",
            stationName = "홍대입구",
            prevStations = previewPrevStations,
            nextStations = previewNextStations
        )
    }
}

