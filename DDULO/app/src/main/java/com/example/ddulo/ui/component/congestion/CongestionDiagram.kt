package com.example.ddulo.ui.component.congestion

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.*
import com.example.ddulo.data.response.StationCongestionResult
import com.example.ddulo.ui.preview.FakeCongestionPreviewData

/**
 * 순수 혼잡도 시각화 컴포넌트. 타임라인 원/세로선/Divider를 포함하지 않으며,
 * RouteTimelineRow의 right(우측 콘텐츠 영역)에서만 사용한다.
 * width/레이아웃은 외부 modifier로만 제어하며, 내부에서 fillMaxWidth(0.x) 등 추가 제약을 두지 않는다.
 */
@Composable
fun CongestionDiagram(
    stationCongestionResult: StationCongestionResult,
    modifier: Modifier = Modifier,
    numberOfCars: Int = 10,
    trainFrontCornerRadius: Dp = 6.dp
) {
    val (trainCongestion, platformCongestion) = stationCongestionResult.toTrainAndPlatformLists(numberOfCars)
    CongestionDiagram(
        trainCongestion = trainCongestion,
        platformCongestion = platformCongestion,
        modifier = modifier,
        trainFrontCornerRadius = trainFrontCornerRadius
    )
}

/** 내부 오버로드: 열차 칸/플랫폼 리스트 직접 전달. 레이아웃은 modifier에만 의존. */
@Composable
fun CongestionDiagram(
    trainCongestion: List<Int>,
    platformCongestion: List<List<Int>>,
    modifier: Modifier = Modifier,
    /** 사다리꼴에서 비스듬한 변이 만나는 꼭짓점(왼쪽 위·왼쪽 아래)의 라운드 반경. 0.dp면 각짐. */
    trainFrontCornerRadius: Dp = 6.dp
) {
    val numberOfCars = trainCongestion.size
    val density = LocalDensity.current
    val slantPx = with(density) { 10.dp.toPx() }   // 열차 선두 사다리꼴 기울기
    val cornerRadiusPx = with(density) { trainFrontCornerRadius.toPx() }

    val roundedShape = RoundedCornerShape(6.dp)

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        // ─── 상단: 열차 칸 (1번 = 사다리꼴+비스듬한 변 꼭짓점 라운드, 2~10 = 둥근 정사각형, 칸 사이 간격 없음)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(0.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            for (index in 0 until numberOfCars) {
                val congestion = trainCongestion[index]
                val carColor = getTrainCongestionColor(congestion)
                if (index == 0) {
                    // 1번 칸: 사다리꼴 + 비스듬한 변 꼭짓점 라운드 (반경 조절: trainFrontCornerRadius)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val w = size.width
                            val h = size.height
                            val r = cornerRadiusPx.coerceIn(0f, minOf(slantPx, h, w) * 0.5f)
                            val maxSlant = minOf(slantPx, h, w) * 0.5f
                            val rSlant = (r * 1.25f).coerceIn(0f, maxSlant)  // 빗변 끼는 두 각 라운드 살짝 증가
                            val path = Path().apply {
                                moveTo(slantPx + rSlant, 0f)
                                lineTo(w - r, 0f)
                                // 오른쪽 위 꼭짓점 (다른 칸과 동일 라운드)
                                arcTo(
                                    Rect(w - 2 * r, 0f, w, 2 * r),
                                    270f,
                                    90f,
                                    forceMoveTo = false
                                )
                                lineTo(w, h - r)
                                // 오른쪽 아래 꼭짓점 (다른 칸과 동일 라운드)
                                arcTo(
                                    Rect(w - 2 * r, h - 2 * r, w, h),
                                    0f,
                                    90f,
                                    forceMoveTo = false
                                )
                                lineTo(rSlant, h)
                                // 왼쪽 아래 꼭짓점 라운드 (빗변 끼는 각)
                                arcTo(
                                    Rect(0f, h - 2 * rSlant, 2 * rSlant, h),
                                    90f,
                                    90f,
                                    forceMoveTo = false
                                )
                                // 빗변을 호(곡선)로 - 열차 선두처럼 불룩한 곡선
                                quadraticBezierTo(
                                    0f, h * 0.5f,  // 제어점: 선두부 곡률
                                    slantPx, rSlant
                                )
                                // 좌상단: 볼록하고 부드럽게 연결 (찌그러짐 없는 안정적 곡선)
                                cubicTo(
                                    slantPx + rSlant * 0.38f, rSlant * 0.58f,   // 제어점1: 볼록한 곡선
                                    slantPx + rSlant * 0.82f, rSlant * 0.04f,   // 제어점2: 윗변 부드럽게 연결
                                    slantPx + rSlant, 0f
                                )
                                close()
                            }
                            drawPath(path, carColor, style = Fill)
                        }
                        Text(
                            text = "1",
                            color = Color.White,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .clip(roundedShape)
                            .background(carColor),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = (index + 1).toString(),
                            color = Color.White,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }

        // ─── 하단: 플랫폼 (열차와 간격 없이 붙임, 차량 간 간격 없음)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(0.dp),
            verticalAlignment = Alignment.Top
        ) {
            for (index in 0 until numberOfCars) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 3.dp),  // 첫번째 플랫폼혼잡도 오른쪽 이동
                    horizontalArrangement = Arrangement.spacedBy((-4).dp)  // 차량 내 평행사변형 간격
                ) {
                    if (platformCongestion.size > index) {
                        platformCongestion[index].forEach { gateCongestion ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Canvas(modifier = Modifier.fillMaxSize()) {
                                    val w = size.width
                                    val h = size.height
                                    val s = h.coerceAtMost(w * 0.5f)  // 가로길이 확보 (이전과 동일)
                                    // 기본 평행사변형: 우하향 45도 (라운드 없음)
                                    val path = Path().apply {
                                        moveTo(0f, 0f)
                                        lineTo(w - s, 0f)
                                        lineTo(w, h)
                                        lineTo(s, h)
                                        close()
                                    }
                                    drawPath(
                                        path,
                                        getPlatformCongestionColor(gateCongestion),
                                        style = Fill
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun CongestionDiagramPreview() {
    Box(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        CongestionDiagram(
            trainCongestion = listOf(10, 20, 30, 80, 50, 60, 70, 40),
            platformCongestion = List(8) { listOf(it * 12, 100 - it * 12, (it + 1) * 15, 100 - (it + 1) * 15).map { c -> c.coerceIn(0, 100) } },
            modifier = Modifier.fillMaxWidth(0.8f)
        )
    }
}

@Preview(showBackground = true)
@Composable
fun CongestionDiagramPreview10() {
    Box(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        CongestionDiagram(
            trainCongestion = listOf(10, 20, 30, 80, 50, 60, 70, 40, 90, 100),
            platformCongestion = List(10) { listOf(it * 10, 100 - it * 10, (it + 1) * 8, 100 - (it + 1) * 8).map { c -> c.coerceIn(0, 100) } },
            modifier = Modifier.fillMaxWidth(0.8f)
        )
    }
}

@Preview(showBackground = true)
@Composable
fun CongestionDiagramFromApiPreview() {
    val stationCongestionResult = FakeCongestionPreviewData.create()
    Box(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        CongestionDiagram(
            stationCongestionResult = stationCongestionResult,
            modifier = Modifier.fillMaxWidth(0.8f)
        )
    }
}
