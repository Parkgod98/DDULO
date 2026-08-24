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
import com.example.ddulo.data.response.StationDetailResponse
import com.example.ddulo.data.response.StationResult

private val SectionPaddingVertical = 12.dp

@Composable
fun StationHeaderOnly(
    station: StationResult,
    segmentType: SegmentType,
    stationDetail: StationDetailResponse? = null,
    onTimetableClick: (StationDetailResponse) -> Unit = {}
) {
    val isDisembark = segmentType == SegmentType.DISEMBARK
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = Color.Transparent
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = SectionPaddingVertical)
                .padding(end = 12.dp),  // 혼잡도 다이어그램 오른쪽(end 12.dp)과 맞춤
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
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
                    style = MaterialTheme.typography.titleMedium.copy(fontSize = 16.sp),
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (!isDisembark) {
                TimetableButton(
                    onClick = { stationDetail?.let { onTimetableClick(it) } },
                    enabled = stationDetail != null
                )
            }
        }
    }
}
