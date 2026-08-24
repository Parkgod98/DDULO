package com.example.ddulo.ui.component.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ddulo.domain.model.Station
import com.example.ddulo.ui.component.station.StationGroupItem
import com.example.ddulo.viewmodel.FavoriteViewModel

/**
 * 주변역 리스트 UI
 */
@Composable
fun NearbyStationSection(
    nearbyStations: List<Station>,
    fallbackStation: Station?,
    onStationClick: (List<Station>) -> Unit,
    favoriteViewModel: FavoriteViewModel
) {
    if (nearbyStations.isEmpty() && fallbackStation == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("추천할 역 정보가 없습니다.", style = MaterialTheme.typography.bodyMedium)
        }
        return
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.MyLocation,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = if (nearbyStations.isNotEmpty()) "내 주변 역" else "가장 가까운 역",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(Modifier.height(12.dp))

        if (nearbyStations.isNotEmpty()) {
            val groupedNearby = nearbyStations.groupBy { it.stationName }
            LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                items(groupedNearby.entries.toList()) { entry: Map.Entry<String, List<Station>> ->
                    val name = entry.key
                    val lines = entry.value
                    StationGroupItem(
                        stationName = name,
                        lines = lines,
                        favoriteViewModel = favoriteViewModel,
                        onClick = { onStationClick(lines) },
                        trailingContent = {
                            lines.first().distanceMeters?.let {
                                Text("${it.toInt()}m", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        thickness = 0.5.dp,
                        color = MaterialTheme.colorScheme.outlineVariant
                    )
                }
            }
        } else if (fallbackStation != null) {
            StationGroupItem(
                stationName = fallbackStation.stationName,
                lines = listOf(fallbackStation),
                favoriteViewModel = favoriteViewModel,
                onClick = { onStationClick(listOf(fallbackStation)) }
            )
        }
    }
}