package com.example.ddulo.ui.component.search

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ddulo.ui.component.station.StationList
import com.example.ddulo.viewmodel.FavoriteViewModel
import com.example.ddulo.viewmodel.SharedViewModel
import com.example.ddulo.viewmodel.StationDetailViewModel

/**
 * 자동완성 검색 결과 UI
 */
@Composable
fun AutocompleteContent(
    sharedViewModel: SharedViewModel,
    favoriteViewModel: FavoriteViewModel,
    stationDetailViewModel: StationDetailViewModel,
    onShowSheet: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = "역 검색 결과",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.height(8.dp))
        StationList(
            stations = sharedViewModel.filteredStations.value,
            favoriteViewModel = favoriteViewModel,
            onStationClick = { group ->
                sharedViewModel.selectStationGroup(group)
                stationDetailViewModel.clearState()

                // ⭐ 첫 번째 역 자동 선택
                stationDetailViewModel.selectStation(group.first().stationId)
                onShowSheet()
            },
            modifier = Modifier.fillMaxSize()
        )
    }
}