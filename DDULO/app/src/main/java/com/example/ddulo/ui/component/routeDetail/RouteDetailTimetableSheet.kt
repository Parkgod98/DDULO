package com.example.ddulo.ui.component.routeDetail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ddulo.data.response.StationDetailResponse
import com.example.ddulo.data.response.TrainBound
import com.example.ddulo.ui.util.formatArrivalTime

/**
 * 경로 상세 화면 '시간표' 버튼 → 바텀시트 내용.
 * 이미지 참고: 해당 역 도착 열차를 "도착 시각(HH:mm) + 목적지" 표로 상행/하행 두 칸 나열.
 * (API는 실시간 도착 정보만 제공하므로, 현재 시각 + arrivalSec으로 도착 예정 시각 계산)
 */
@Composable
fun TimetableBottomSheetContent(
    detail: StationDetailResponse,
    modifier: Modifier = Modifier
) {
    val station = detail.station
    val title = "${station.stationName} ${station.lineName}"
    val upBound = detail.upBound
    val downBound = detail.downBound
    val rowCount = maxOf(upBound.size, downBound.size)

    Column(modifier = modifier.fillMaxSize()) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
        )
        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

        // 테이블 헤더: 상행 | 하행
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            Text(
                text = "상행",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            HorizontalDivider(
                modifier = Modifier
                    .width(1.dp)
                    .height(20.dp)
                    .padding(vertical = 4.dp),
                color = MaterialTheme.colorScheme.outlineVariant
            )
            Text(
                text = "하행",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
        }

        if (rowCount == 0) {
            Text(
                text = "도착 예정 열차 정보가 없습니다.",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(24.dp)
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                itemsIndexed(List(rowCount) { it }) { index, _ ->
                    val up = upBound.getOrNull(index)
                    val down = downBound.getOrNull(index)
                    val isFirstRow = index == 0
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .then(
                                if (isFirstRow) Modifier.background(Color(0xFFE3F2FD))
                                else Modifier
                            )
                            .padding(horizontal = 12.dp, vertical = 10.dp)
                    ) {
                        TimetableCell(
                            train = up,
                            modifier = Modifier.weight(1f)
                        )
                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .height(36.dp)
                                .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        )
                        TimetableCell(
                            train = down,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    if (index < rowCount - 1) {
                        HorizontalDivider(
                            thickness = 0.5.dp,
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TimetableCell(
    train: TrainBound?,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = Color.Transparent
    ) {
        if (train == null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Text(
                    text = "-",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = formatArrivalTime(train.arrivalSec),
                    style = MaterialTheme.typography.titleSmall.copy(fontSize = 14.sp),
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = train.destination,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
