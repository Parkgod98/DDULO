package com.example.ddulo.ui.component.routeList

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ddulo.domain.model.RouteLeg
import com.example.ddulo.ui.util.getLineColor
import java.util.concurrent.TimeUnit

@Composable
fun RouteTimelineBar(legs: List<RouteLeg>) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(24.dp) // 바의 높이를 조절
            .clip(RoundedCornerShape(4.dp)), // 가장자리를 조금만 깎음
        verticalAlignment = Alignment.CenterVertically
    ) {
        legs.forEachIndexed { index, leg ->
            val legColor = getLineColor(leg.lineName)
            Box(
                modifier = Modifier
                    .weight(leg.sectionTime.toFloat())
                    .fillMaxHeight()
                    .background(legColor)
            ) {
                // 소요 시간 표시 (겹침 방지를 위해 시작 부분에 패딩 추가)
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(start = 22.dp), // 뱃지 너비(18) + 여백(4)
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "${TimeUnit.SECONDS.toMinutes(leg.sectionTime.toLong())}분",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // 호선 정보 뱃지 (시작 부분에 위치)
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(start = 2.dp)
                        .size(18.dp)
                        .background(Color.White, RoundedCornerShape(2.dp)), // 약간 각진 뱃지
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = leg.lineName[0].toString(),
                        color = legColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        lineHeight = 18.sp,
                    )
                }
            }

            // 환승 지점 구분선
            if (index < legs.size - 1) {
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .fillMaxHeight()
                        .background(Color.White.copy(alpha = 0.3f))
                )
            }
        }
    }
}