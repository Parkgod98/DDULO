package com.example.ddulo.ui.component.station

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.ddulo.domain.model.Station
import com.example.ddulo.viewmodel.FavoriteViewModel

@Composable
fun StationList(
    stations: List<Station>,
    favoriteViewModel: FavoriteViewModel,
    onStationClick: (List<Station>) -> Unit,
    modifier: Modifier = Modifier
) {
    val groupedStations = stations.groupBy { it.stationName }

    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        items(groupedStations.entries.toList()) { (stationName, stationLines) ->
            StationGroupItem(
                stationName = stationName,
                lines = stationLines,
                favoriteViewModel = favoriteViewModel,
                onClick = {
                    onStationClick(stationLines)
                }
            )
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 16.dp),
                thickness = 0.5.dp,
                color = MaterialTheme.colorScheme.outlineVariant
            )
        }
    }
}
