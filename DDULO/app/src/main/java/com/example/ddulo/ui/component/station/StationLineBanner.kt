package com.example.ddulo.ui.component.station

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ddulo.data.response.AdjacentStation
import com.example.ddulo.ui.util.getLineColor

@Composable
fun StationLineBanner(
    lineName: String,
    stationName: String,
    prevStations: List<AdjacentStation> = emptyList(),
    nextStations: List<AdjacentStation> = emptyList()
) {
    val lineColor = getLineColor(lineName)
    val lineNumber = lineName[0].toString()

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 20.dp, horizontal = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        // 1. 배경 노선 띠
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(20.dp),
            color = lineColor,
            shape = CircleShape
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // 이전 역
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Text(
                        text =  "‹ ",
                        color = Color.White,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 1.sp
                    )
                    Text(
                        text = prevStations.joinToString("/") { it.stationName },
                        color = Color.White,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // 중앙 캡슐 공간 확보용 스페이서
                Spacer(modifier = Modifier.width(140.dp))

                // 이후 역
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.End
                ) {
                    Text(
                        text = nextStations.joinToString("/") { it.stationName },
                        color = Color.White,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.End
                    )
                    Text(
                        text = " ›",
                        color = Color.White,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 1.sp
                    )
                }
            }
        }

        // 2. 중앙 역 이름 캡슐
        Surface(
            modifier = Modifier
                .wrapContentWidth()
                .height(40.dp),
            color = Color.White,
            shape = CircleShape,
            border = BorderStroke(4.dp, lineColor)
        ) {
            Row(
                modifier = Modifier.padding(start = 10.dp, end = 22.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // 호선 번호 원형 배지
                Surface(
                    modifier = Modifier.size(26.dp),
                    color = lineColor,
                    shape = CircleShape
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = lineNumber,
                            color = Color.White,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // 현재 역 이름
                Text(
                    text = stationName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1F2937) // 다크 그레이
                )
            }
        }
    }
}
