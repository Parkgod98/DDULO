package com.example.ddulo.ui.component.station

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ddulo.data.local.entity.FavoriteType
import com.example.ddulo.domain.model.Station
import com.example.ddulo.viewmodel.FavoriteViewModel
import com.google.gson.Gson
import kotlin.collections.forEach

@Composable
fun StationGroupItem(
    stationName: String,
    lines: List<Station>,
    favoriteViewModel: FavoriteViewModel,
    onClick: () -> Unit,
    trailingContent: @Composable (() -> Unit)? = null
) {
    val favorites by favoriteViewModel.favorites.collectAsState(initial = emptyList())

    // station과 연결된 FavoriteEntity 찾기
    val favoriteEntity = favorites.find { fav ->
        fav.type == FavoriteType.STATION &&
                Gson().fromJson(fav.payload, Array<Station>::class.java).toList().map { it.stationId } ==
                lines.map { it.stationId }
    }

    val isFavorite = favoriteEntity != null

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {  // 텍스트와 LineBadge
                Text(
                    text = stationName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    lines.forEach { station ->
                        LineBadge(lineName = station.lineName)
                    }
                }
            }

            trailingContent?.invoke()

            IconButton(  // ← 오른쪽 끝으로 이동
                onClick = {
                    if (isFavorite) favoriteViewModel.removeFavoriteById(favoriteEntity!!.id)
                    else favoriteViewModel.addStationFavorite(lines)
                }
            ) {
                Icon(
                    imageVector = if (isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                    contentDescription = "즐겨찾기",
                    tint = if (isFavorite) Color(0xFF00C775) else LocalContentColor.current
                )
            }
        }
    }
}