package com.example.ddulo.ui.component.routeDetail

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp

private val TimetableButtonShape = RoundedCornerShape(20.dp)
private val TimetableButtonContentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp)

/** 칠한 영역과 같은 경계에 테두리: 연한 회색 */
private val TimetableButtonBorderColor = Color(0xFFE0E0E0)

/**
 * 경로 상세 역 헤더용 '시간표' 버튼.
 * 테두리만 있고 배경은 투명.
 */
@Composable
fun TimetableButton(
    onClick: () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(TimetableButtonShape)
            .border(1.dp, TimetableButtonBorderColor, TimetableButtonShape)
            .clickable(
                enabled = enabled,
                role = Role.Button,
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(TimetableButtonContentPadding),
        contentAlignment = Alignment.Center
    ) {
        Text(
            "시간표",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
