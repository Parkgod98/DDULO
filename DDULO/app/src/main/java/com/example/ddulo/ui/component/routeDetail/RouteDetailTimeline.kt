package com.example.ddulo.ui.component.routeDetail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

enum class SegmentType { BOARDING, TRANSFER, DISEMBARK }

private val TimelineNodeColor = Color(0xFF4CE5B1)
private val DetailCardDividerColor = Color(0xFFE0E0E0)
// 원이 정사각형으로 그려지려면 타임라인 영역 너비가 TimelineNodeSize 이상이어야 함 (이하일 경우 가로가 잘려 29×32 등으로 측정되어 둥근 사각형처럼 보일 수 있음)
private val TimelineWidth = 32.dp
private val TimelineNodeSize = 32.dp
private val TimelineLineWidth = 3.dp
private val TimelineContentGap = 12.dp
private val StationHeaderRowMinHeight = 48.dp
private val TimelineDividerRowHeight = 16.dp
private val RecommendationSectionDividerHeight = 16.dp
val TimelineLineColorNone = Color.White

@Composable
fun TimelineRow(
    left: @Composable (rowHeight: Dp) -> Unit,
    right: @Composable () -> Unit
) {
    val density = LocalDensity.current
    var rowHeightDp by remember { mutableStateOf(StationHeaderRowMinHeight) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .onSizeChanged { rowHeightDp = with(density) { it.height.toDp() } },
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .width(TimelineWidth)
                .height(rowHeightDp)
                .heightIn(min = StationHeaderRowMinHeight),
            contentAlignment = Alignment.Center
        ) {
            left(rowHeightDp)
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = TimelineContentGap)
        ) {
            right()
        }
    }
}

@Composable
fun TimelineSpacer() {
    Box(modifier = Modifier.width(TimelineWidth))
}

@Composable
fun LineNumberComponent(
    rowHeight: Dp,
    lineLabel: String,
    topLineColor: Color,
    bottomLineColor: Color
) {
    Box(
        modifier = Modifier
            .width(TimelineWidth)
            .height(rowHeight)
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .width(TimelineLineWidth)
                .height(rowHeight / 2)
                .background(topLineColor)
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .width(TimelineLineWidth)
                .height(rowHeight / 2)
                .background(bottomLineColor)
        )
        Surface(
            shape = CircleShape,
            color = if (bottomLineColor != TimelineLineColorNone) bottomLineColor else topLineColor,
            modifier = Modifier
                .align(Alignment.Center)
                .size(TimelineNodeSize)  // 정사각형 고정 → CircleShape가 원으로 렌더링되도록
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = lineLabel,
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
fun LineLineComponent(
    rowHeight: Dp,
    lineColor: Color
) {
    Box(
        modifier = Modifier
            .width(TimelineWidth)
            .height(rowHeight)
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(TimelineLineWidth)
                .background(lineColor)
                .align(Alignment.Center)
        )
    }
}

@Composable
fun TimelineNode(
    label: String,
    showLineBelow: Boolean,
    rowHeight: Dp
) {
    Box(
        modifier = Modifier
            .width(TimelineWidth)
            .height(rowHeight)
    ) {
        if (showLineBelow) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(TimelineLineWidth)
                    .background(TimelineNodeColor)
                    .align(Alignment.Center)
            )
        }
        Surface(
            shape = CircleShape,
            color = TimelineNodeColor,
            modifier = Modifier
                .align(Alignment.Center)
                .size(TimelineNodeSize)  // 정사각형 고정 → CircleShape가 원으로 렌더링
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
fun RecommendationSectionDivider() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(RecommendationSectionDividerHeight),
        verticalAlignment = Alignment.CenterVertically
    ) {
        HorizontalDivider(
            modifier = Modifier.fillMaxWidth(),
            color = DetailCardDividerColor,
            thickness = 1.dp
        )
    }
}

@Composable
fun TimelineDivider(
    showVerticalLine: Boolean = true,
    lineColor: Color = TimelineNodeColor
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(TimelineDividerRowHeight),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .width(TimelineWidth)
                .fillMaxHeight(),
            contentAlignment = Alignment.Center
        ) {
            if (showVerticalLine) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(TimelineLineWidth)
                        .background(lineColor)
                )
            }
        }
        Spacer(modifier = Modifier.width(TimelineContentGap))
        HorizontalDivider(
            modifier = Modifier.weight(1f),
            color = DetailCardDividerColor,
            thickness = 1.dp
        )
    }
}
