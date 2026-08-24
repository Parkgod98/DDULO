package com.example.ddulo.ui.component.search

import androidx.compose.animation.*
import androidx.compose.runtime.Composable
import com.example.ddulo.viewmodel.AppViewModel
import com.example.ddulo.viewmodel.FavoriteViewModel
import com.example.ddulo.viewmodel.SharedViewModel
import com.example.ddulo.viewmodel.StationDetailViewModel

/**
 * 추천 <-> 자동완성 전환 Crossfade
 */
@Composable
fun UnifiedSearchContent(
    isSearching: Boolean,
    appViewModel: AppViewModel,
    sharedViewModel: SharedViewModel,
    favoriteViewModel: FavoriteViewModel,
    stationDetailViewModel: StationDetailViewModel,
    onShowSheet: () -> Unit
) {
    // 💡 Crossfade를 사용하여 추천 <-> 자동완성 간의 전환 시 시각적 공백(깜빡임)을 방지합니다.
    Crossfade(targetState = isSearching, label = "SearchContentTransition") { searching ->
        if (searching) {
            AutocompleteContent(sharedViewModel, favoriteViewModel, stationDetailViewModel, onShowSheet)
        } else {
            RecommendationContent(appViewModel, sharedViewModel, favoriteViewModel, onShowSheet)
        }
    }
}