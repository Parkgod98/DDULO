package com.example.ddulo.ui.component.routeDetail

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

private val HeaderMinHeight = 120.dp
private val ScreenPaddingHorizontal = 16.dp

@Composable
fun RouteDetailHeader(
    totalMinutes: Int,
    pathSummary: String,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = HeaderMinHeight)
            .padding(vertical = 8.dp)
    ) {
        IconButton(
            onClick = onBack,
            modifier = Modifier.size(48.dp)
        ) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "뒤로",
                tint = Color.White
            )
        }
        Column(modifier = Modifier.padding(horizontal = ScreenPaddingHorizontal)) {
            Text(
                text = "${totalMinutes}분",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = pathSummary,
                style = MaterialTheme.typography.titleLarge,
                color = Color.White
            )
        }
    }
}
