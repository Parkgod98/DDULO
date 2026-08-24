package com.example.ddulo.ui.screen.favorites

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
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
import com.example.ddulo.domain.model.FavoriteRoute
import com.example.ddulo.ui.component.favorite.FavoriteListItem
import com.example.ddulo.viewmodel.FavoriteViewModel
import com.google.gson.Gson

/**
 * Route 탭 리스트
 */
@Composable
fun RouteFavoritesList(
    favorites: List<FavoriteEntity>,
    favoriteViewModel: FavoriteViewModel,
    onRouteClick: (FavoriteRoute) -> Unit
) {
    if (favorites.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("즐겨찾기한 경로가 없습니다.")
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(favorites) { favorite ->
                val favoriteData = try {
                    Gson().fromJson(favorite.payload, FavoriteRoute::class.java)
                } catch (e: Exception) {
                    null
                }

                favoriteData?.let { data ->
                    FavoriteListItem(
                        title = favorite.displayName,
                        subtitle = {
                            val totalMinutes = data.totalTime / 60
                            Text(
                                text = "총 ${totalMinutes}분 · 환승 ${data.transferCount}회",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        onClick = { onRouteClick(data) },
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
}
