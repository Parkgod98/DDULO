package com.example.ddulo.ui.screen.favorites

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.ddulo.data.local.entity.FavoriteEntity
import com.example.ddulo.domain.model.Station
import com.example.ddulo.ui.component.favorite.FavoriteListItem
import com.example.ddulo.ui.component.station.LineBadge
import com.example.ddulo.viewmodel.FavoriteViewModel
import com.google.gson.Gson

/**
 * Station 탭 리스트
 */
@Composable
fun StationFavoritesList(
    favorites: List<FavoriteEntity>,
    favoriteViewModel: FavoriteViewModel,
    onStationClick: (List<Station>) -> Unit
) {
    if (favorites.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("즐겨찾기한 역이 없습니다.")
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp), // ⭐️ Route 리스트와 동일하게 패딩 설정
            verticalArrangement = Arrangement.spacedBy(12.dp) // ⭐️ 간격 통일
        ) {
            items(favorites) { favorite ->
                val stations = try {
                    Gson().fromJson(
                        favorite.payload,
                        Array<Station>::class.java
                    ).toList()
                } catch (e: Exception) {
                    emptyList()
                }

                FavoriteListItem(
                    title = favorite.displayName,
                    subtitle = {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            stations.forEach { station ->
                                LineBadge(lineName = station.lineName)
                            }
                        }
                    },
                    onClick = { onStationClick(stations) },
                    onDeleteClick = { favoriteViewModel.removeFavoriteById(favorite.id) }
                )
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    thickness = 0.5.dp,
                    color = MaterialTheme.colorScheme.outlineVariant
                )
            }
        }
    }
}
