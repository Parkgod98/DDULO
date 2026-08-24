package com.example.ddulo.ui.component.routeList

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ddulo.domain.model.RouteLeg
import com.example.ddulo.ui.util.getLineColor

@Composable
fun RouteStationList(legs: List<RouteLeg>) {
    val pathEffect = remember { PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f) }

    // 전체 리스트를 감싸는 Column에 점선을 그려서 연속성을 확보합니다.
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .drawBehind {
                val badgeRadius = 12.dp.toPx() // Badge size(24) / 2
                val lineX = 16.dp.toPx()      // Box width(32) / 2

                drawLine(
                    color = Color.LightGray,
                    start = Offset(lineX, badgeRadius),
                    end = Offset(lineX, size.height - 10.dp.toPx()), // 마지막 점(radius 4)까지
                    strokeWidth = 2f,
                    pathEffect = pathEffect
                )
            },
        verticalArrangement = Arrangement.spacedBy(20.dp) // ✅ 요소 간 거리 확대
    ) {
        legs.forEachIndexed { index, leg ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.width(32.dp)) {
                    LineBadge(lineName = leg.lineName, size = 24.dp)
                }

                Spacer(Modifier.width(12.dp))
                Text(
                    text = leg.startStation,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = leg.lineName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // 마지막 도착역 표시
        val lastLeg = legs.last()
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.width(32.dp)) {
                Surface(
                    modifier = Modifier.size(8.dp),
                    color = Color.LightGray,
                    shape = CircleShape
                ) {}
            }
            Spacer(Modifier.width(12.dp))
            Text(
                text = lastLeg.endStation,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun LineBadge(lineName: String, size: Dp) {
    val legColor = getLineColor(lineName)
    Box(
        modifier = Modifier
            .size(size)
            .background(Color.White, CircleShape) // ✅ 모서리 투명화: 배경도 원형으로 제한
            .border(2.dp, legColor, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = lineName[0].toString(),
            color = legColor,
            fontSize = (size.value / 2.2).sp,
            fontWeight = FontWeight.ExtraBold,
            style = LocalTextStyle.current.copy(
                platformStyle = PlatformTextStyle(
                    includeFontPadding = false // 수직 중앙 정렬 최적화
                )
            )
        )
    }
}