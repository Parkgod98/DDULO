package com.example.ddulo.ui.component.search

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.example.ddulo.viewmodel.AppViewModel
import com.example.ddulo.viewmodel.FavoriteViewModel
import com.example.ddulo.viewmodel.SharedViewModel
import com.example.ddulo.viewmodel.state.InitialLoadState

@Composable
fun RecommendationContent(
    appViewModel: AppViewModel,
    sharedViewModel: SharedViewModel,
    favoriteViewModel: FavoriteViewModel,
    onShowSheet: () -> Unit
) {
    when (val state = appViewModel.loadState) {
        is InitialLoadState.Loading -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        is InitialLoadState.Success -> {
            NearbyStationSection(
                nearbyStations = state.nearbyStations,
                fallbackStation = state.fallbackStation,
                favoriteViewModel = favoriteViewModel,
                onStationClick = { group ->
                    sharedViewModel.selectStationGroup(group)
                    onShowSheet()
                }
            )
        }
        is InitialLoadState.Error -> {
            ErrorSection(message = state.message)
        }
    }
}